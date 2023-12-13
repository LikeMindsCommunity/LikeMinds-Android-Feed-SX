package com.likeminds.feedsx.utils.pluralize

import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

fun String.pluralize(count: Int = 2): String {
    return if (abs(count) != 1)
        this.pluralizer()
    else
        this.singularizer()
}

fun String.singularize(count: Int = 1): String = pluralize(count)

private fun String.pluralizer(): String {
    if (unCountable().contains(this.lowercase(Locale.getDefault()))) return this
    val rule = pluralizeRules().last {
        Pattern.compile(it.component1(), Pattern.CASE_INSENSITIVE).matcher(this).find()
    }
    var found = Pattern.compile(rule.component1(), Pattern.CASE_INSENSITIVE).matcher(this)
        .replaceAll(rule.component2())
    val endsWith = exceptions().firstOrNull { this.endsWith(it.component1()) }
    if (endsWith != null) found = this.replace(endsWith.component1(), endsWith.component2())
    val exception = exceptions().firstOrNull { this.equals(it.component1(), true) }
    if (exception != null) found = exception.component2()
    return found
}

private fun String.singularizer(): String {
    if (unCountable().contains(this.lowercase(Locale.getDefault()))) {
        return this
    }
    val exceptions = exceptions().firstOrNull { this.equals(it.component2(), true) }

    if (exceptions != null) {
        return exceptions.component1()
    }
    val endsWith = exceptions().firstOrNull { this.endsWith(it.component2()) }

    if (endsWith != null) return this.replace(endsWith.component2(), endsWith.component1())

    try {
        if (singularizeRules().count {
                Pattern.compile(it.component1(), Pattern.CASE_INSENSITIVE).matcher(this).find()
            } == 0) return this
        val rule = singularizeRules().last {
            Pattern.compile(it.component1(), Pattern.CASE_INSENSITIVE).matcher(this).find()
        }
        return Pattern.compile(rule.component1(), Pattern.CASE_INSENSITIVE).matcher(this)
            .replaceAll(rule.component2())
    } catch (ex: IllegalArgumentException) {
        Exception("Can't singularize this word, could not find a rule to match.")
    }
    return this
}