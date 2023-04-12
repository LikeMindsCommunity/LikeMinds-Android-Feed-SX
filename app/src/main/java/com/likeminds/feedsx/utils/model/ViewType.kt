package com.likeminds.feedsx.utils.model

import androidx.annotation.IntDef

const val ITEM_NONE = 0
const val ITEM_POST_TEXT_ONLY = 1
const val ITEM_POST_SINGLE_IMAGE = 2
const val ITEM_POST_SINGLE_VIDEO = 3
const val ITEM_POST_DOCUMENTS = 4
const val ITEM_POST_LINK = 5
const val ITEM_POST_MULTIPLE_MEDIA = 6
const val ITEM_MULTIPLE_MEDIA_IMAGE = 7
const val ITEM_MULTIPLE_MEDIA_VIDEO = 8
const val ITEM_POST_ATTACHMENT = 9
const val ITEM_OVERFLOW_MENU_ITEM = 10
const val ITEM_USER = 11
const val ITEM_POST_DETAIL_COMMENT = 12
const val ITEM_POST_DOCUMENTS_ITEM = 13
const val ITEM_MEDIA_PICKER_FOLDER = 14
const val ITEM_MEDIA_PICKER_BROWSE = 15
const val ITEM_MEDIA_PICKER_HEADER = 16
const val ITEM_MEDIA_PICKER_SINGLE = 17
const val ITEM_MEDIA_PICKER_DOCUMENT = 18
const val ITEM_CREATE_POST_DOCUMENTS_ITEM = 19
const val ITEM_CREATE_POST_MULTIPLE_MEDIA_IMAGE = 20
const val ITEM_CREATE_POST_MULTIPLE_MEDIA_VIDEO = 21
const val ITEM_LIKES_SCREEN = 22
const val ITEM_POST_DETAIL_COMMENTS_COUNT = 23
const val ITEM_POST_DETAIL_REPLY = 24
const val ITEM_REPLY_VIEW_MORE_REPLY = 25
const val ITEM_REPORT_TAG = 26
const val ITEM_REASON_CHOOSE = 27
const val ITEM_NOTIFICATION_FEED = 28
const val ITEM_NO_COMMENTS_FOUND = 29

@IntDef(
    ITEM_NONE,
    ITEM_POST_TEXT_ONLY,
    ITEM_POST_SINGLE_IMAGE,
    ITEM_POST_SINGLE_VIDEO,
    ITEM_POST_DOCUMENTS,
    ITEM_POST_LINK,
    ITEM_POST_MULTIPLE_MEDIA,
    ITEM_MULTIPLE_MEDIA_IMAGE,
    ITEM_MULTIPLE_MEDIA_VIDEO,
    ITEM_POST_ATTACHMENT,
    ITEM_OVERFLOW_MENU_ITEM,
    ITEM_USER,
    ITEM_POST_DETAIL_COMMENT,
    ITEM_POST_DOCUMENTS_ITEM,
    ITEM_MEDIA_PICKER_FOLDER,
    ITEM_MEDIA_PICKER_BROWSE,
    ITEM_MEDIA_PICKER_HEADER,
    ITEM_MEDIA_PICKER_SINGLE,
    ITEM_MEDIA_PICKER_DOCUMENT,
    ITEM_CREATE_POST_DOCUMENTS_ITEM,
    ITEM_CREATE_POST_MULTIPLE_MEDIA_IMAGE,
    ITEM_CREATE_POST_MULTIPLE_MEDIA_VIDEO,
    ITEM_LIKES_SCREEN,
    ITEM_POST_DETAIL_COMMENTS_COUNT,
    ITEM_POST_DETAIL_REPLY,
    ITEM_REPLY_VIEW_MORE_REPLY,
    ITEM_REPORT_TAG,
    ITEM_REASON_CHOOSE,
    ITEM_NOTIFICATION_FEED,
    ITEM_NO_COMMENTS_FOUND
)
@Retention(AnnotationRetention.SOURCE)
annotation class ViewType