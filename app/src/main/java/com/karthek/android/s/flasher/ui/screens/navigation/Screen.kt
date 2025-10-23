package com.karthek.android.s.flasher.ui.screens.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class Screen() : NavKey {
	@Serializable
	object Home : Screen()

	@Serializable
	object Settings : Screen()

	@Serializable
	object Licenses : Screen()
}
