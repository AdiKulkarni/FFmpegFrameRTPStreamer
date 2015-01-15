package com.davisECS.virtualfrontview;

import net.majorkernelpanic.streaming.rtsp.RtspServer;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * Streaming the camera output of a host device (server) to a connected peer
 * (client), using the library LibStreaming.
 */
public class MainActivity extends Activity {

	private final static String TAG = "VirtualFrontView";
	private TextView mUserIp;
	private static String mVideoIP;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mVideoIP = "rtsp://" + IpUtility.getIPAddress(true) + ":4002";

		mUserIp = (TextView) findViewById(R.id.user_ip);
		mUserIp.setText("RTP stream: " + mVideoIP);

		Button buttonStartCameraPreview = (Button) findViewById(R.id.startcamerapreview);
		Button buttonStopCameraPreview = (Button) findViewById(R.id.stopcamerapreview);

		// Set what happens when buttons are clicked
		buttonStartCameraPreview.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// Opens server activity
				Intent launchServer = new Intent(getApplicationContext(),
						ServerActivity.class);
				startActivity(launchServer);
			}
		});
		buttonStopCameraPreview
				.setOnClickListener(new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						finish();
					}
				});
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Stop RTSP server if it is running
		getApplicationContext().stopService(new Intent(this, RtspServer.class));
	}

}