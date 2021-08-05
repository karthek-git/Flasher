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
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.work.WorkInfo
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import com.karthek.android.s.flasher.R
import com.karthek.android.s.flasher.SettingsActivity
import com.karthek.android.s.flasher.helpers.UsbMassStorageDevice
import com.karthek.android.s.flasher.state.SelectionViewModel
import com.karthek.android.s.flasher.state.selectedDevice
import com.karthek.android.s.flasher.state.selectedImage
import com.karthek.android.s.flasher.workers.PROGRESS_TAG
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SelectionScreen(viewModel: SelectionViewModel) {
	val coroutineScope = rememberCoroutineScope()
	val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
	ModalBottomSheetLayout(
		sheetState = sheetState,
		sheetContent = {
			DeviceSelectionSheet(deviceList = viewModel.devices) {
				viewModel.onSelectDevice(it)
				coroutineScope.launch { sheetState.hide() }
			}
		}) {
		Scaffold(topBar = { TopBar() }) {
			val scrollState = rememberScrollState()
			Column(modifier = Modifier.verticalScroll(scrollState)) {
				val flashWorkData by viewModel.flashProgress.observeAsState()
				val flashing = !flashWorkData.isNullOrEmpty()
				SelectDevice(enabled = !flashing, selectedDevice) {
					coroutineScope.launch { sheetState.show() }
				}
				SelectImage(enabled = !flashing, viewModel = viewModel)
				if (selectedDevice != null && selectedImage != null && !flashing)
					FlashButton(
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
}

@Composable
fun TopBar() {
	TopAppBar(
		title = { Text(text = stringResource(id = R.string.app_name)) },
		contentPadding = rememberInsetsPaddingValues(insets = LocalWindowInsets.current.statusBars),
		elevation = 8.dp,
		actions = {
			val context = LocalContext.current
			IconButton(onClick = {
				context.startActivity(Intent(context, SettingsActivity::class.java))
			}) {
				Icon(
					imageVector = Icons.Outlined.MoreVert,
					contentDescription = "",
					tint = MaterialTheme.colors.onSurface,
					modifier = Modifier
						.padding(start = 8.dp, end = 16.dp)
				)
			}
		}
	)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SelectDevice(enabled: Boolean, selectedDevice: UsbMassStorageDevice?, onClick: () -> Unit) {
	Column(modifier = Modifier.padding(16.dp)) {
		Text(
			text = "USB device",
			style = MaterialTheme.typography.subtitle2,
			modifier = Modifier.padding(start = 8.dp)
		)
		Card(
			enabled = enabled,
			onClick = onClick,
			modifier = Modifier
				.fillMaxWidth()
				.height(80.dp)
				.padding(vertical = 8.dp),
			shape = RoundedCornerShape(8.dp),
			elevation = 8.dp
		) {
			Row(
				horizontalArrangement = Arrangement.Center,
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier.padding(8.dp)
			) {
				Text(
					text = selectedDevice?.usbDevice?.fullName() ?: "SELECT USB DEVICE",
					style = MaterialTheme.typography.button
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SelectImage(enabled: Boolean, viewModel: SelectionViewModel) {
	val launcher =
		rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
			uri?.let { viewModel.putSelectedImage(it) }
		}
	Column(modifier = Modifier.padding(16.dp)) {
		Text(
			text = "Image",
			style = MaterialTheme.typography.subtitle2,
			modifier = Modifier.padding(start = 8.dp)
		)
		Card(
			enabled = enabled,
			onClick = { launcher.launch("application/*") },
			modifier = Modifier
				.fillMaxWidth()
				.height(80.dp)
				.padding(vertical = 8.dp),
			shape = RoundedCornerShape(8.dp),
			elevation = 8.dp
		) {
			Row(
				horizontalArrangement = Arrangement.Center,
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier.padding(8.dp)
			) {
				Text(
					text = selectedImage?.name ?: "SELECT ISO IMAGE",
					style = MaterialTheme.typography.button
				)
			}
		}
	}
}

@Composable
fun FlashButton(onClick: () -> Unit, modifier: Modifier) {
	Button(onClick = onClick, modifier = modifier) {
		Text(text = "WRITE")
	}
}

@Composable
fun FlashingState(
	workInfo: WorkInfo,
	onConfirm: () -> Unit,
	modifier: Modifier
) {
	when (workInfo.state) {
		WorkInfo.State.SUCCEEDED -> {
			FlashSuccessDialog("Image written successfully", onConfirm)
		}
		WorkInfo.State.RUNNING -> {
			FlashProgress(
				progress = workInfo.progress.getFloat(PROGRESS_TAG, 0f),
				modifier = modifier
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
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier
				.background(
					color = MaterialTheme.colors.surface
						.copy(0.88f)
						.compositeOver(MaterialTheme.colors.onSurface),
					shape = RoundedCornerShape(16.dp)
				)
				.padding(horizontal = 2.dp)
		) {
			Text(
				text = text,
				style = MaterialTheme.typography.body1,
				modifier = Modifier
					.padding(top = 32.dp, bottom = 16.dp)
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
		Text(text = "Writing Image...", style = MaterialTheme.typography.subtitle2)
		LinearProgressIndicator(
			progress = progress, modifier = Modifier
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
			contentPadding = rememberInsetsPaddingValues(
				insets = LocalWindowInsets.current.navigationBars,
			), modifier = Modifier
				.defaultMinSize(minHeight = 180.dp)
				.padding(vertical = 8.dp)
		) {
			item {
				Text(
					text = "DEVICES",
					color = MaterialTheme.colors.primary,
					style = MaterialTheme.typography.body2,
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
				style = MaterialTheme.typography.caption,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.alpha(0.7f)
			)
		}
	}
}

@ExperimentalMaterialApi
@Composable
fun ModalBottomSheetLayout(
	sheetState: ModalBottomSheetState,
	sheetContent: @Composable () -> Unit,
	content: @Composable () -> Unit
) {
	val coroutineScope = rememberCoroutineScope()
	BackHandler(enabled = sheetState.isVisible) {
		coroutineScope.launch { sheetState.hide() }
	}
	val scrimColor = if (MaterialTheme.colors.isLight) {
		MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
	} else {
		MaterialTheme.colors.surface.copy(alpha = 0.5f)
	}
	ModalBottomSheetLayout(
		sheetState = sheetState,
		sheetShape = RoundedCornerShape(8.dp),
		scrimColor = scrimColor,
		sheetContent = { sheetContent() }) {
		content()
	}
}

fun UsbDevice.fullName() = "$manufacturerName $productName"
