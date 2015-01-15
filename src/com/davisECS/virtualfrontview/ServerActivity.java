package com.davisECS.virtualfrontview;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public class ServerActivity extends Activity implements Session.Callback,
		SurfaceHolder.Callback {

	private final static String TAG = "VirtualFrontView";
	private int bitrate = 500000;
	private SurfaceView mSurfaceView;
	SurfaceHolder surfaceHolder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface);

		// Sets the port of the RTSP server to 4002
		Editor editor = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).edit();
		editor.putString(RtspServer.KEY_PORT, String.valueOf(4002));
		editor.commit();

		// Configures the SessionBuilder
		SessionBuilder.getInstance().setSurfaceView(mSurfaceView)
				.setPreviewOrientation(0).setContext(this)
				.setVideoQuality(new VideoQuality(640, 480, 30, bitrate))
				.setAudioEncoder(SessionBuilder.AUDIO_NONE)
				.setVideoEncoder(SessionBuilder.VIDEO_H264);
		
		// Starts the RTSP server
		getApplicationContext().startService(
				new Intent(getApplicationContext(), RtspServer.class));
	}

	@Override
	protected void onPause() {
		super.onPause();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// Stop RTSP server if it is running
		getApplicationContext().stopService(new Intent(this, RtspServer.class));

	}

	@Override
	protected void onStop() {
		super.onStop();
		finish();
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

}
