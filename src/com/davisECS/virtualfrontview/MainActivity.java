package com.davisECS.virtualfrontview;

import java.io.IOException;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;

/**
 * A straightforward example of how to stream AMR and H.263 to some public IP
 * using libstreaming. Note that this example may not be using the latest
 * version of libstreaming !
 */
public class MainActivity extends Activity implements OnClickListener,
		Session.Callback, SurfaceHolder.Callback, OnPreparedListener {

	private final static String TAG = "MainActivity";

	private SurfaceView mSurfaceView;
	private Button mServerButton;
	private Button mClientButton;

	Object test;
	
	// For client video playback
	MediaPlayer mediaPlayer;
	SurfaceHolder surfaceHolder;
	String videoSrc = "rtsp://10.0.1.51:1234";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		mServerButton = (Button) findViewById(R.id.server_button);
		mClientButton = (Button) findViewById(R.id.client_button);

		// For client video
		//surfaceHolder.addCallback(this);

		test = this;
		Log.e(TAG, test.toString());
		
		mServerButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mServerButton.setEnabled(false);
				mClientButton.setEnabled(false);

				// Sets the port of the RTSP server to 1234
				Editor editor = PreferenceManager.getDefaultSharedPreferences(
						getApplicationContext()).edit();
				editor.putString(RtspServer.KEY_PORT, String.valueOf(1234));
				editor.commit();

				// Configures the SessionBuilder
				SessionBuilder.getInstance().setSurfaceView(mSurfaceView)
						.setPreviewOrientation(90)
						.setContext(getApplicationContext())
						.setAudioEncoder(SessionBuilder.AUDIO_NONE)
						.setVideoEncoder(SessionBuilder.VIDEO_H264);
				// Starts the RTSP server
				getApplicationContext().startService(
						new Intent(getApplicationContext(), RtspServer.class));
			}
		});

		mClientButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Client code
				mServerButton.setEnabled(false);
				mClientButton.setEnabled(false);
				mSurfaceView = (SurfaceView) findViewById(R.id.surface);
				surfaceHolder.addCallback(this);
				surfaceHolder = mSurfaceView.getHolder();

			}
		});

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setDisplay(surfaceHolder);
			mediaPlayer.setDataSource(videoSrc);
			mediaPlayer.prepare();
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBitrareUpdate(long bitrate) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSessionError(int reason, int streamType, Exception e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPreviewStarted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSessionConfigured() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSessionStarted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSessionStopped() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		
	}

}