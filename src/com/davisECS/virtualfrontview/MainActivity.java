package com.davisECS.virtualfrontview;

import net.majorkernelpanic.streaming.rtsp.RtspServer;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Streaming the camera output of a host device (server) to a connected peer
 * (client), using the library LibStreaming.
 */
public class MainActivity extends Activity {

	private final static String TAG = "VirtualFrontView";
	private Button mServerButton;
	private Button mClientButton;
	String videoSrc = "rtsp://10.0.1.51:1234";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Stop RTSP server if it is running
		getApplicationContext().stopService(new Intent(this,RtspServer.class));
		
		
		// Get button references
		mServerButton = (Button) findViewById(R.id.server_button);
		mClientButton = (Button) findViewById(R.id.client_button);

		// Set what happens when buttons are clicked
		mServerButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// Opens server activity
				Intent launchServer = new Intent(getApplicationContext(),
						ServerActivity.class);
				startActivity(launchServer);
			}
		});
		mClientButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoSrc));
				startActivity(intent); 
				
				
//				// Opens client activity
//				Intent launchClient = new Intent(getApplicationContext(),
//						ClientActivity.class);
//				startActivity(launchClient);
			}
		});
	}

}