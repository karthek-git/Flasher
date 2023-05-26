package com.karthek.android.s.flasher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import com.karthek.android.s.flasher.ui.theme.AppTheme

class SettingsActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		WindowCompat.setDecorFitsSystemWindows(window, false)
		setContent { ScreenContent() }
	}

	@Composable
	fun ScreenContent() {
		AppTheme {
			Surface {
				val version = remember { getVersionString() }
				SettingsScreen(version)
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
				ListItem(
					headlineContent = { Text(text = "Version") },
					supportingContent = { Text(text = version, fontWeight = FontWeight.Light) }
				)
				Divider()
				ListItem(
					headlineContent = { Text(text = "Privacy Policy") },
					modifier = Modifier.clickable {
						val uri =
							Uri.parse("https://policies.karthek.com/Flasher/-/blob/master/privacy.md")
						startActivity(Intent(Intent.ACTION_VIEW, uri))
					})
				Divider()
				ListItem(headlineContent = { Text(text = "Open source licenses") },
					modifier = Modifier.clickable { startLicensesActivity() }
				)
			}
		}
	}

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonScaffold(activity: Activity, name: String, content: @Composable (PaddingValues) -> Unit) {
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
				navigationIcon = {
					IconButton(onClick = { activity.finish() }) {
						Icon(
							imageVector = Icons.Outlined.ArrowBack,
							contentDescription = stringResource(id = R.string.go_back)
						)
					}
				},
				scrollBehavior = scrollBehavior
			)
		}, modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
	) {
		content(it)
	}
}

