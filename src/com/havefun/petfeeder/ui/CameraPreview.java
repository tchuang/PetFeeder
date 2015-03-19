package com.havefun.petfeeder.ui;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

@SuppressWarnings("deprecation")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	
	private final String TAG = CameraPreview.class.getSimpleName();
	
	private SurfaceHolder mHolder;
    private Camera mCamera;

	public CameraPreview(Context context, Camera camera) {
		super(context);
		mCamera = camera;
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.setDisplayOrientation(90);
			mCamera.startPreview();
		}
		catch (IOException e) {
			Log.d(TAG, "# Error setting camera preview: " + e.getMessage());
        }
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.
		
		if (mHolder.getSurface() == null){
			// preview surface does not exist
			return;
		}
		
		try {
			mCamera.stopPreview();
		}
		catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }
		
		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();
        }
		catch (Exception e){
			Log.d(TAG, "# Error starting camera preview: " + e.getMessage());
		}
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the Camera preview in your activity.
	}

}
