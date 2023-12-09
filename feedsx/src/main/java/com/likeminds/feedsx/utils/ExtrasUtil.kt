package com.likeminds.feedsx.utils

import android.os.Build
import android.os.Bundle
import androidx.core.os.BundleCompat

object ExtrasUtil {

    //returns the extras passed with if-else check
    @Suppress("DEPRECATION")
    fun <T> getParcelable(argument: Bundle?, argumentName: String, clazz: Class<T>): T? {
        if (argument == null) {
            return null
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BundleCompat.getParcelable(argument, argumentName, clazz)
        } else {
            argument.getParcelable(argumentName)
        }
    }
}