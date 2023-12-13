package com.likeminds.feedsx

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.messaging.FirebaseMessaging
import com.likeminds.feedsx.SDKApplication.Companion.LOG_TAG
import com.likeminds.feedsx.feed.ConfigurationRepository
import com.likeminds.feedsx.feed.UserWithRightsRepository
import com.likeminds.feedsx.posttypes.model.UserViewData
import com.likeminds.feedsx.utils.LMFeedUserPreferences
import com.likeminds.feedsx.utils.ViewDataConverter
import com.likeminds.feedsx.utils.coroutine.launchIO
import com.likeminds.feedsx.utils.memberrights.util.MemberRightUtil
import com.likeminds.likemindsfeed.LMFeedClient
import com.likeminds.likemindsfeed.helper.model.RegisterDeviceRequest
import com.likeminds.likemindsfeed.initiateUser.model.InitiateUserRequest
import com.likeminds.likemindsfeed.sdk.model.User
import javax.inject.Inject

class InitiateViewModel @Inject constructor(
    private val userPreferences: LMFeedUserPreferences,
    private val userWithRightsRepository: UserWithRightsRepository,
    private val configurationRepository: ConfigurationRepository
) : ViewModel() {

    private val lmFeedClient = LMFeedClient.getInstance()

    private val _userResponse = MutableLiveData<UserViewData>()
    val userResponse: LiveData<UserViewData> = _userResponse

    private val _logoutResponse = MutableLiveData<Boolean>()
    val logoutResponse: LiveData<Boolean> = _logoutResponse

    private val _hasCreatePostRights = MutableLiveData(true)
    val hasCreatePostRights: LiveData<Boolean> = _hasCreatePostRights

    private val _hasCommentRights = MutableLiveData(true)
    val hasCommentRights: LiveData<Boolean> = _hasCommentRights

    private val _initiateErrorMessage = MutableLiveData<String?>()
    val initiateErrorMessage: LiveData<String?> = _initiateErrorMessage

    /***
     * calls InitiateUser API
     * store user:{} in db
     * and posts the response in LiveData
     * */
    fun initiateUser(
        context: Context,
        apiKey: String,
        userName: String,
        userId: String?,
        isGuest: Boolean
    ) {
        viewModelScope.launchIO {
            if (apiKey.isEmpty()) {
                _initiateErrorMessage.postValue(context.getString(R.string.empty_api_key))
                return@launchIO
            }

            val request = InitiateUserRequest.Builder()
                .apiKey(apiKey)
                .deviceId(userPreferences.getDeviceId())
                .uuid(userId)
                .userName(userName)
                .isGuest(isGuest)
                .build()

            //call api
            val initiateResponse = lmFeedClient.initiateUser(request)

            if (initiateResponse.success) {
                val data = initiateResponse.data ?: return@launchIO
                if (data.logoutResponse != null) {
                    //user is invalid
                    userPreferences.clearPrefs()
                    _logoutResponse.postValue(true)
                } else {
                    val user = data.user
                    val id = user?.userUniqueId ?: ""
                    val uuid = user?.sdkClientInfo?.uuid ?: ""

                    //add user in local db
                    addUser(user)

                    //save user.id in local prefs
                    saveUserDetailsToPrefs(
                        apiKey,
                        userName,
                        id,
                        isGuest
                    )
                    userPreferences.saveUserUniqueId(id)
                    userPreferences.saveUUID(uuid)

                    val userViewData = ViewDataConverter.convertUser(user)
                    //get community configuration
                    getCommunityConfiguration(userViewData)
                }
            } else {
                _initiateErrorMessage.postValue(initiateResponse.errorMessage)
            }
        }
    }

    //add user:{} into local db
    private fun addUser(user: User?) {
        if (user == null) return
        viewModelScope.launchIO {
            //convert user into userEntity
            val userEntity = ViewDataConverter.createUserEntity(user)
            //add it to local db
            userWithRightsRepository.insertUser(userEntity)

            //call member state api
            getMemberState()

            //call register device api
            registerDevice()
        }
    }

    //call member state api
    private fun getMemberState() {
        viewModelScope.launchIO {
            //get member state response
            val memberStateResponse = lmFeedClient.getMemberState().data

            val memberState = memberStateResponse?.state ?: return@launchIO
            val isOwner = memberStateResponse.isOwner
            val userId = memberStateResponse.userUniqueId

            //get existing userEntity
            var userEntity = userWithRightsRepository.getUser(userId)

            //updated userEntity
            userEntity = userEntity.toBuilder().state(memberState).isOwner(isOwner).build()

            // creates list of [MemberRightsEntity]
            val memberRightsEntity = ViewDataConverter.createMemberRightsEntity(
                userId,
                memberStateResponse.memberRights
            )

            //updates user's create posts right
            _hasCreatePostRights.postValue(
                MemberRightUtil.hasCreatePostsRight(
                    memberState,
                    memberRightsEntity
                )
            )

            //updates user's comment right
            _hasCommentRights.postValue(
                MemberRightUtil.hasCommentRight(
                    memberState,
                    memberRightsEntity
                )
            )

            //inserts userEntity with their rights in local db
            userWithRightsRepository.insertUserWithRights(userEntity, memberRightsEntity)
        }
    }

    //call register device
    private fun registerDevice() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(
                    LOG_TAG,
                    "Fetching FCM registration token failed",
                    task.exception
                )
                return@addOnCompleteListener
            }

            val token = task.result.toString()
            pushToken(token)
        }
    }

    private fun pushToken(token: String) {
        viewModelScope.launchIO {
            //create request
            val request = RegisterDeviceRequest.Builder()
                .deviceId(userPreferences.getDeviceId())
                .token(token)
                .build()

            //call api
            lmFeedClient.registerDevice(request)
        }
    }

    /**
     * get community configurations
     * and save it local db
     */
    private fun getCommunityConfiguration(userViewData: UserViewData) {
        viewModelScope.launchIO {
            val response = lmFeedClient.getCommunityConfiguration()
            if (response.success) {
                val configurations = response.data?.configurations ?: return@launchIO

                val configurationEntities = configurations.map { configuration ->
                    ViewDataConverter.createConfigurationEntity(configuration)
                }

                //add configuration to db
                configurationRepository.insertConfigurations(configurationEntities)

                //post the user response in LiveData
                _userResponse.postValue(userViewData)
            } else {
                Log.d(LOG_TAG, "community/configuration failed -> ${response.errorMessage}")
            }
        }
    }

    private fun saveUserDetailsToPrefs(
        apiKey: String,
        userName: String,
        userUniqueId: String,
        isGuest: Boolean
    ) {
        userPreferences.apply {
            saveApiKey(apiKey)
            saveUserName(userName)
            saveUserUniqueId(userUniqueId)
            saveIsGuest(isGuest)
        }
    }
}