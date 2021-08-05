package com.karthek.android.s.flasher.state

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.karthek.android.s.flasher.helpers.SelectedImage
import com.karthek.android.s.flasher.helpers.UsbMassStorageDevice
import com.karthek.android.s.flasher.workers.FlashImageWorker

class SelectionViewModel(private val appContext: Application) : AndroidViewModel(appContext) {
	val devices = UsbMassStorageDevice.getMassStorageDevices(appContext)

	val flashProgress =
		WorkManager.getInstance(appContext).getWorkInfosForUniqueWorkLiveData(FLASH_WORK_NAME)

	fun onSelectDevice(index: Int) {
		val device = devices[index]
		val permissionIntent =
			PendingIntent.getBroadcast(appContext, 0, Intent(ACTION_USB_PERMISSION), 0)
		appContext.registerReceiver(usbReceiver, IntentFilter(ACTION_USB_PERMISSION))
		val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
		usbManager.requestPermission(device.usbDevice, permissionIntent)
		selectedDevice = device
	}

	fun putSelectedImage(uri: Uri) {
		val cursor = appContext.contentResolver.query(uri, null, null, null, null) ?: return
		val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
		val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
		cursor.moveToFirst()
		selectedImage = SelectedImage(
			uri = uri,
			name = cursor.getString(nameIndex),
			size = cursor.getLong(sizeIndex)
		)
		cursor.close()
	}

	fun onFlashClick() {
		val flashWorkRequest = OneTimeWorkRequestBuilder<FlashImageWorker>()
			.build()
		WorkManager.getInstance(appContext).apply {
			enqueueUniqueWork(FLASH_WORK_NAME, ExistingWorkPolicy.REPLACE, flashWorkRequest)
		}
	}

	fun onSuccessConfirmed() {
		WorkManager.getInstance(appContext).pruneWork()
	}


	private val usbReceiver = object : BroadcastReceiver() {

		override fun onReceive(context: Context, intent: Intent) {
			if (ACTION_USB_PERMISSION == intent.action) {
				synchronized(this) {
					val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

					if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						selectedDevice = null
					} else {
						Log.d("usbPerm", "permission denied for device $device")
					}
				}
			}
		}
	}

	companion object {
		private const val ACTION_USB_PERMISSION = "com.karthek.android.s.flasher.USB_PERMISSION"
		const val FLASH_WORK_NAME = "flashWork"
	}

}

var selectedDevice by mutableStateOf<UsbMassStorageDevice?>(null)
var selectedImage by mutableStateOf<SelectedImage?>(null)