package com.karthek.android.s.flasher.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.github.mjdev.libaums.usb.UsbCommunicationFactory.UnderlyingUsbCommunication
import com.github.mjdev.libaums.usb.UsbCommunicationFactory.registerCommunication
import com.github.mjdev.libaums.usb.UsbCommunicationFactory.underlyingUsbCommunication
import com.karthek.android.s.flasher.R
import com.karthek.android.s.flasher.state.selectedDevice
import com.karthek.android.s.flasher.state.selectedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.jahnen.libaums.libusbcommunication.LibusbCommunicationCreator
import java.io.IOException
import java.nio.ByteBuffer


class FlashImageWorker(appContext: Context, params: WorkerParameters) :
	CoroutineWorker(appContext, params) {

	private val notificationManager =
		appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	override suspend fun doWork(): Result {
		val device = selectedDevice ?: return Result.failure()
		val image = selectedImage ?: return Result.failure()
		registerCommunication(LibusbCommunicationCreator())
		underlyingUsbCommunication = UnderlyingUsbCommunication.OTHER

		val res = withContext(Dispatchers.IO) {
			runCatching {
				device.init()
				val blockDev = device.blockDevice ?: throw IOException("blockDev init error")
				val blockSize = blockDev.blockSize
				val byteBuffer = ByteBuffer.allocate(BLOCKS_CLUSTER_SIZE)
				val inputStream = applicationContext.contentResolver.openInputStream(image.uri)
					?: throw IOException("Unable to open InputStream")
				var bytesWritten = 0
				while (isActive) {
					val progress = bytesWritten / image.size.toFloat()
					setForegroundInfo(text = image.name, progress = progress)
					val bytesRead =
						inputStream.read(byteBuffer.array())
					if (bytesRead < 0) break
					if (bytesRead % blockSize != 0) {
						for (i in bytesRead until BLOCKS_CLUSTER_SIZE)
							byteBuffer.put(i, 0)
					}

					byteBuffer.position(0)
					blockDev.write((bytesWritten / blockSize).toLong(), byteBuffer)
					bytesWritten += BLOCKS_CLUSTER_SIZE
				}
				device.close()
			}
		}
		if (res.isFailure) return Result.failure()
		return Result.success()
	}


	private suspend fun setForegroundInfo(text: String, progress: Float) {
		setProgress(workDataOf(PROGRESS_TAG to progress))
		val cancel = applicationContext.getString(android.R.string.cancel)
		// This PendingIntent can be used to cancel the worker
		val intent = WorkManager.getInstance(applicationContext)
			.createCancelPendingIntent(id)

		// Create a Notification channel if necessary
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createNotificationChannel()
		}

		val notification =
			NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID_FLASH)
				.setSmallIcon(R.drawable.ic_stat_default)
				.setContentTitle("Flashing...")
				.setContentText(text)
				.setOngoing(true)
				// Add the cancel action to the notification which can
				// be used to cancel the worker
				.addAction(android.R.drawable.ic_delete, cancel, intent)
				.setCategory(NotificationCompat.CATEGORY_PROGRESS)
				.setProgress(100, (progress * 100).toInt(), false)
				.build()

		setForeground(ForegroundInfo(NOTIFICATION_ID_FLASH, notification))
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private fun createNotificationChannel() {
		val channel =
			NotificationChannel(
				NOTIFICATION_CHANNEL_ID_FLASH,
				"Flash progress",
				NotificationManager.IMPORTANCE_LOW
			).apply {
				description = "Shows flash progress notifications"
			}
		notificationManager.createNotificationChannel(channel)
	}
}

const val PROGRESS_TAG = "progress"
const val NOTIFICATION_CHANNEL_ID_FLASH = "flash"
const val NOTIFICATION_ID_FLASH = 1
const val BLOCKS_CLUSTER_SIZE = 1048576