package net.majorkernelpanic.streaming.audio;

import java.io.IOException;
import java.lang.reflect.Field;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtp.AMRNBPacketizer;
import android.media.MediaRecorder;
import android.service.textservice.SpellCheckerService.Session;

/**
 * A class for streaming AAC from the camera of an android device using RTP.
 * You should use a {@link Session} instantiated with {@link SessionBuilder} instead of using this class directly.
 * Call {@link #setDestinationAddress(InetAddress)}, {@link #setDestinationPorts(int)} and {@link #setAudioQuality(AudioQuality)}
 * to configure the stream. You can then call {@link #start()} to start the RTP stream.
 * Call {@link #stop()} to stop the stream.
 */
public class AMRNBStream extends AudioStream {

	public AMRNBStream() {
		super();

		mPacketizer = new AMRNBPacketizer();

		setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		
		try {
			// RAW_AMR was deprecated in API level 16.
			Field deprecatedName = MediaRecorder.OutputFormat.class.getField("RAW_AMR");
			setOutputFormat(deprecatedName.getInt(null));
		} catch (Exception e) {
			setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
		}
		
		setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		
	}

	/**
	 * Starts the stream.
	 */
	public synchronized void start() throws IllegalStateException, IOException {
		if (!mStreaming) {
			configure();
			super.start();
		}
	}
	
	public synchronized void configure() throws IllegalStateException, IOException {
		super.configure();
		mMode = MODE_FFMPEG_API;
		mQuality = mRequestedQuality.clone();
	}
	
	/**
	 * Returns a description of the stream using SDP. It can then be included in an SDP file.
	 */	
	public String getSessionDescription() {
		return "m=audio "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
				"a=rtpmap:96 AMR/8000\r\n" +
				"a=fmtp:96 octet-align=1;\r\n";
	}

	@Override
	protected void encodeWithFFmpeg() throws IOException {
		//do nothing
	}

}
