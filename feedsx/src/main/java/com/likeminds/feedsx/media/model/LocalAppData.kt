package com.likeminds.feedsx.media.model

import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

data class LocalAppData(
    val appId: Int,
    val appName: String,
    val appIcon: Drawable,
    val resolveInfo: ResolveInfo
)
