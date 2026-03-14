package com.example.spotcast

import android.app.Application
import android.content.Context
import com.example.spotcast.audio.AudioPlayer
import com.example.spotcast.audio.AudioRecorder
import com.example.spotcast.data.local.AppDatabase
import com.example.spotcast.data.preferences.LocaleManager
import com.example.spotcast.data.preferences.TokenManager
import com.example.spotcast.data.remote.RetrofitClient
import com.example.spotcast.data.repository.AuthRepository
import com.example.spotcast.data.repository.CapsuleRepository
import com.example.spotcast.data.repository.FriendsRepository
import com.example.spotcast.geofence.GeofenceManager
import com.example.spotcast.tts.TtsManager
import org.osmdroid.config.Configuration

class SpotCastApplication : Application() {

    lateinit var tokenManager: TokenManager
    lateinit var localeManager: LocaleManager
    lateinit var authRepository: AuthRepository
    lateinit var capsuleRepository: CapsuleRepository
    lateinit var friendsRepository: FriendsRepository
    lateinit var geofenceManager: GeofenceManager
    lateinit var ttsManager: TtsManager
    lateinit var audioRecorder: AudioRecorder
    lateinit var audioPlayer: AudioPlayer

    private val notificationShownSet = mutableSetOf<Int>()

    fun hasShownNotification(capsuleId: Int): Boolean = capsuleId in notificationShownSet

    fun markNotificationShown(capsuleId: Int) {
        notificationShownSet.add(capsuleId)
    }

    override fun attachBaseContext(base: Context?) {
        if (base != null) {
            val lm = LocaleManager(base)
            super.attachBaseContext(lm.applyLocale(base))
        } else {
            super.attachBaseContext(base)
        }
    }

    override fun onCreate() {
        super.onCreate()

        Configuration.getInstance().userAgentValue = packageName

        tokenManager = TokenManager(this)
        localeManager = LocaleManager(this)

        val api = RetrofitClient.create(tokenManager)
        val db = AppDatabase.getInstance(this)

        authRepository = AuthRepository(api, tokenManager)
        capsuleRepository = CapsuleRepository(api, db.capsuleDao())
        friendsRepository = FriendsRepository(api)
        geofenceManager = GeofenceManager(this)
        ttsManager = TtsManager(this)
        audioRecorder = AudioRecorder(this)
        audioPlayer = AudioPlayer()
    }
}
