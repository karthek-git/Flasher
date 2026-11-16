package com.karthek.android.s.flasher.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.karthek.android.s.flasher.R
import com.karthek.android.s.flasher.state.selectedDevice
import com.karthek.android.s.flasher.state.selectedImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.jahnen.libaums.core.usb.UsbCommunicationFactory.UnderlyingUsbCommunication
import me.jahnen.libaums.core.usb.UsbCommunicationFactory.registerCommunication
import me.jahnen.libaums.core.usb.UsbCommunicationFactory.underlyingUsbCommunication
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
				// The device is released and the stream closed on success, failure and
				// cancel, so the next attempt doesn't find the USB device still claimed.
				try {
					device.init()
					val blockDev = device.blockDevice ?: throw IOException("blockDev init error")
					val blockSize = blockDev.blockSize
					val byteBuffer = ByteBuffer.allocate(BLOCKS_CLUSTER_SIZE)
					val inputStream = applicationContext.contentResolver.openInputStream(image.uri)
						?: throw IOException("Unable to open InputStream")
					inputStream.use { input ->
						var bytesWritten = 0L
						var lastReportedStep = -1
						while (true) {
							// Throws CancellationException when the user taps Cancel, instead of
							// leaving the loop and reporting a half-written flash as a success.
							ensureActive()
							val progress = bytesWritten / image.size.toFloat()
							// Update the notification in 5 percent steps
							val step = (progress * 100).toInt() / PROGRESS_STEP_PERCENT
							if (step != lastReportedStep) {
								lastReportedStep = step
								setForegroundInfo(text = image.name, progress = progress)
							}
							val bytesRead = input.read(byteBuffer.array())
							if (bytesRead < 0) break
							if (bytesRead % blockSize != 0) {
								for (i in bytesRead until BLOCKS_CLUSTER_SIZE)
									byteBuffer.put(i, 0)
							}

							byteBuffer.position(0)
							blockDev.write((bytesWritten / blockSize).toLong(), byteBuffer)
							bytesWritten += BLOCKS_CLUSTER_SIZE
						}
					}
				} finally {
					runCatching { device.close() }
				}
			}
		}
		// Don't treat a cancel as a flash error: let it propagate so WorkManager marks the
		// work as cancelled (the finally block above has already released the device).
		val error = res.exceptionOrNull()
		if (error is CancellationException) throw error
		if (res.isFailure) {
			Log.e("FlashImageWorker", "Flash failed", res.exceptionOrNull())
			return Result.failure()
		}
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

		setForeground(
			ForegroundInfo(
				NOTIFICATION_ID_FLASH,
				notification,
				ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
			)
		)
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
private const val PROGRESS_STEP_PERCENT = 5