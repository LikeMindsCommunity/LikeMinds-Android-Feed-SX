package com.likeminds.feedsx.utils

import android.content.Context
import android.net.Uri
import android.util.Patterns
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import com.likeminds.feedsx.media.model.IMAGE
import com.likeminds.feedsx.media.model.PDF
import com.likeminds.feedsx.media.model.VIDEO
import com.likeminds.feedsx.utils.ViewUtils.isValidUrl
import com.likeminds.feedsx.utils.pluralize.model.WordAction
import com.likeminds.feedsx.utils.pluralize.pluralize
import com.likeminds.feedsx.utils.pluralize.singularize

object ValueUtils {

    @JvmStatic
    fun <K, V> getOrDefault(map: Map<K, V>, key: K, defaultValue: V): V? {
        return if (map.containsKey(key)) map[key] else defaultValue
    }

    fun String.getValidTextForLinkify(): String {
        return this.replace("\u202C", "")
            .replace("\u202D", "")
            .replace("\u202E", "")
    }

    fun <T> List<T>.getItemInList(position: Int): T? {
        if (position < 0 || position >= this.size) {
            return null
        }
        return this[position]
    }

    /**
     * This function run filter and map operation in single loop
     */
    inline fun <T, R, P> Iterable<T>.filterThenMap(
        predicate: (T) -> Pair<Boolean, P>,
        transform: (Pair<T, P>) -> R
    ): List<R> {
        return filterThenMap(ArrayList(), predicate, transform)
    }

    inline fun <T, R, P, C : MutableCollection<in R>>
            Iterable<T>.filterThenMap(
        collection: C, predicate: (T) -> Pair<Boolean, P>,
        transform: (Pair<T, P>) -> R
    ): C {
        for (element in this) {
            val response = predicate(element)
            if (response.first) {
                collection.add(transform(Pair(element, response.second)))
            }
        }
        return collection
    }

    /**
     * Check mediaType from Media mimeType
     * */
    fun String?.getMediaType(): String? {
        var mediaType: String? = null
        if (this != null) {
            when {
                this.startsWith("image") -> mediaType = IMAGE
                this.startsWith("video") -> mediaType = VIDEO
                this == "application/pdf" -> mediaType = PDF
            }
        }
        return mediaType
    }

    fun Uri?.getMediaType(context: Context): String? {
        var mediaType: String? = null
        this?.let {
            mediaType = this.getMimeType(context).getMediaType()
        }
        return mediaType
    }

    private fun Uri.getMimeType(context: Context): String? {
        var type = context.contentResolver.getType(this)
        if (type == null) {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(this.toString())
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
        return type
    }

    fun String.getUrlIfExist(): String? {
        return try {
            val links: MutableList<String> = ArrayList()
            val matcher = Patterns.WEB_URL.matcher(this)
            while (matcher.find()) {
                val link = matcher.group()
                if (URLUtil.isValidUrl(link)) {
                    links.add(link)
                } else {
                    val newHttpsLink = "https://$link"
                    if (URLUtil.isValidUrl(newHttpsLink)) {
                        links.add(newHttpsLink)
                    }
                }
            }
            if (links.isNotEmpty()) {
                links.first()
            } else null
        } catch (e: Exception) {
            return null
        }
    }

    fun String?.isImageValid(): Boolean {
        return this?.isValidUrl() ?: false
    }

    fun String.pluralizeOrCapitalize(action: WordAction): String {
        return when (action) {
            WordAction.FIRST_LETTER_CAPITAL_SINGULAR -> {
                val singular = this.singularize()
                singular.replaceFirstChar {
                    it.uppercase()
                }
            }

            WordAction.ALL_CAPITAL_SINGULAR -> {
                val singular = this.singularize()
                singular.uppercase()
            }

            WordAction.ALL_SMALL_SINGULAR -> {
                val singular = this.singularize()
                singular.lowercase()
            }

            WordAction.FIRST_LETTER_CAPITAL_PLURAL -> {
                val plural = this.pluralize()
                plural.replaceFirstChar {
                    it.uppercase()
                }
            }

            WordAction.ALL_CAPITAL_PLURAL -> {
                val plural = this.pluralize()
                plural.uppercase()
            }

            WordAction.ALL_SMALL_PLURAL -> {
                val plural = this.pluralize()
                plural.lowercase()
            }
        }
    }
}