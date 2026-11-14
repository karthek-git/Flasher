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
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.AndroidViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.karthek.android.s.flasher.helpers.SelectedImage
import com.karthek.android.s.flasher.helpers.UsbMassStorageDevice
import com.karthek.android.s.flasher.ui.components.AppOpenAdManager
import com.karthek.android.s.flasher.workers.FlashImageWorker

class SelectionViewModel(private val appContext: Application) : AndroidViewModel(appContext) {
	val devices = UsbMassStorageDevice.getMassStorageDevices(appContext)

	val flashProgress =
		WorkManager.getInstance(appContext).getWorkInfosForUniqueWorkLiveData(FLASH_WORK_NAME)

	fun onSelectDevice(index: Int) {
		val device = devices[index]
		// The system fills in the device and result extras, so the PendingIntent must be
		// mutable. Restricting the intent to our package keeps it explicit, which is
		// required for mutable PendingIntents when targeting Android 14+.
		val intent = Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName)
		val flags = PendingIntent.FLAG_UPDATE_CURRENT or
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
		val permissionIntent = PendingIntent.getBroadcast(appContext, 0, intent, flags)
		val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
		AppOpenAdManager.suppressNextShow() // returning from the USB permission dialog
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
					val device = IntentCompat.getParcelableExtra(
						intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java
					)

					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						Log.d("usbPerm", "permission granted for device $device")
					} else {
						Log.d("usbPerm", "permission denied for device $device")
						selectedDevice = null
					}
				}
			}
		}
	}

	init {
		// Registered once for the lifetime of the ViewModel. Not exported: the result is
		// delivered by the system through our own PendingIntent.
		ContextCompat.registerReceiver(
			appContext,
			usbReceiver,
			IntentFilter(ACTION_USB_PERMISSION),
			ContextCompat.RECEIVER_NOT_EXPORTED
		)
	}

	override fun onCleared() {
		appContext.unregisterReceiver(usbReceiver)
	}

	companion object {
		private const val ACTION_USB_PERMISSION = "com.karthek.android.s.flasher.USB_PERMISSION"
		const val FLASH_WORK_NAME = "flashWork"
	}

}

var selectedDevice by mutableStateOf<UsbMassStorageDevice?>(null)
var selectedImage by mutableStateOf<SelectedImage?>(null)