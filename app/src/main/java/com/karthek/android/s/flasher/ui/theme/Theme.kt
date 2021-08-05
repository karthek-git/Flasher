package com.karthek.android.s.flasher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
	primary = BlueLight,
	primaryVariant = BlueLight,
	secondary = BlueLight,
	secondaryVariant = BlueLight,
	background = Color.Black,
	surface = Color.Black,
	error = Color(0xFFCF6679),
	onPrimary = Color.Black,
	onSecondary = Color.Black,
	onBackground = Color.White,
	onSurface = Color.White,
	onError = Color.Black
)


private val LightColorPalette = lightColors(
	primary = Blue600,
	primaryVariant = Blue600,
	secondary = Blue600,
	secondaryVariant = Blue600,
	background = Color.White,
	surface = Color.White,
	error = Color(0xFFB00020),
	onPrimary = Color.White,
	onSecondary = Color.White,
	onBackground = Color.Black,
	onSurface = Color.Black,
	onError = Color.White
)

@Composable
fun FlasherTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit) {
	val colors = if (darkTheme) {
		DarkColorPalette
	} else {
		LightColorPalette
	}

	MaterialTheme(
		colors = colors,
		typography = Typography,
		shapes = Shapes,
		content = content
	)
}