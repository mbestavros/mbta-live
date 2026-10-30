package com.example

import android.app.Application
import com.example.data.api.MbtaApiClient

class MbtaLiveApp : Application() {
  override fun onCreate() {
    super.onCreate()
    MbtaApiClient.init(this)
  }
}
