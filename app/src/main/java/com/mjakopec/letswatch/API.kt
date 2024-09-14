package com.mjakopec.letswatch

import android.app.Application

class API: Application() {
    companion object {
        var globalKey: String = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJjYjE4NGYxNzM5MjMzNTJkOWIwYTgwOTcwY2M0OWU1ZSIsIm5iZiI6MTcxOTc1OTg4NS4zMDU1NTgsInN1YiI6IjYxZjU2MWY2YzM5MjY2MDA0OTY2ZTg5MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.nmjRQdQLahL0QLc56WsANGTae0fv5nWmLRQ7-X-z720"
    }

    override fun onCreate() {
        super.onCreate()
    }
}