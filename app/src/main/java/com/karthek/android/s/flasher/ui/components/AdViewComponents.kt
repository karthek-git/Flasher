package com.karthek.android.s.flasher.ui.components

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
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback

@Composable
fun AdComponent(adUnitId: String, adSize: AdSize, modifier: Modifier) {
	AndroidView(
		factory = { context ->
			AdView(context).apply {
				loadAd(
					BannerAdRequest.Builder(adUnitId, adSize).build(),
					object : AdLoadCallback<BannerAd> {
						override fun onAdLoaded(ad: BannerAd) {
							Log.d("kad", "Banner ad was loaded.")
						}

						override fun onAdFailedToLoad(adError: LoadAdError) {
							Log.d("kad", adError.message)
						}
					}
				)
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
	adLoadedCallback: (InterstitialAd) -> Unit,
	adCompleteCallback: () -> Unit,
) {
	InterstitialAd.load(
		AdRequest.Builder("ca-app-pub-2240982710415001/7738065776").build(),
		//AdRequest.Builder("ca-app-pub-3940256099942544/1033173712").build(),
		object : AdLoadCallback<InterstitialAd> {
			override fun onAdLoaded(ad: InterstitialAd) {
				Log.d("kad", "Ad was loaded.")
				adLoadedCallback(ad)
				ad.adEventCallback = object : InterstitialAdEventCallback {
					override fun onAdDismissedFullScreenContent() {
						adCompleteCallback()
					}

					override fun onAdFailedToShowFullScreenContent(
						fullScreenContentError: FullScreenContentError,
					) {
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