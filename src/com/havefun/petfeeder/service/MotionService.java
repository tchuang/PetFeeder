package com.havefun.petfeeder.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

import com.havefun.petfeeder.R;
import com.havefun.petfeeder.ui.CameraActivity;
import com.havefun.petfeeder.ui.FeederFragment;
import com.havefun.petfeeder.util.ArduinoUtil;
import com.havefun.petfeeder.util.PreferenceService;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class MotionService extends IntentService {
	
	private final String TAG = MotionService.class.getSimpleName();
	
	public MotionService() {
		super("MotionService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		Message message;
		
		Log.i(TAG, "# Motion Service Start.");
		message = FeederFragment.mHandler.obtainMessage(FeederFragment.MESSAGE_UPDATE_SYSTEM_STATUS, "Motion Service Start.");
		FeederFragment.mHandler.sendMessage(message);
		
		PreferenceService sharedPref = new PreferenceService(getBaseContext());
		
		List<UsbSerialPort> ports = ArduinoUtil.discoverUsbPort(getBaseContext());
		
		boolean takePicture = false;
		
		if (ports.size() > 0) {

			UsbSerialPort port = ports.get(0);

			UsbManager manager = (UsbManager) getBaseContext().getSystemService(Context.USB_SERVICE);
			UsbDeviceConnection connection = manager.openDevice(port.getDriver().getDevice());
			
			if (connection == null) {
				Log.e(TAG, "# Opening Device Failed");
				return;
			}
			
			try {
				port.open(connection);
				port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
				
				boolean loopFlag = true;
				int counter = 1;

				while(loopFlag) {
					Log.i(TAG, "# Loop: " + counter);
						
					byte buffer[] = new byte[4];
					Log.i(TAG, "# before read");
					int numBytesRead = port.read(buffer, 2000);
					Log.i(TAG, "# after read");
					
					if (numBytesRead > 0) {
						String str = new String(buffer);
						Log.i(TAG, "# Read " + numBytesRead + " bytes, Data: " + str);
						loopFlag = false;
						takePicture = true;
					}
					
					counter++;
					//SystemClock.sleep(2000);
				}
				
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
		
		if (takePicture) {
			// open camera
			Intent cameraIntent = new Intent(this, CameraActivity.class);
			cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(cameraIntent);
			// take picture
			SystemClock.sleep(3000);
			CameraActivity.mHandler.sendEmptyMessage(CameraActivity.MESSAGE_CAPTURE);
			SystemClock.sleep(6000);
			CameraActivity.mHandler.sendEmptyMessage(CameraActivity.MESSAGE_EXIT);
			
			//post twitter
			try {
				String filePath = CameraActivity.lastPicturePath;
				String consumerKey = getString(R.string.twitter_consumer_key);
				String consumerSecret = getString(R.string.twitter_consumer_secret);
				
				ConfigurationBuilder builder = new ConfigurationBuilder();
				builder.setOAuthConsumerKey(consumerKey);
				builder.setOAuthConsumerSecret(consumerSecret);
				
				// Access Token
				String access_token = sharedPref.getTwitterOauthToken();
				// Access Token Secret
				String access_token_secret = sharedPref.getTwitterOauthSecret();
				
				AccessToken accessToken = new AccessToken(access_token, access_token_secret);
				Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
				
				// Update status
				long lastStatusId = sharedPref.getTwitterLastStatusId();
				StatusUpdate statusUpdate = new StatusUpdate("@"+ FeederService.lastStatusScreenName + " I'm eating now...");
				statusUpdate.inReplyToStatusId(lastStatusId);
				File file = new File(filePath);
				FileInputStream fis = new FileInputStream(file);
				//InputStream is = getResources().openRawResource(R.drawable.test_image);
				statusUpdate.setMedia("eating.jpg", fis);
				
				twitter4j.Status response;
				response = twitter.updateStatus(statusUpdate);
				
				sharedPref.setTwitterLastStatusId(response.getId());
				
				Log.d("Status", response.getText());
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			catch (TwitterException e) {
				Log.d("Failed to post!", e.getMessage());
				e.printStackTrace();
			}

		}
		
		Log.i(TAG, "# Motion Service End.");
		
		message = FeederFragment.mHandler.obtainMessage(FeederFragment.MESSAGE_UPDATE_SYSTEM_STATUS, "Motion Service Done.");
		FeederFragment.mHandler.sendMessage(message);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

}
