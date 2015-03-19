package com.havefun.petfeeder.ui;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.havefun.petfeeder.R;
import com.havefun.petfeeder.service.FeederService;
import com.havefun.petfeeder.service.MotionService;
import com.havefun.petfeeder.util.ArduinoUtil;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class FeederFragment extends Fragment implements View.OnClickListener {
	
	private final String TAG = FeederFragment.class.getSimpleName();
	
	private UsbSerialPort arduinoSerialPort = null;
	
	Button startServiceBtn, stopServiceBtn, manualFeedBtn, activateMotionServiceBtn, startListenBtn, stopListenBtn;
	TextView arduinoStatusMsg, twitterStatusMsg, sensorStatusMsg, serviceStatusMsg, systemStatusMsg;
	
	public static final int MESSAGE_DISCOVER = 101;
	public static final int MESSAGE_UPDATE_STATUS = 102;
	public static final int MESSAGE_UPDATE_SYSTEM_STATUS = 103;
	public static Handler mHandler;
	
	private static class FeederHandler extends Handler {
		
		private WeakReference<FeederFragment> mFragment; 
		
		FeederHandler(FeederFragment fragment) {
			mFragment = new WeakReference<FeederFragment>(fragment);
		}
		
		@Override
		public void handleMessage(Message msg) {
			
			FeederFragment fragment = mFragment.get();
			
			switch (msg.what) {
			case MESSAGE_DISCOVER:
				fragment.discoverSerialPort();
				break;
			case MESSAGE_UPDATE_STATUS:
				fragment.sensorStatusMsg.setText((String)msg.obj);
				break;
			case MESSAGE_UPDATE_SYSTEM_STATUS:
				fragment.systemStatusMsg.setText((String)msg.obj);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	}
	
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	private SerialInputOutputManager mSerialIoManager;

	private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			Log.d(TAG, "Runner stopped.");
		}

		@Override
		public void onNewData(final byte[] data) {
			
			updateReceivedData(data);
			
			/*
			SerialConsoleActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					SerialConsoleActivity.this.updateReceivedData(data);
				}
			});
			*/
		}
	};
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
		mHandler = new FeederHandler(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View layout = inflater.inflate(R.layout.feeder_view, container, false);
		
		startServiceBtn = (Button) layout.findViewById(R.id.start_service_btn);
		stopServiceBtn = (Button) layout.findViewById(R.id.stop_service_btn);
		manualFeedBtn = (Button) layout.findViewById(R.id.manual_feed_btn);
		startListenBtn = (Button) layout.findViewById(R.id.start_listen_btn);
		stopListenBtn = (Button) layout.findViewById(R.id.stop_listen_btn);
		activateMotionServiceBtn = (Button) layout.findViewById(R.id.activate_motion_service_btn);
		arduinoStatusMsg = (TextView) layout.findViewById(R.id.arduino_status_msg);
		twitterStatusMsg = (TextView) layout.findViewById(R.id.twitter_status_msg);
		sensorStatusMsg = (TextView) layout.findViewById(R.id.sensor_status_msg);
		serviceStatusMsg = (TextView) layout.findViewById(R.id.service_status_msg);
		systemStatusMsg = (TextView) layout.findViewById(R.id.system_status_msg);
		
		serviceStatusMsg.setText("Feeder Service Stop.");
		
		startServiceBtn.setOnClickListener(this);
		stopServiceBtn.setOnClickListener(this);
		manualFeedBtn.setOnClickListener(this);
		startListenBtn.setOnClickListener(this);
		stopListenBtn.setOnClickListener(this);
		activateMotionServiceBtn.setOnClickListener(this);
		
		mHandler.sendEmptyMessage(MESSAGE_DISCOVER);
		
		return layout;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start_service_btn:
			FeederService.startService(getActivity(), 60000);
			startServiceBtn.setVisibility(View.GONE);
			stopServiceBtn.setVisibility(View.VISIBLE);
			serviceStatusMsg.setText("Feeder Service Started.");
			//ArduinoUtil.cleanPortBuffer(getActivity());
			break;
		case R.id.stop_service_btn:
			FeederService.stopService(getActivity());
			startServiceBtn.setVisibility(View.VISIBLE);
			stopServiceBtn.setVisibility(View.GONE);
			serviceStatusMsg.setText("Feeder Service Stop.");
			break;
		case R.id.manual_feed_btn:
			if (arduinoSerialPort == null) {
				Toast.makeText(getActivity(), "Arduino Not Connected", Toast.LENGTH_LONG).show();
	        }
			else {
				Toast.makeText(getActivity(), "Manual Feed Start", Toast.LENGTH_LONG).show();
				ArduinoUtil.send(getActivity(), arduinoSerialPort, "90\n");
	        }
			break;
		case R.id.activate_motion_service_btn:
			Intent intent = new Intent(getActivity(), MotionService.class);
			getActivity().startService(intent);
			break;
		case R.id.start_listen_btn:
			
			final UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);

			UsbDeviceConnection connection = usbManager.openDevice(arduinoSerialPort.getDriver().getDevice());
			if (connection == null) {
				Log.e(TAG, "# Opening Device Failed");
				return;
			}

			try {
				arduinoSerialPort.open(connection);
				arduinoSerialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
			}
			catch (IOException e) {
				Log.e(TAG, "# Error setting up device: " + e.getMessage(), e);
				try {
					arduinoSerialPort.close();
				}
				catch (IOException ioe) {
					// Ignore.
				}
				arduinoSerialPort = null;
				return;
			}
			
			
			stopIoManager();
			startIoManager();
			break;
		case R.id.stop_listen_btn:
			
			if (arduinoSerialPort != null) {
				try {
					arduinoSerialPort.close();
				}
				catch (IOException ioe) {
					// Ignore.
				}
			}
			
			stopIoManager();
			break;
		default:
			break;
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void discoverSerialPort() {
		
		new AsyncTask<Void, Void, UsbSerialPort>() {
			@Override
			protected UsbSerialPort doInBackground(Void... params) {

				SystemClock.sleep(1000);
				
				List<UsbSerialPort> ports = ArduinoUtil.discoverUsbPort(getActivity());
				if (ports.size() > 0) {
					arduinoSerialPort = ports.get(0);
				}
				
				return arduinoSerialPort;
			}

			@Override
			protected void onPostExecute(UsbSerialPort port) {
				if (port != null) {
					UsbSerialDriver driver = port.getDriver();
					UsbDevice device = driver.getDevice();
					String deviceInfo = String.format("Vendor %s Product %s", Integer.toString(device.getVendorId()), Integer.toString(device.getProductId()));
					arduinoStatusMsg.setText(deviceInfo);
				}
			}

		}.execute((Void) null);
	}
	
	private void stopIoManager() {
		if (mSerialIoManager != null) {
			Log.i(TAG, "# Stopping io manager ...");
			mSerialIoManager.stop();
			mSerialIoManager = null;
		}
	}

	private void startIoManager() {
		if (arduinoSerialPort != null) {
			Log.i(TAG, "# Starting io manager ...");
			mSerialIoManager = new SerialInputOutputManager(arduinoSerialPort, mListener);
			mExecutor.submit(mSerialIoManager);
		}
	}

	private void updateReceivedData(byte[] data) {
		//final String msg = "Read " + data.length + " bytes: " + HexDump.dumpHexString(data);
		final String msg = "Read " + data.length + " bytes: " + new String(data);
		Log.i(TAG, "# " + msg);
		Message message = mHandler.obtainMessage(MESSAGE_UPDATE_STATUS, msg);
		mHandler.sendMessage(message);
		//sensorStatusMsg.setText(message);
	}

}

