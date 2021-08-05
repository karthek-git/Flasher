package com.karthek.android.s.flasher

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.karthek.android.s.flasher.ui.theme.FlasherTheme
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LicensesActivity : ComponentActivity() {

    private val title = "Open source licenses"
    private var libs by mutableStateOf<List<Library>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        lifecycleScope.launchWhenCreated {
            libs = withContext(Dispatchers.Default) {
                Libs(this@LicensesActivity).libraries.sorted()
            }
        }
        setContent {
            FlasherTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val systemUiController = rememberSystemUiController()
                    val useDarkIcons = MaterialTheme.colors.isLight
                    SideEffect {
                        systemUiController.setSystemBarsColor(Color.Transparent, useDarkIcons)
                    }
                    ProvideWindowInsets {
                        LicensesContent()
                    }
                }
            }
        }
    }

    @Composable
    fun LicensesContent() {
        val lazyListState = rememberLazyListState()
        var license by remember { mutableStateOf(title) }
        var nav by remember { mutableStateOf(0) }
        var licenseText by remember { mutableStateOf("") }
        BackHandler(nav != 0) {
            nav = 0
            license = title
        }
        CommonScaffold(activity = this, name = license) {
            val insetPaddingValues =
                rememberInsetsPaddingValues(insets = LocalWindowInsets.current.navigationBars)
            if (nav == 1) {
                LicenseViewer(insetPaddingValues, licenseText)
            } else {
                LicensesMenu(lazyListState, insetPaddingValues) {
                    license = it.licenseName
                    nav = 1
                    licenseText = it.licenseDescription
                }
            }
        }
    }

    @Composable
    fun LicensesMenu(
        state: LazyListState,
        paddingValues: PaddingValues,
        callback: (License) -> Unit
    ) {
        if (libs != null) {
            LazyColumn(state = state, contentPadding = paddingValues) {
                items(libs!!) {
                    Column(modifier = Modifier
                        .clickable {
                            it.licenses?.let { it1 -> callback(it1.first()) }
                        }
                        .padding(start = 16.dp)) {
                        Text(
                            text = it.libraryName, modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        )
                        Divider()
                    }
                }
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .size(64.dp)
                    .wrapContentSize(Alignment.Center),
                strokeWidth = 4.dp
            )
        }
    }

    @Composable
    fun LicenseViewer(paddingValues: PaddingValues, text: String) {
        val htmlDesc = remember { HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT) }
        AndroidView(factory = {
            TextView(it).apply {
                movementMethod = LinkMovementMethod.getInstance()
            }
        }, update = {
            it.text = htmlDesc
        },
            modifier = Modifier
                .padding(paddingValues)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
        )
    }

}
