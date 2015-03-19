package com.havefun.petfeeder.ui;

import java.io.InputStream;
import java.util.List;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.havefun.petfeeder.R;
import com.havefun.petfeeder.service.FeederService;
import com.havefun.petfeeder.util.PreferenceService;

public class TwitterAuthFragment extends Fragment implements View.OnClickListener {
	
	/* Any number for uniquely distinguish your request */
	public static final int WEBVIEW_REQUEST_CODE = 100;

	private ProgressDialog pDialog;

	private static Twitter twitter;
	private static RequestToken requestToken;
	
	PreferenceService sharedPref;

	private EditText mShareEditText;
	private TextView userName;
	private View loginLayout;
	private View shareLayout;

	private String consumerKey = null;
	private String consumerSecret = null;
	private String callbackUrl = null;
	private String oAuthVerifier = null;	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		sharedPref = new PreferenceService(getActivity().getBaseContext());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		/* initializing twitter parameters from string.xml */
		initTwitterConfigs();

		/* Enabling strict mode */
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		View layout = inflater.inflate(R.layout.twitter_auth_view, container, false);
		
		loginLayout = (RelativeLayout) layout.findViewById(R.id.login_layout);
		shareLayout = (LinearLayout) layout.findViewById(R.id.share_layout);
		mShareEditText = (EditText) layout.findViewById(R.id.share_text);
		userName = (TextView) layout.findViewById(R.id.user_name);
		
		/* register button click listeners */
		layout.findViewById(R.id.btn_login).setOnClickListener(this);
		layout.findViewById(R.id.btn_share).setOnClickListener(this);
		layout.findViewById(R.id.btn_timeline).setOnClickListener(this);
		layout.findViewById(R.id.btn_start_service).setOnClickListener(this);
		layout.findViewById(R.id.btn_stop_service).setOnClickListener(this);

		/* Check if required twitter keys are set */
		if (TextUtils.isEmpty(consumerKey) || TextUtils.isEmpty(consumerSecret)) {
			Toast.makeText(getActivity(), "Twitter key and secret not configured", Toast.LENGTH_SHORT).show();
		}
		
		boolean isLoggedIn = sharedPref.isTwitterLogin();
		
		/*  if already logged in, then hide login layout and show share layout */
		if (isLoggedIn) {
			loginLayout.setVisibility(View.GONE);
			shareLayout.setVisibility(View.VISIBLE);

			String username = sharedPref.getTwitterUserName();
			userName.setText(getResources ().getString(R.string.hello) + username);

		} else {
			loginLayout.setVisibility(View.VISIBLE);
			shareLayout.setVisibility(View.GONE);

			Uri uri = getActivity().getIntent().getData();
			
			if (uri != null && uri.toString().startsWith(callbackUrl)) {
			
				String verifier = uri.getQueryParameter(oAuthVerifier);

				try {
					
					/* Getting oAuth authentication token */
					AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

					/* Getting user id form access token */
					long userID = accessToken.getUserId();
					final User user = twitter.showUser(userID);
					final String username = user.getName();

					/* save updated token */
					saveTwitterInfo(accessToken);

					loginLayout.setVisibility(View.GONE);
					shareLayout.setVisibility(View.VISIBLE);
					userName.setText(getString(R.string.hello) + username);
					
				} catch (Exception e) {
					Log.e("Failed to login Twitter!!", e.getMessage());
				}
			}

		}
		
		return layout;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	
	/**
	 * Saving user information, after user is authenticated for the first time.
	 * You don't need to show user to login, until user has a valid access toen
	 */
	private void saveTwitterInfo(AccessToken accessToken) {
		
		long userID = accessToken.getUserId();
		
		User user;
		try {
			user = twitter.showUser(userID);
		
			String username = user.getName();
			
			sharedPref.setTwitterOauthToken(accessToken.getToken());
			sharedPref.setTwitterOauthSecret(accessToken.getTokenSecret());
			sharedPref.setTwitterLogin(true);
			sharedPref.setTwitterUserName(username);

		} catch (TwitterException e1) {
			e1.printStackTrace();
		}
	}

	/* Reading twitter essential configuration parameters from strings.xml */
	private void initTwitterConfigs() {
		consumerKey = getString(R.string.twitter_consumer_key);
		consumerSecret = getString(R.string.twitter_consumer_secret);
		callbackUrl = getString(R.string.twitter_callback);
		oAuthVerifier = getString(R.string.twitter_oauth_verifier);
	}

	
	private void loginToTwitter() {
		
		if (!sharedPref.isTwitterLogin()) {
			final ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.setOAuthConsumerKey(consumerKey);
			builder.setOAuthConsumerSecret(consumerSecret);

			final Configuration configuration = builder.build();
			final TwitterFactory factory = new TwitterFactory(configuration);
			twitter = factory.getInstance();

			try {
				requestToken = twitter.getOAuthRequestToken(callbackUrl);

				/**
				 *  Loading twitter login page on webview for authorization 
				 *  Once authorized, results are received at onActivityResult
				 *  */
				final Intent intent = new Intent(getActivity(), TwitterWebViewActivity.class);
				intent.putExtra(TwitterWebViewActivity.EXTRA_URL, requestToken.getAuthenticationURL());
				startActivityForResult(intent, WEBVIEW_REQUEST_CODE);
				
			} catch (TwitterException e) {
				e.printStackTrace();
			}
		} else {

			loginLayout.setVisibility(View.GONE);
			shareLayout.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == Activity.RESULT_OK) {
			String verifier = data.getExtras().getString(oAuthVerifier);
			try {
				AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

				long userID = accessToken.getUserId();
				final User user = twitter.showUser(userID);
				String username = user.getName();
				
				saveTwitterInfo(accessToken);

				loginLayout.setVisibility(View.GONE);
				shareLayout.setVisibility(View.VISIBLE);
				userName.setText(getActivity().getResources().getString(R.string.hello) + username);

			} catch (Exception e) {
				Log.e("Twitter Login Failed", e.getMessage());
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onClick(View v) {
		
		Intent intent;
		
		switch (v.getId()) {
		case R.id.btn_login:
			loginToTwitter();
			break;
		case R.id.btn_share:
			final String status = mShareEditText.getText().toString();
			
			if (status.trim().length() > 0) {
				new updateTwitterStatus().execute(status);
			} else {
				Toast.makeText(getActivity(), "Message is empty!!", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.btn_timeline:
			//new getTwitterStatus().execute();
			break;
		case R.id.btn_start_service:
			intent = new Intent(getActivity(), FeederService.class);
			getActivity().startService(intent);
			break;
		case R.id.btn_stop_service:
			intent = new Intent(getActivity(), FeederService.class);
			getActivity().stopService(intent);
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
	
	class updateTwitterStatus extends AsyncTask<String, String, Void> {
		
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			pDialog = new ProgressDialog(getActivity());
			pDialog.setMessage("Posting to twitter...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		protected Void doInBackground(String... args) {

			String status = args[0];
			try {
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
				StatusUpdate statusUpdate = new StatusUpdate(status);
				InputStream is = getResources().openRawResource(R.drawable.test_image);
				statusUpdate.setMedia("test.jpg", is);
				
				twitter4j.Status response = twitter.updateStatus(statusUpdate);

				Log.d("Status", response.getText());
				
			} catch (TwitterException e) {
				Log.d("Failed to post!", e.getMessage());
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			
			/* Dismiss the progress dialog after sharing */
			pDialog.dismiss();
			
			Toast.makeText(getActivity(), "Posted to Twitter!", Toast.LENGTH_SHORT).show();

			// Clearing EditText field
			mShareEditText.setText("");
		}

	}

}

