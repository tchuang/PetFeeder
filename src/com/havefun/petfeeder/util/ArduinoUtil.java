package com.havefun.petfeeder.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

public class ArduinoUtil {
	
	private static final String TAG = ArduinoUtil.class.getSimpleName();
	
	public static List<UsbSerialPort> discoverUsbPort(Context context) {
		
		List<UsbSerialPort> ports = new ArrayList<UsbSerialPort>();
		
		UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
		
		for (UsbSerialDriver driver : drivers) {
			Log.i(TAG, "# Found Device: Vendor " + driver.getDevice().getVendorId() + ", Product " + driver.getDevice().getProductId());
			List<UsbSerialPort> serialPorts = driver.getPorts();
			ports.addAll(serialPorts);
		}
		
		return ports;
	}
	
	public static void send(Context context, UsbSerialPort port, String msg) {
		
		UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		UsbDeviceConnection connection = manager.openDevice(port.getDriver().getDevice());
        
		if (connection == null) {
			Log.e(TAG, "# Opening Device Failed");
			return;
		}

		try {
			port.open(connection);
			port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
			Log.i(TAG, "# Sending Data...");
			
			//String output = "90\n";
			byte[] byteArray = msg.getBytes();
			port.write(byteArray, 1000);
		}
		catch (IOException e) {
			Log.e(TAG, "# Error Opening Device: " + e.getMessage(), e);
			e.printStackTrace();
		}
		finally {
			try {
				port.close();
			}
			catch (IOException ioe) {
				// Ignore.
			}
		}
		
	}
	
	public static void cleanPortBuffer(Context context) {
		
		List<UsbSerialPort> ports = ArduinoUtil.discoverUsbPort(context);
		
		if (ports.size() > 0) {

			UsbSerialPort port = ports.get(0);

			UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
			UsbDeviceConnection connection = manager.openDevice(port.getDriver().getDevice());
			
			if (connection == null) {
				Log.e(TAG, "# Opening Device Failed");
				return;
			}
			
			try {
				port.open(connection);
				port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

				byte buffer[] = new byte[4];
				int numBytesRead = port.read(buffer, 1000);
				Log.i(TAG, "# Read " + numBytesRead + " bytes, Data: " + new String(buffer));
				
			}
			catch (IOException e) {
				Log.e(TAG, "# Error Opening Device: " + e.getMessage(), e);
				e.printStackTrace();
			}
			finally {
				try {
					port.close();
				}
				catch (IOException ioe) {
					// Ignore.
				}
			}
		}
		
	}

}
