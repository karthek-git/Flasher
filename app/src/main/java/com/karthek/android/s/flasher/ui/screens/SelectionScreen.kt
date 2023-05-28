package com.karthek.android.s.flasher.ui.screens

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.work.WorkInfo
import com.karthek.android.s.flasher.R
import com.karthek.android.s.flasher.SettingsActivity
import com.karthek.android.s.flasher.helpers.UsbMassStorageDevice
import com.karthek.android.s.flasher.state.SelectionViewModel
import com.karthek.android.s.flasher.state.selectedDevice
import com.karthek.android.s.flasher.state.selectedImage
import com.karthek.android.s.flasher.workers.PROGRESS_TAG
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(viewModel: SelectionViewModel) {
	val coroutineScope = rememberCoroutineScope()
	var openBottomSheet by rememberSaveable { mutableStateOf(false) }
	val sheetState = rememberModalBottomSheetState()

	if (openBottomSheet) {
		Column(modifier = Modifier.fillMaxSize()) {
			ModalBottomSheetLayout(
				onDismissRequest = { openBottomSheet = false }, sheetState = sheetState
			) {
				DeviceSelectionSheet(deviceList = viewModel.devices) {
					viewModel.onSelectDevice(it)
					coroutineScope.launch {
						sheetState.hide()
					}.invokeOnCompletion {
						if (!(sheetState.isVisible)) {
							openBottomSheet = false
						}
					}
				}
			}
		}
	}

	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
	Scaffold(
		topBar = { TopBar(scrollBehavior) },
		contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars),
		modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
	) { paddingValues ->

		val scrollState = rememberScrollState()
		Column(
			modifier = Modifier
				.padding(paddingValues)
				.verticalScroll(scrollState)
		) {
			val flashWorkData by viewModel.flashProgress.observeAsState()
			val flashing = !flashWorkData.isNullOrEmpty()
			SelectDevice(enabled = !flashing, selectedDevice) {
				openBottomSheet = true
			}
			SelectImage(enabled = !flashing, viewModel = viewModel)
			if (selectedDevice != null && selectedImage != null && !flashing) FlashButton(
				onClick = viewModel::onFlashClick,
				modifier = Modifier.align(Alignment.CenterHorizontally)
			)
			if (flashing) {
				FlashingState(
					workInfo = flashWorkData!![0],
					viewModel::onSuccessConfirmed,
					modifier = Modifier
						.padding(32.dp)
						.align(Alignment.CenterHorizontally)
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(scrollBehavior: TopAppBarScrollBehavior) {
	Box {
		TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) }, actions = {
			val context = LocalContext.current
			IconButton(onClick = {
				context.startActivity(Intent(context, SettingsActivity::class.java))
			}) {
				Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "")
			}
		}, scrollBehavior = scrollBehavior
		)
	}
}

@Composable
fun SelectionCard(enabled: Boolean, title: String, text: String, onClick: () -> Unit) {
	Column(modifier = Modifier.padding(16.dp)) {
		Text(
			text = title,
			style = MaterialTheme.typography.titleSmall,
			modifier = Modifier.padding(start = 8.dp)
		)
		ElevatedCard(
			modifier = Modifier
				.fillMaxWidth()
				.height(80.dp)
				.padding(vertical = 8.dp)
				.clickable(enabled = enabled, onClick = onClick),
			shape = RoundedCornerShape(8.dp),
			elevation = CardDefaults.elevatedCardElevation(4.dp)
		) {
			Row(
				horizontalArrangement = Arrangement.Center,
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxSize()
					.padding(8.dp)
			) {
				Text(
					text = text, style = MaterialTheme.typography.labelLarge
				)
			}
		}
	}
}


@Composable
fun SelectDevice(enabled: Boolean, selectedDevice: UsbMassStorageDevice?, onClick: () -> Unit) {
	SelectionCard(
		enabled = enabled,
		title = "USB device",
		text = selectedDevice?.usbDevice?.fullName() ?: "SELECT USB DEVICE",
		onClick = onClick
	)
}

@Composable
fun SelectImage(enabled: Boolean, viewModel: SelectionViewModel) {
	val launcher =
		rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
			uri?.let { viewModel.putSelectedImage(it) }
		}
	SelectionCard(enabled = enabled,
		title = "Image",
		text = selectedImage?.name ?: "SELECT ISO IMAGE",
		onClick = { launcher.launch("application/*") })
}

