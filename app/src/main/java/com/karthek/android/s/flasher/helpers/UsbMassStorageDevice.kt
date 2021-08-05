/*
 * (C) Copyright 2014-2019 magnusja <github@mgns.tech>
 */

package com.karthek.android.s.flasher.helpers

import android.content.Context
import android.hardware.usb.*
import android.util.Log
import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.driver.BlockDeviceDriverFactory
import com.github.mjdev.libaums.driver.scsi.commands.sense.MediaNotInserted
import com.github.mjdev.libaums.usb.UsbCommunication
import com.github.mjdev.libaums.usb.UsbCommunicationFactory
import java.io.IOException

// copied from com.github.mjdev.libaums.UsbMassStorageDevice.kt

class UsbMassStorageDevice
private constructor(
	private val usbManager: UsbManager,
	/**
	 * This returns the [android.hardware.usb.UsbDevice] which can be used
	 * to request permission for communication.
	 *
	 * @return Underlying [android.hardware.usb.UsbDevice] used for
	 * communication.
	 */
	val usbDevice: UsbDevice,
	private val usbInterface: UsbInterface,
	private val inEndpoint: UsbEndpoint,
	private val outEndpoint: UsbEndpoint
) {

	var blockDevice: BlockDeviceDriver? = null

	// TODO this is never used, should we only allow one init() call?
	private var inited = false

	private lateinit var usbCommunication: UsbCommunication

	/**
	 * Initializes the mass storage device and determines different things like
	 * for example the MBR or the file systems for the different partitions.
	 *
	 * @throws IOException
	 * If reading from the physical device fails.
	 * @throws IllegalStateException
	 * If permission to communicate with the underlying
	 * [UsbDevice] is missing.
	 * @see .getUsbDevice
	 */
	@Throws(IOException::class)
	fun init() {
		if (usbManager.hasPermission(usbDevice))
			setupDevice()
		else
			throw IllegalStateException("Missing permission to access usb device: $usbDevice")

		inited = true
	}

	private fun setupDevice() {
		usbCommunication = UsbCommunicationFactory
			.createUsbCommunication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint)
		val maxLun = ByteArray(1)
		usbCommunication.controlTransfer(161, 254, 0, usbInterface.id, maxLun, 1)

		Log.i(TAG, "MAX LUN " + maxLun[0].toInt())

		blockDevice = (0..maxLun[0])
			.map { lun ->
				val blockDevice =
					BlockDeviceDriverFactory.createBlockDevice(usbCommunication, lun = lun.toByte())
				try {
					blockDevice.init()
				} catch (e: MediaNotInserted) {
					// This LUN does not have media inserted. Ignore it.
				}
				blockDevice
			}[0]
	}


	/**
	 * Releases the [android.hardware.usb.UsbInterface] and closes the
	 * [android.hardware.usb.UsbDeviceConnection]. After calling this
	 * method no further communication is possible. That means you can not read
	 * or write from or to the partitions returned by [.getPartitions].
	 */
	fun close() {
		usbCommunication.close()
		inited = false
	}

	companion object {

		private val TAG = UsbMassStorageDevice::class.java.simpleName

		/**
		 * subclass 6 means that the usb mass storage device implements the SCSI
		 * transparent command set
		 */
		private const val INTERFACE_SUBCLASS = 6

		/**
		 * protocol 80 means the communication happens only via bulk transfers
		 */
		private const val INTERFACE_PROTOCOL = 80

		@JvmStatic
		fun UsbDevice.getMassStorageDevices(context: Context): List<UsbMassStorageDevice> {
			val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

			return (0 until this.interfaceCount)
				.map { getInterface(it) }
				.filter {
					// we currently only support SCSI transparent command set with
					// bulk transfers only!
					it.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
							&& it.interfaceSubclass == INTERFACE_SUBCLASS
							&& it.interfaceProtocol == INTERFACE_PROTOCOL
				}
				.map { usbInterface ->
					Log.i(TAG, "Found usb interface: $usbInterface")

					// Every mass storage device has exactly two endpoints
					// One IN and one OUT endpoint
					val endpointCount = usbInterface.endpointCount
					if (endpointCount != 2) {
						Log.w(TAG, "Interface endpoint count != 2")
					}

					var outEndpoint: UsbEndpoint? = null
					var inEndpoint: UsbEndpoint? = null

					for (j in 0 until endpointCount) {
						val endpoint = usbInterface.getEndpoint(j)
						Log.i(TAG, "Found usb endpoint: $endpoint")
						if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
							if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
								outEndpoint = endpoint
							} else {
								inEndpoint = endpoint
							}
						}
					}

					if (outEndpoint == null || inEndpoint == null) {
						Log.e(
							TAG,
							"Not all needed endpoints found. In: ${outEndpoint != null}, Out: ${outEndpoint != null}"
						)
						return@map null
					}

					return@map UsbMassStorageDevice(
						usbManager, this, usbInterface, inEndpoint, outEndpoint
					)
				}
				.filterNotNull()
		}

		/**
		 * This method iterates through all connected USB devices and searches for
		 * mass storage devices.
		 *
		 * @param context
		 * Context to get the [UsbManager]
		 * @return An array of suitable mass storage devices or an empty array if
		 * none could be found.
		 */
		@JvmStatic
		fun getMassStorageDevices(context: Context): Array<UsbMassStorageDevice> {
			val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

			return usbManager.deviceList
				.map {
					val device = it.value
					Log.i(TAG, "found usb device: $it")
					device.getMassStorageDevices(context)
				}
				.flatten()
				.toTypedArray()
		}
	}
}