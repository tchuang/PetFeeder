package com.havefun.petfeeder.service;

import java.util.List;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.havefun.petfeeder.R;
import com.havefun.petfeeder.ui.CameraActivity;
import com.havefun.petfeeder.ui.FeederFragment;
import com.havefun.petfeeder.util.ArduinoUtil;
import com.havefun.petfeeder.util.PreferenceService;
import com.hoho.android.usbserial.driver.UsbSerialPort;

public class FeederService extends IntentService {
	
	private final String TAG = FeederService.class.getSimpleName();
	
	public static PendingIntent pendingIntent = null;
	public static Twitter twitter = null;
	
	public static String lastStatusScreenName;

	public FeederService() {
		super("FeederService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		PreferenceService sharedPref = new PreferenceService(getBaseContext());
		String consumerKey = getString(R.string.twitter_consumer_key);
		String consumerSecret = getString(R.string.twitter_consumer_secret);
		
		// check twitter timeline
		try {
			if (twitter == null) {
				ConfigurationBuilder builder = new ConfigurationBuilder();
				builder.setOAuthConsumerKey(consumerKey);
				builder.setOAuthConsumerSecret(consumerSecret);
				
				// Access Token
				String access_token = sharedPref.getTwitterOauthToken();
				// Access Token Secret
				String access_token_secret = sharedPref.getTwitterOauthSecret();
				
				AccessToken accessToken = new AccessToken(access_token, access_token_secret);
				twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
			}
			
			Paging paging = new Paging();
			paging.setCount(1);
			List<twitter4j.Status> statuses = twitter.getHomeTimeline(paging);
			
			twitter4j.Status status = statuses.get(0);
			
			Log.i(TAG, "# Twitter Latest Status: " + status.getText() + ", Id: " + status.getId() + ",  Created at " + status.getCreatedAt());
			
			// check and update status id if needed
			long lastStatusId = sharedPref.getTwitterLastStatusId();
			
			if (lastStatusId != status.getId()) {
				
				Message message = FeederFragment.mHandler.obtainMessage(FeederFragment.MESSAGE_UPDATE_SYSTEM_STATUS, "Found new twitter status.");
				FeederFragment.mHandler.sendMessage(message);
				
				// update id
				sharedPref.setTwitterLastStatusId(status.getId());
				lastStatusScreenName = status.getUser().getScreenName();
				// trigger feeder
				List<UsbSerialPort> ports = ArduinoUtil.discoverUsbPort(getBaseContext());
				if (ports.size() > 0) {
					ArduinoUtil.send(getBaseContext(), ports.get(0), "90\n");
				}
				// open camera
				Message cMessage = FeederFragment.mHandler.obtainMessage(FeederFragment.MESSAGE_UPDATE_SYSTEM_STATUS, "Start receiving motion sensor data.");
				FeederFragment.mHandler.sendMessage(cMessage);
				SystemClock.sleep(5000);
				
				Intent motionIntent = new Intent(getBaseContext(), MotionService.class);
				startService(motionIntent);
			}
			
		}
		catch (TwitterException e) {
			Log.e(TAG, "# Failed to retrieve twitter status: " + e.getMessage(), e);
			Message message = FeederFragment.mHandler.obtainMessage(FeederFragment.MESSAGE_UPDATE_SYSTEM_STATUS, "Failed to retrieve twitter status, code: " + e.getErrorCode());
			FeederFragment.mHandler.sendMessage(message);
		}
		
	}
	
	public static void startService(Context context, int interval) {
		
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		if (pendingIntent != null) am.cancel(pendingIntent);
		
		Intent intent = new Intent(context, FeederService.class);
		pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
	}
	
	public static void stopService(Context context) {
		
		if (pendingIntent != null) {
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			am.cancel(pendingIntent);
			pendingIntent = null;
		}
	}

}
