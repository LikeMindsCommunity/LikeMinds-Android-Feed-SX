package com.likeminds.feedsx.delete.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class DeleteExtras private constructor(
    @DeleteType
    val entityType: Int,
    val postId: String,
    val commentId: String?,
    val parentCommentId: String?,
    val postAsVariable: String
) : Parcelable {

    class Builder {
        private var entityType: Int = 0
        private var postId: String = ""
        private var commentId: String? = null
        private var parentCommentId: String? = null
        private var postAsVariable: String = ""

        fun entityType(@DeleteType entityType: Int) = apply { this.entityType = entityType }
        fun postId(postId: String) = apply { this.postId = postId }

        fun commentId(commentId: String?) = apply { this.commentId = commentId }
        fun parentCommentId(parentCommentId: String?) =
            apply { this.parentCommentId = parentCommentId }

        fun postAsVariable(postAsVariable: String) = apply { this.postAsVariable = postAsVariable }

        fun build() = DeleteExtras(
            entityType,
            postId,
            commentId,
            parentCommentId,
            postAsVariable
        )
    }

    fun toBuilder(): Builder {
        return Builder().entityType(entityType)
            .postId(postId)
            .commentId(commentId)
            .parentCommentId(parentCommentId)
            .postAsVariable(postAsVariable)
    }
}