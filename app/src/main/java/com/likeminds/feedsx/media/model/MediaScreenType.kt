package com.likeminds.feedsx.media.model

import androidx.annotation.IntDef

const val MEDIA_VERTICAL_LIST_SCREEN = 0
const val MEDIA_HORIZONTAL_LIST_SCREEN = 1
const val MEDIA_CONVERSATION_EDIT_SCREEN = 2
const val MEDIA_CROP_SCREEN = 3
const val MEDIA_VIDEO_PLAY_SCREEN = 4
const val MEDIA_DOCUMENT_SEND_SCREEN = 5

@IntDef(
    MEDIA_VERTICAL_LIST_SCREEN,
    MEDIA_HORIZONTAL_LIST_SCREEN,
    MEDIA_CONVERSATION_EDIT_SCREEN,
    MEDIA_CROP_SCREEN,
    MEDIA_VIDEO_PLAY_SCREEN,
    MEDIA_DOCUMENT_SEND_SCREEN
)

@Retention(AnnotationRetention.SOURCE)
internal annotation class MediaScreenType