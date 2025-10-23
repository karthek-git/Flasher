package com.karthek.android.s.flasher.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.Date

@Composable
fun AdComponent(adUnitId: String, adSize: AdSize, modifier: Modifier) {
	AndroidView(
		factory = { context ->
			AdView(context).apply {
				this.adUnitId = adUnitId
				setAdSize(adSize)
				loadAd(AdRequest.Builder().build())
			}
		},
		onRelease = { adView -> adView.destroy() },
		modifier = modifier
	)
}


@Composable
fun BannerAdComponent() {
	//val adUnitId = "ca-app-pub-2240982710415001/9756008428"
	val adUnitId = "ca-app-pub-3940256099942544/6300978111"
	AdComponent(
		adUnitId = adUnitId,
		adSize = AdSize.BANNER,
		modifier = Modifier
			.fillMaxWidth()
			.height(60.dp)
	)
}

@Composable
fun BannerAdComponentContainer() {
	Box(
		contentAlignment = Alignment.Center,
		modifier = Modifier
			.fillMaxWidth()
			.height(60.dp)
	) {
		Text("An Ad will appear here", style = MaterialTheme.typography.labelSmall)
		BannerAdComponent()
	}
}

fun loadInterstitialAd(
	context: Context,
	adLoadedCallback: (InterstitialAd) -> Unit,
	adCompleteCallback: () -> Unit,
) {
	InterstitialAd.load(
		context,
		"ca-app-pub-2240982710415001/7738065776",
		//"ca-app-pub-3940256099942544/1033173712",
		AdRequest.Builder().build(),
		object : InterstitialAdLoadCallback() {
			override fun onAdLoaded(ad: InterstitialAd) {
				Log.d("kad", "Ad was loaded.")
				adLoadedCallback(ad)
				ad.fullScreenContentCallback = object : FullScreenContentCallback() {
					override fun onAdDismissedFullScreenContent() {
						adCompleteCallback()
					}

					override fun onAdFailedToShowFullScreenContent(adError: AdError) {
						adCompleteCallback()
					}
				}
			}

			override fun onAdFailedToLoad(adError: LoadAdError) {
				Log.d("kad", adError.message)
			}
		},
	)
}


const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

class AppOpenAdManager {
	private var appOpenAd: AppOpenAd? = null
	private var isLoadingAd = false
	var isShowingAd = false

	fun loadAd(context: Context) {
		if (isLoadingAd || isAdAvailable()) {
			return
		}

		isLoadingAd = true
		val request = AdRequest.Builder().build()
		AppOpenAd.load(
			context, AD_UNIT_ID,
			request,
			AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
			object : AppOpenAd.AppOpenAdLoadCallback() {

				override fun onAdLoaded(ad: AppOpenAd) {
					Log.d("kad", "Ad was loaded.")
					appOpenAd = ad
					isLoadingAd = false
					val loadTime = Date().time
				}

				override fun onAdFailedToLoad(loadAdError: LoadAdError) {
					Log.d("kad", loadAdError.message)
					isLoadingAd = false;
				}
			})
	}

	fun showAdIfAvailable(
		activity: Activity,
		onShowAdCompleteCallback: () -> Unit,
	) {
		if (isShowingAd) {
			Log.d("kad", "The app open ad is already showing.")
			return
		}

		if (!isAdAvailable()) {
			Log.d("kad", "The app open ad is not ready yet.")
			onShowAdCompleteCallback()
			loadAd(activity)
			return
		}
		isShowingAd = true
		appOpenAd?.show(activity)
	}

	private fun isAdAvailable(): Boolean {
		return appOpenAd != null
	}
}