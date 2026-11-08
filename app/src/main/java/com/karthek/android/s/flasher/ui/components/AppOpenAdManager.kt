package com.karthek.android.s.flasher.ui.components

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.WorkManager
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.karthek.android.s.flasher.MainActivity
import com.karthek.android.s.flasher.state.SelectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val AD_APP_ID = "ca-app-pub-2240982710415001~9774210362"

// TEST ca-app-pub-3940256099942544/9257395921
const val AD_UNIT_ID = "ca-app-pub-2240982710415001/7540715109"

/**
 * Initializes the ads SDK and shows an app open ad when the user returns to the app.
 *
 * - Foregrounding is detected with [ProcessLifecycleOwner], which ignores rotation and
 *   quick switches, and is only treated as a "return" after the process was stopped once,
 *   so it never shows on cold start.
 * - The ad is shown once [MainActivity] is resumed, and never while a flash is active.
 * - Call [suppressNextShow] right before launching a system flow (USB permission dialog,
 *   file picker) so coming back from it does not show an ad.
 * - Ads come from [AppOpenAdPreloader], which keeps a fresh ad ready and handles expiry.
 */
object AppOpenAdManager : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
	private const val MIN_INTERVAL_MS = 3 * 60 * 1000L
	private const val SUPPRESS_WINDOW_MS = 10 * 60 * 1000L

	private var initialized = false
	private var wasBackgrounded = false
	private var pendingShow = false
	private var flashActive = false
	private var isShowingAd = false
	private var lastShownAt = 0L
	private var suppressedAt = 0L

	fun init(app: Application) {
		if (initialized) return
		initialized = true

		app.registerActivityLifecycleCallbacks(this)
		ProcessLifecycleOwner.get().lifecycle.addObserver(this)
		WorkManager.getInstance(app)
			.getWorkInfosForUniqueWorkLiveData(SelectionViewModel.FLASH_WORK_NAME)
			.observeForever { flashActive = !it.isNullOrEmpty() }

		// MobileAds.initialize must not run on the main thread.
		CoroutineScope(Dispatchers.IO).launch {
			MobileAds.initialize(app, InitializationConfig.Builder(AD_APP_ID).build()) {
				AppOpenAdPreloader.start(
					AD_UNIT_ID,
					PreloadConfiguration(AdRequest.Builder(AD_UNIT_ID).build())
				)
			}
		}
	}

	fun suppressNextShow() {
		suppressedAt = SystemClock.elapsedRealtime()
	}

	override fun onStop(owner: LifecycleOwner) {
		wasBackgrounded = true
	}

	override fun onStart(owner: LifecycleOwner) {
		if (wasBackgrounded) {
			wasBackgrounded = false
			pendingShow = true
		}
	}

	override fun onActivityResumed(activity: Activity) {
		if (pendingShow && activity is MainActivity) {
			pendingShow = false
			showAdIfAppropriate(activity)
		}
	}

	private fun showAdIfAppropriate(activity: Activity) {
		val now = SystemClock.elapsedRealtime()

		// Always consume the suppress flag, even if it already expired.
		val suppressed = suppressedAt != 0L && now - suppressedAt < SUPPRESS_WINDOW_MS
		suppressedAt = 0L

		if (isShowingAd || flashActive || suppressed) return
		if (lastShownAt != 0L && now - lastShownAt < MIN_INTERVAL_MS) return

		val ad = AppOpenAdPreloader.pollAd(AD_UNIT_ID)
		if (ad == null) {
			Log.d("kad", "No preloaded app open ad available.")
			return
		}
		ad.adEventCallback = object : AppOpenAdEventCallback {
			override fun onAdDismissedFullScreenContent() {
				isShowingAd = false
			}

			override fun onAdFailedToShowFullScreenContent(
				fullScreenContentError: FullScreenContentError,
			) {
				isShowingAd = false
			}
		}
		isShowingAd = true
		lastShownAt = now
		ad.show(activity)
	}

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
	override fun onActivityStarted(activity: Activity) {}
	override fun onActivityPaused(activity: Activity) {}
	override fun onActivityStopped(activity: Activity) {}
	override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
	override fun onActivityDestroyed(activity: Activity) {}
}