@Composable
fun FlashButton(onClick: () -> Unit, modifier: Modifier) {
	Button(onClick = onClick, modifier = modifier) {
		Text(text = "WRITE")
	}
}

@Composable
fun FlashingState(
	workInfo: WorkInfo, onConfirm: () -> Unit, modifier: Modifier
) {
	when (workInfo.state) {
		WorkInfo.State.SUCCEEDED -> {
			FlashSuccessDialog("Image written successfully", onConfirm)
		}

		WorkInfo.State.RUNNING -> {
			FlashProgress(
				progress = workInfo.progress.getFloat(PROGRESS_TAG, 0f), modifier = modifier
			)
		}

		WorkInfo.State.CANCELLED -> {
			FlashSuccessDialog(text = "Image write cancelled", onConfirm)
		}

		WorkInfo.State.FAILED -> {
			FlashSuccessDialog(text = "Failed to write Image", onConfirm)
		}

		else -> {
			Log.v("state", "is ${workInfo.state}")
		}
	}
}

@Composable
fun FlashSuccessDialog(text: String, onConfirm: () -> Unit) {
	Dialog(onDismissRequest = onConfirm) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
				.background(
					color = MaterialTheme.colorScheme.surface
						.copy(0.88f)
						.compositeOver(MaterialTheme.colorScheme.onSurface),
					shape = RoundedCornerShape(16.dp)
				)
				.padding(horizontal = 2.dp)
		) {
			Text(
				text = text,
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
			)
			TextButton(
				onClick = onConfirm, modifier = Modifier
					.fillMaxWidth()
					.padding(4.dp)
			) {
				Text(text = stringResource(id = android.R.string.ok))
			}
		}
	}
}


@Composable
fun FlashProgress(progress: Float, modifier: Modifier) {
	Column(modifier = modifier) {
		Text(text = "Writing Image...", style = MaterialTheme.typography.titleSmall)
		LinearProgressIndicator(
			progress = progress,
			modifier = Modifier
				.padding(vertical = 16.dp)
				.height(8.dp)
				.clip(RoundedCornerShape(4.dp))
		)
	}
}

@Composable
fun DeviceSelectionSheet(deviceList: Array<UsbMassStorageDevice>, onClick: (Int) -> Unit) {
	if (deviceList.isEmpty()) {
		Text(
			text = "No devices found",
			textAlign = TextAlign.Center,
			modifier = Modifier
				.fillMaxWidth()
				.padding(64.dp)
		)
	} else {
		LazyColumn(
			modifier = Modifier
				.defaultMinSize(minHeight = 180.dp)
				.padding(vertical = 8.dp)
		) {
			item {
				Text(
					text = "DEVICES",
					color = MaterialTheme.colorScheme.primary,
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.padding(start = 16.dp, top = 8.dp)
				)
			}
			itemsIndexed(deviceList) { index, device ->
				DeviceItem(device = device) { onClick(index) }
			}
		}
	}
}

@Composable
fun DeviceItem(device: UsbMassStorageDevice, onClick: () -> Unit) {
	Row(
		modifier = Modifier
			.clickable(onClick = onClick)
			.fillMaxWidth()
			.padding(vertical = 16.dp)
	) {
		Icon(
			imageVector = Icons.Outlined.Usb,
			contentDescription = "",
			modifier = Modifier
				.padding(horizontal = 16.dp)
				.align(Alignment.CenterVertically)
		)
		Column {
			Text(
				text = device.usbDevice.fullName(),
				fontWeight = FontWeight.SemiBold,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
			Text(
				text = device.usbDevice.deviceName,
				style = MaterialTheme.typography.bodySmall,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.alpha(0.7f)
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetLayout(
	onDismissRequest: () -> Unit, sheetState: SheetState, sheetContent: @Composable () -> Unit
) {
	val coroutineScope = rememberCoroutineScope()
	BackHandler(enabled = sheetState.isVisible) {
		coroutineScope.launch { sheetState.hide() }
	}
	ModalBottomSheet(
		onDismissRequest = onDismissRequest,
		sheetState = sheetState,
		windowInsets = WindowInsets(0,0,0,0),
		content = { sheetContent() }
	)
}

fun UsbDevice.fullName() = "$manufacturerName $productName"
