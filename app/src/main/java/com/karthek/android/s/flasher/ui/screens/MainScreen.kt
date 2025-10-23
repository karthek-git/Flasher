package com.karthek.android.s.flasher.ui.screens

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.karthek.android.s.flasher.LicensesContent
import com.karthek.android.s.flasher.SettingsScreen
import com.karthek.android.s.flasher.state.SelectionViewModel
import com.karthek.android.s.flasher.ui.components.loadInterstitialAd
import com.karthek.android.s.flasher.ui.screens.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SelectionViewModel) {
	val backStack = rememberNavBackStack(Screen.Home)
	val onBackClick = { backStack.removeLastOrNull(); Unit }

	val context = LocalContext.current
	var interstitialAd: InterstitialAd? = null
	val interstitialAdLoader = {
		loadInterstitialAd(
			context,
			adLoadedCallback = { interstitialAd = it },
			adCompleteCallback = { interstitialAd = null }
		)
	}
	val activity = LocalActivity.current
	var adTimer = true
	val backgroundScope = rememberCoroutineScope()
	val showInterstitialAd = {
		interstitialAd?.let {
			if (adTimer) {
				it.show(activity!!)
				adTimer = false
			}
		}
		interstitialAdLoader()
	}
	LaunchedEffect(context) {
		interstitialAdLoader()
		backgroundScope.launch {
			delay(5000)
			showInterstitialAd()
		}
		backgroundScope.launch(Dispatchers.Default) {
			while (true) {
				delay(60000)
				adTimer = true
				withContext(Dispatchers.Main) {
					showInterstitialAd()
				}
			}
		}
	}

	NavDisplay(
		backStack = backStack,
		entryDecorators = listOf(
			rememberSceneSetupNavEntryDecorator(),
			rememberSavedStateNavEntryDecorator(),
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
		predictivePopTransitionSpec = {
			slideInHorizontally(initialOffsetX = { -it }) togetherWith
					slideOutHorizontally(targetOffsetX = { it })
		},
		entryProvider = entryProvider {
			entry<Screen.Home> {
				SelectionScreen(
					viewModel = viewModel,
					onMoreClick = {
						backStack.add(Screen.Settings)
						showInterstitialAd()
					})
			}
			entry<Screen.Settings> {
				SettingsScreen(
					onBackClick = onBackClick,
					onLicensesClick = { backStack.add(Screen.Licenses) })
			}
			entry<Screen.Licenses> { LicensesContent(onBackClick = onBackClick) }
		}
	)
}

