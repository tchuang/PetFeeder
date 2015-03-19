package com.havefun.petfeeder.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.havefun.petfeeder.R;

@SuppressWarnings("deprecation")
public class CameraActivity extends Activity implements View.OnClickListener {
	
	private final String TAG = CameraActivity.class.getSimpleName();
	
	private Camera mCamera;
	private CameraPreview mPreview;
	
	Button captureBtn;
	
	public static String lastPicturePath = "";
	
	public static Handler mHandler;
	
	public static final int MESSAGE_CAPTURE = 101;
	public static final int MESSAGE_EXIT = 102;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_preview);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		mCamera = getCameraInstance();
		
		mPreview = new CameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);
		
		captureBtn = (Button) findViewById(R.id.capture_btn);
		captureBtn.setOnClickListener(this);
		
		mHandler = new CameraHandler(this);
	}
	
	private static class CameraHandler extends Handler {
		
		private WeakReference<CameraActivity> mActivity; 
		
		CameraHandler(CameraActivity context) {
			mActivity = new WeakReference<CameraActivity>(context);
		}
		
		@Override
		public void handleMessage(Message msg) {
			
			CameraActivity activity = mActivity.get();
			
			switch (msg.what) {
			case MESSAGE_CAPTURE:
				activity.mCamera.autoFocus(activity.autoFocus);
				break;
			case MESSAGE_EXIT:
				activity.finish();
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	}
	
	public Camera getCameraInstance(){
		
		Camera c = null;
		
		//Search for the back facing camera
		int cameraId = -1;
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				cameraId = i;
				break;
			}
		}
		
		try {
			c = Camera.open(cameraId);	
		}
		catch (Exception e){
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}
	
	public void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		//when on Pause, release camera in order to be used from other applications
		releaseCamera();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.capture_btn:
			mCamera.autoFocus(autoFocus);
			break;
		default:
			break;
		}
	}
	
	private AutoFocusCallback autoFocus = new AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			if (success) mCamera.takePicture(null, null, mPicture);
		}
	};

	private PictureCallback mPicture = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			File pictureFile = getOutputMediaFile();
			if (pictureFile == null){
				Log.d(TAG, "# Error creating media file, check storage permissions.");
				return;
			}

			try {
				Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
				
				Matrix matrix = new Matrix();
				matrix.postRotate(90);
				bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
				
				FileOutputStream fos = new FileOutputStream(pictureFile);
				bmp.compress(Bitmap.CompressFormat.JPEG, 98, fos);
				//fos.write(data);
				fos.flush();
				fos.close();
				Toast.makeText(CameraActivity.this, "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG).show();
			}
			catch (FileNotFoundException e) {
				Log.d(TAG, "# File not found: " + e.getMessage());
			}
			catch (IOException e) {
				Log.d(TAG, "# Error accessing file: " + e.getMessage());
			}
			
			camera.startPreview();
		}
	};

	private File getOutputMediaFile() {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "PetFeeder");

		Log.d(TAG, "# mediaStorageDir: " + mediaStorageDir.getPath());
		
		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d(TAG, "# failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		lastPicturePath = mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg";
		File mediaFile = new File(lastPicturePath);

		return mediaFile;
	}

	private void releaseCamera() {
		// stop and release camera
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}

}
