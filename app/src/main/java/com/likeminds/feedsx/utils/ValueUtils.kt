package com.likeminds.feedsx.utils

class ValueUtils {

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

fun Int.isValidIndex(items: List<*>? = null): Boolean {
    return if (items != null) {
        this > -1 && this < items.size
    } else {
        this > -1
    }
}