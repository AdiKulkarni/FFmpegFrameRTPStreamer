package com.davisECS.virtualfrontview;

import java.io.IOException;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.os.Build;
import android.preference.PreferenceManager;

public class ServerActivity extends Activity implements OnClickListener,
		Session.Callback, SurfaceHolder.Callback, OnPreparedListener {

	private final static String TAG = "VirtualFrontView";

	private SurfaceView mSurfaceView;

	// For client video playback
	MediaPlayer mediaPlayer;
	SurfaceHolder surfaceHolder;
	String videoSrc = "rtsp://10.0.1.51:1234";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mSurfaceView = (SurfaceView) findViewById(R.id.surface);

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

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
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
}
