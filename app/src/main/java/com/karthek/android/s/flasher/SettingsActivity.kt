package com.karthek.android.s.flasher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
				SettingsScreen(
					onBackClick = { this.finish() },
					onLicensesClick = {
						startActivity(Intent(this, LicensesActivity::class.java))
					})
			}
		}
	}
}

private fun getAppVersionString(): String {
	return "${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_TYPE} (${BuildConfig.VERSION_CODE})"
}

@Composable
fun SettingsScreen(onBackClick: () -> Unit, onLicensesClick: () -> Unit) {
	val context = LocalContext.current
	CommonScaffold(name = "About", onBackClick = onBackClick) { paddingValues ->
		ListComponent(
			modifier = Modifier
				.consumeWindowInsets(paddingValues)
				.padding(paddingValues)
				.fillMaxSize()
		) {
			CardComponent {
				NavigationListItem(headLineText = "Privacy Policy") {
					val uri =
						"https://policies.karthek.com/Flasher/-/blob/master/privacy.md".toUri()
					context.startActivity(Intent(Intent.ACTION_VIEW, uri))
				}
				HorizontalDivider()
				NavigationListItem(headLineText = "Open source licenses", onClick = onLicensesClick)
				HorizontalDivider()
				LicenseBottomSheet(
					"https://www.gnu.org/licenses/gpl-3.0.html",
					"GNU General Public License, version 3 or later"
				)
				HorizontalDivider()
				NavigationListItem(
					headLineText = "Version",
					supportingText = getAppVersionString()
				) {
					val intent = Intent(Intent.ACTION_VIEW).apply {
						data =
							"https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}".toUri()
						setPackage("com.android.vending")
					}
					context.startActivity(intent)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonScaffold(
	name: String,
	onBackClick: () -> Unit,
	content: @Composable (PaddingValues) -> Unit,
) {
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
	Scaffold(
		topBar = {
			LargeTopAppBar(
				title = { Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
				navigationIcon = {
					IconButton(onClick = onBackClick) {
						Icon(
							imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseBottomSheet(url: String, urlText: String) {
	var openBottomSheet by rememberSaveable { mutableStateOf(false) }

	NavigationListItem(headLineText = "Legal") { openBottomSheet = true }

	if (openBottomSheet) {
		ModalBottomSheet(onDismissRequest = { openBottomSheet = false }) {
			LicenseText(url, urlText)
		}
	}
}

@Composable
fun LicenseText(url: String, urlText: String) {
	val annotatedLicenseText = buildAnnotatedString {
		append("Copyright © Karthik Alapati\n\n")
		append("This application comes with absolutely no warranty. See the ")

		withLink(
			LinkAnnotation.Url(
				url,
				TextLinkStyles(
					style = SpanStyle(
						color = MaterialTheme.colorScheme.primary,
						textDecoration = TextDecoration.Underline
					)
				)
			)
		) {
			append(urlText)
		}

		append(" for details.")
	}
	Text(
		text = annotatedLicenseText,
		style = MaterialTheme.typography.labelLarge,
		modifier = Modifier
			.padding(16.dp)
			.padding(bottom = 16.dp)
	)
}

@Composable
fun NavigationListItem(
	headLineText: String,
	supportingText: String? = null,
	icon: ImageVector? = null,
	onClick: (() -> Unit)? = null,
) {
	val modifier = onClick?.let {
		Modifier.combinedClickable(
			onClick = it,
			onLongClick = {})
	} ?: Modifier

	Row(
		modifier = modifier
			.padding(vertical = 16.dp, horizontal = 8.dp)
	) {
		icon?.let {
			Icon(
				imageVector = it,
				contentDescription = headLineText,
				modifier = Modifier.padding(horizontal = 16.dp)
			)
		}
		Column(
			Modifier
				.weight(1f)
				.align(Alignment.CenterVertically)
				.padding(horizontal = 8.dp)
		) {
			Text(
				text = headLineText,
				style = MaterialTheme.typography.titleMedium,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
			supportingText?.let {
				Text(
					text = it,
					modifier = Modifier
						.alpha(0.7f)
						.padding(start = 1.dp),
					style = MaterialTheme.typography.bodyMedium,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
		}
		onClick?.let {
			Icon(
				Icons.Outlined.ChevronRight,
				contentDescription = "",
				modifier = Modifier
					.padding(horizontal = 4.dp)
					.align(Alignment.CenterVertically),
			)
		}
	}
}

@Composable
fun ListComponent(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
	Column(modifier = modifier.verticalScroll(rememberScrollState()), content = content)
}

@Composable
fun CardComponent(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
	Card(
		shape = RoundedCornerShape(16.dp),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp) // todo(temp fix for compose m3 car elevation color issue)
		),
		elevation = CardDefaults.cardElevation(4.dp),
		modifier = modifier
			.padding(horizontal = 16.dp, vertical = 8.dp),
		content = content
	)
}

