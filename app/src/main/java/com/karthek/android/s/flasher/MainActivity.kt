package com.karthek.android.s.flasher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.google.android.gms.ads.MobileAds
import com.karthek.android.s.flasher.state.SelectionViewModel
import com.karthek.android.s.flasher.ui.screens.MainScreen
import com.karthek.android.s.flasher.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		WindowCompat.setDecorFitsSystemWindows(window, false)
		setContent { ScreenContent() }

		val backgroundScope = CoroutineScope(Dispatchers.IO)
		backgroundScope.launch {
			MobileAds.initialize(this@MainActivity) {}
		}
	}

	@Composable
	fun ScreenContent() {
		val viewModel by viewModels<SelectionViewModel>()
		AppTheme {
			Surface(modifier = Modifier.fillMaxSize()) {
				MainScreen(viewModel)
			}
		}
	}
}