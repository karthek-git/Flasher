package com.karthek.android.s.flasher

import android.app.Application
import com.karthek.android.s.flasher.ui.components.AppOpenAdManager

class FlasherApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		AppOpenAdManager.init(this)
	}
}