package com.example.spotcast.audio

import android.media.MediaPlayer

class AudioPlayer {

    private var player: MediaPlayer? = null

    fun play(filePath: String, onComplete: () -> Unit = {}) {
        stop()
        player = MediaPlayer().apply {
            setDataSource(filePath)
            setOnCompletionListener {
                onComplete()
                release()
                player = null
            }
            prepare()
            start()
        }
    }

    fun playUrl(url: String, onComplete: () -> Unit = {}) {
        stop()
        player = MediaPlayer().apply {
            setDataSource(url)
            setOnCompletionListener {
                onComplete()
                release()
                player = null
            }
            prepareAsync()
            setOnPreparedListener { it.start() }
        }
    }

    fun stop() {
        player?.apply {
            if (isPlaying) stop()
            release()
        }
        player = null
    }

    fun isPlaying(): Boolean = player?.isPlaying == true
}
