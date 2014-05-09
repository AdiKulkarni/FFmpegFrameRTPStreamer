package com.davisECS.virtualfrontview;

import java.io.IOException;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class ClientActivity extends Activity implements OnPreparedListener,
		SurfaceHolder.Callback {

	MediaPlayer mMediaPlayer;
	SurfaceHolder mSurfaceHolder;
	SurfaceView mSurfaceView;
	private static String mVideoIP = "";
	private static final String TAG = "VirtualFrontView";
	private static final String SERVER_IP = "server ip";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client);
		
		mVideoIP = getIntent().getStringExtra(SERVER_IP);
		
		mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		

		if (savedInstanceState == null) {

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.client, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
//		// Create a new media player and set the listeners
//		mMediaPlayer = new MediaPlayer();
//		
//		try {
//			mMediaPlayer.setDataSource("rtsp://" + mVideoIP
//					+ ":8988");
//		} catch (IllegalArgumentException | SecurityException
//				| IllegalStateException | IOException e) {
//			e.printStackTrace();
//			Log.e(TAG, "MediaPlayer error!");
//		}
//		// mMediaPlayer.setDisplay(holder);
//		mMediaPlayer.setScreenOnWhilePlaying(true);
//		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//		mMediaPlayer.prepareAsync();
//		mMediaPlayer.setOnPreparedListener(this);

		super.onResume();
	}

	@Override
	protected void onPause() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (mMediaPlayer != null) {

			mMediaPlayer.release();
			mMediaPlayer = null;
		}
		super.onPause();
	}
	
	
	@Override
	protected void onStop() {
		super.onStop();
		finish();
	}


	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(TAG, "Media started!");
		mMediaPlayer.start();

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setDisplay(mSurfaceHolder);
			mMediaPlayer.setDataSource("rtsp://" + mVideoIP
					+ ":8988");
			mMediaPlayer.prepare();
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setScreenOnWhilePlaying(true);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

}
