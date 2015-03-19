package com.havefun.petfeeder.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceService {
	
	private Context context;
	private SharedPreferences sharedPref;
	
	public static final String PREF_NAME = "pet_feeder_pref";
	
	//** Twitter
	public static final String PREF_KEY_TWITTER_IS_LOGIN         = "twitter_is_login";
	public static final String PREF_KEY_TWITTER_OAUTH_TOKEN      = "twitter_oauth_token";
	public static final String PREF_KEY_TWITTER_OAUTH_SECRET     = "twitter_oauth_token_secret";
	public static final String PREF_KEY_TWITTER_USER_NAME        = "twitter_user_name";
	public static final String PREF_KEY_TWITTER_LAST_STATUS_TIME = "twitter_last_status_time";
	public static final String PREF_KEY_TWITTER_LAST_STATUS_ID   = "twitter_last_status_id";
	
	
	public PreferenceService(Context context) {
		this.context = context;
		sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
	}
	
	public boolean isTwitterLogin() {
		return sharedPref.getBoolean(PREF_KEY_TWITTER_IS_LOGIN, false);
	}
	
	public void setTwitterLogin(boolean value) {
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(PREF_KEY_TWITTER_IS_LOGIN, value);
		editor.commit();
	}
	
	public String getTwitterOauthToken() {
		return sharedPref.getString(PREF_KEY_TWITTER_OAUTH_TOKEN, "");
	}

	public void setTwitterOauthToken(String value) {
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(PREF_KEY_TWITTER_OAUTH_TOKEN, value);
		editor.commit();
	}
	
	public String getTwitterOauthSecret() {
		return sharedPref.getString(PREF_KEY_TWITTER_OAUTH_SECRET, "");
	}

	public void setTwitterOauthSecret(String value) {
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(PREF_KEY_TWITTER_OAUTH_SECRET, value);
		editor.commit();
	}
	
	public String getTwitterUserName() {
		return sharedPref.getString(PREF_KEY_TWITTER_USER_NAME, "");
	}

	public void setTwitterUserName(String value) {
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(PREF_KEY_TWITTER_USER_NAME, value);
		editor.commit();
	}
	
	public long getTwitterLastStatusTime() {
		return sharedPref.getLong(PREF_KEY_TWITTER_LAST_STATUS_TIME, 0);
	}

	public void setTwitterLastStatusTime(long value) {
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putLong(PREF_KEY_TWITTER_LAST_STATUS_TIME, value);
		editor.commit();
	}
	
	public long getTwitterLastStatusId() {
		return sharedPref.getLong(PREF_KEY_TWITTER_LAST_STATUS_ID, 0);
	}

	public void setTwitterLastStatusId(long value) {
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putLong(PREF_KEY_TWITTER_LAST_STATUS_ID, value);
		editor.commit();
	}

}
