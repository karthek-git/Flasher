package com.karthek.android.s.flasher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.karthek.android.s.flasher.ui.theme.FlasherTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            FlasherTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val systemUiController = rememberSystemUiController()
                    val useDarkIcons = MaterialTheme.colors.isLight
                    SideEffect {
                        systemUiController.setSystemBarsColor(Color.Transparent, useDarkIcons)
                    }
                    ProvideWindowInsets {
                        val version = remember { getVersionString() }
                        SettingsScreen(version)
                    }
                }
            }
        }
    }

    private fun getVersionString(): String {
        return "${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_TYPE} (${BuildConfig.VERSION_CODE})"
    }

    private fun startLicensesActivity() {
        startActivity(Intent(this, LicensesActivity::class.java))
    }

    @Composable
    fun SettingsScreen(version: String) {
        CommonScaffold(activity = this, name = "About") { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                AboutItem(text = "Version", secondaryText = version)
                AboutItem(
                    text = "Open source licenses",
                    modifier = Modifier.clickable { startLicensesActivity() }
                )
            }
        }
    }

    @Composable
    fun AboutItem(text: String, modifier: Modifier = Modifier, secondaryText: String? = null) {
        Column(modifier = modifier) {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp),
                style = MaterialTheme.typography.subtitle1
            )
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .alpha(ContentAlpha.medium),
                    style = MaterialTheme.typography.body2
                )
            }
            Divider(modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
fun CommonScaffold(activity: Activity, name: String, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                contentPadding = rememberInsetsPaddingValues(insets = LocalWindowInsets.current.statusBars),
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "",
                        modifier = Modifier
                            .clickable { activity.finish() }
                            .padding(start = 12.dp, top = 2.dp)
                    )
                },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 8.dp
            )
        },
        modifier = Modifier.navigationBarsPadding(bottom = false)
    ) {
        content(it)
    }
}

