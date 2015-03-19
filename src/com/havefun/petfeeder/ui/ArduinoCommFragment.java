package com.havefun.petfeeder.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.hardware.usb.UsbDevice;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.havefun.petfeeder.R;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;

public class ArduinoCommFragment extends Fragment implements View.OnClickListener {

	private final String TAG = ArduinoCommFragment.class.getSimpleName();

	private Button searchBtn;
	private UsbManager usbManager;
	private ListView deviceList;
	private TextView statusMsg;
	private ProgressBar progressBar;

	private List<UsbSerialPort> usbSerialPorts = new ArrayList<UsbSerialPort>();
	private ArrayAdapter<UsbSerialPort> adapter;

	private static final int MESSAGE_SEARCH = 101;

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_SEARCH:
				searchDeviceList();
				break;
			default:
				super.handleMessage(msg);
				break;
			}
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
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View layout = inflater.inflate(R.layout.arduino_comm_view, container, false);

		searchBtn = (Button) layout.findViewById(R.id.search_btn);
		deviceList = (ListView) layout.findViewById(R.id.device_list);
		progressBar = (ProgressBar) layout.findViewById(R.id.progress_bar);
		statusMsg = (TextView) layout.findViewById(R.id.status_msg);

		searchBtn.setOnClickListener(this);

		usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);

		adapter = new ArrayAdapter<UsbSerialPort>(getActivity(), android.R.layout.simple_expandable_list_item_2, usbSerialPorts) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				final TwoLineListItem row;
				if (convertView == null) {
					final LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					row = (TwoLineListItem) inflater.inflate(android.R.layout.simple_list_item_2, null);
				} 
				else {
					row = (TwoLineListItem) convertView;
				}

				final UsbSerialPort port = usbSerialPorts.get(position);
				final UsbSerialDriver driver = port.getDriver();
				final UsbDevice device = driver.getDevice();

				final String title = String.format("Vendor %s Product %s",
						HexDump.toHexString((short) device.getVendorId()),
						HexDump.toHexString((short) device.getProductId()));
				row.getText1().setText(title);

				final String subtitle = driver.getClass().getSimpleName();
				row.getText2().setText(subtitle);

				return row;
			}

		};
		
		deviceList.setAdapter(adapter);

		deviceList.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.d(TAG, "Pressed item " + position);
				if (position >= usbSerialPorts.size()) {
					Log.w(TAG, "Illegal position.");
					return;
				}

				final UsbSerialPort port = usbSerialPorts.get(position);
				showConsoleActivity(port);
			}
		});

		return layout;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.search_btn:
			handler.sendEmptyMessage(MESSAGE_SEARCH);
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

	private void searchDeviceList() {
		
		progressBar.setVisibility(View.VISIBLE);
		statusMsg.setText(R.string.searching);

		new AsyncTask<Void, Void, List<UsbSerialPort>>() {
			@Override
			protected List<UsbSerialPort> doInBackground(Void... params) {
				Log.d(TAG, "Refreshing device list ...");
				SystemClock.sleep(1000);

				final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
				final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
				for (final UsbSerialDriver driver : drivers) {
					final List<UsbSerialPort> ports = driver.getPorts();
					Log.d(TAG, String.format("+ %s: %s port%s", driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "": "s"));
					result.addAll(ports);
				}

				return result;
			}

			@Override
			protected void onPostExecute(List<UsbSerialPort> result) {
				usbSerialPorts.clear();
				usbSerialPorts.addAll(result);
				adapter.notifyDataSetChanged();
				statusMsg.setText(String.format("%s device(s) found", Integer.valueOf(usbSerialPorts.size())));
				progressBar.setVisibility(View.INVISIBLE);
				Log.d(TAG, "Done refreshing, " + usbSerialPorts.size() + " entries found.");
			}

		}.execute((Void) null);
	}

	private void showConsoleActivity(UsbSerialPort port) {
		SerialConsoleActivity.show(getActivity(), port);
	}

}
