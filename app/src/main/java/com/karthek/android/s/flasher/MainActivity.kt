package com.karthek.android.s.flasher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.karthek.android.s.flasher.state.SelectionViewModel
import com.karthek.android.s.flasher.ui.screens.SelectionScreen
import com.karthek.android.s.flasher.ui.theme.FlasherTheme

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		WindowCompat.setDecorFitsSystemWindows(window, false)
		val viewModel by viewModels<SelectionViewModel>()
		setContent {
			FlasherTheme {
				val systemUiController = rememberSystemUiController()
				val useDarkIcons = MaterialTheme.colors.isLight
				SideEffect {
					systemUiController.setSystemBarsColor(Color.Transparent, useDarkIcons)
				}
				ProvideWindowInsets {
					Surface(color = MaterialTheme.colors.background) {
						SelectionScreen(viewModel)
					}
				}
			}
		}
	}
}
