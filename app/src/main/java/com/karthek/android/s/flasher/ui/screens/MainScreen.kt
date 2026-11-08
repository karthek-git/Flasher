package com.karthek.android.s.flasher.ui.screens

import android.os.SystemClock
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.karthek.android.s.flasher.LicensesContent
import com.karthek.android.s.flasher.SettingsScreen
import com.karthek.android.s.flasher.state.SelectionViewModel
import com.karthek.android.s.flasher.ui.components.loadInterstitialAd
import com.karthek.android.s.flasher.ui.screens.navigation.Screen

// Minimum gap between interstitials.
private const val INTERSTITIAL_MIN_INTERVAL_MS = 3 * 60 * 1000L

@Composable
fun MainScreen(viewModel: SelectionViewModel) {
	val backStack = rememberNavBackStack(Screen.Home)
	val onBackClick = { backStack.removeLastOrNull(); Unit }

	var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
	var lastInterstitialAt by remember { mutableLongStateOf(0L) }
	val activity = LocalActivity.current
	val loadAd = {
		loadInterstitialAd(
			adLoadedCallback = { interstitialAd = it },
			adCompleteCallback = { interstitialAd = null }
		)
	}
	// Preload only. Never shown on launch or on a timer (AdMob policy:
	// unexpected launch interstitials / accidental clicks).
	LaunchedEffect(Unit) { loadAd() }

	// Shown only at a natural break (leaving Settings), at most once per interval.
	val showInterstitialAd = {
		val ad = interstitialAd
		val now = SystemClock.elapsedRealtime()
		if (ad != null && activity != null &&
			(lastInterstitialAt == 0L || now - lastInterstitialAt >= INTERSTITIAL_MIN_INTERVAL_MS)
		) {
			ad.show(activity)
			lastInterstitialAt = now
			interstitialAd = null
			loadAd()
		} else if (ad == null) {
			loadAd()
		}
	}

	NavDisplay(
		backStack = backStack,
		entryDecorators = listOf(
			rememberSaveableStateHolderNavEntryDecorator(),
			rememberViewModelStoreNavEntryDecorator()
		),
		transitionSpec = {
			slideInHorizontally(initialOffsetX = { it }) togetherWith
					slideOutHorizontally(targetOffsetX = { -it })
		},
		popTransitionSpec = {
			slideInHorizontally(initialOffsetX = { -it }) togetherWith
					slideOutHorizontally(targetOffsetX = { it })
		},
		predictivePopTransitionSpec = { _ ->
			slideInHorizontally(initialOffsetX = { -it }) togetherWith
					slideOutHorizontally(targetOffsetX = { it })
		},
		entryProvider = entryProvider {
			entry<Screen.Home> {
				SelectionScreen(
					viewModel = viewModel,
					onMoreClick = { backStack.add(Screen.Settings) })
			}
			entry<Screen.Settings> {
				SettingsScreen(
					onBackClick = {
						onBackClick()
						showInterstitialAd()
					},
					onLicensesClick = { backStack.add(Screen.Licenses) })
			}
			entry<Screen.Licenses> { LicensesContent(onBackClick = onBackClick) }
		}
	)
}