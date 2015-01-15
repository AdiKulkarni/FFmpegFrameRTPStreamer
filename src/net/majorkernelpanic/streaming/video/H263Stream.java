package net.majorkernelpanic.streaming.video;

import java.io.IOException;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtp.H263Packetizer;
import android.graphics.ImageFormat;
import android.hardware.Camera.CameraInfo;
import android.media.MediaRecorder;
import android.service.textservice.SpellCheckerService.Session;

/**
 * A class for streaming H.263 from the camera of an android device using RTP.
 * You should use a {@link Session} instantiated with {@link SessionBuilder} instead of using this class directly.
 * Call {@link #setDestinationAddress(InetAddress)}, {@link #setDestinationPorts(int)} and {@link #setVideoQuality(VideoQuality)}
 * to configure the stream. You can then call {@link #start()} to start the RTP stream.
 * Call {@link #stop()} to stop the stream.
 */
public class H263Stream extends VideoStream {

	/**
	 * Constructs the H.263 stream.
	 * Uses CAMERA_FACING_BACK by default.
	 * @throws IOException
	 */
	public H263Stream() throws IOException {
		this(CameraInfo.CAMERA_FACING_BACK);
	}	

	/**
	 * Constructs the H.263 stream.
	 * @param cameraId Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT 
	 * @throws IOException
	 */	
	public H263Stream(int cameraId) {
		super(cameraId);
		mCameraImageFormat = ImageFormat.NV21;
		mVideoEncoder = MediaRecorder.VideoEncoder.H263;
		mPacketizer = new H263Packetizer();
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
		return "m=video "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
				"a=rtpmap:96 H263-1998/90000\r\n";
	}

}
