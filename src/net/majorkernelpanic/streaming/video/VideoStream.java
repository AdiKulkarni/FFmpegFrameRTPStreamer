package net.majorkernelpanic.streaming.video;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import net.majorkernelpanic.streaming.MediaStream;
import net.majorkernelpanic.streaming.Stream;
import net.majorkernelpanic.streaming.exceptions.CameraInUseException;
import net.majorkernelpanic.streaming.exceptions.InvalidSurfaceException;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.hw.NV21Convertor;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;

import com.android.myffmpegx264lib.frameEncoder;

/**
 * Don't use this class directly.
 */
public abstract class VideoStream extends MediaStream {

	protected final static String TAG = "VideoStream";

	protected VideoQuality mRequestedQuality = VideoQuality.DEFAULT_VIDEO_QUALITY
			.clone();
	protected VideoQuality mQuality = mRequestedQuality.clone();
	protected SurfaceHolder.Callback mSurfaceHolderCallback = null;
	protected SurfaceView mSurfaceView = null;
	protected SharedPreferences mSettings = null;
	protected int mVideoEncoder, mCameraId = 0;
	protected int mRequestedOrientation = 0, mOrientation = 0;
	protected Camera mCamera;
	protected Thread mCameraThread;
	protected Looper mCameraLooper;

	protected boolean mCameraOpenedManually = true;
	protected boolean mFlashEnabled = false;
	protected boolean mSurfaceReady = false;
	protected boolean mUnlocked = false;
	protected boolean mPreviewStarted = false;
	protected boolean mUpdated = false;

	protected String mMimeType;
	protected String mEncoderName;
	protected int mEncoderColorFormat;
	protected int mCameraImageFormat;
	protected int mMaxFps = 0;

	private byte[] outData;
	private byte[] outBytes = new byte[10000000];
	private int[] outFrameSize = new int[1];
	private int mCount = 0;
	private frameEncoder encoder;

	/**
	 * Don't use this class directly. Uses CAMERA_FACING_BACK by default.
	 */
	public VideoStream() {
		this(CameraInfo.CAMERA_FACING_BACK);
	}

	/**
	 * Don't use this class directly
	 * 
	 * @param camera
	 *            Can be either CameraInfo.CAMERA_FACING_BACK or
	 *            CameraInfo.CAMERA_FACING_FRONT
	 */
	@SuppressLint("InlinedApi")
	public VideoStream(int camera) {
		super();
		setCamera(camera);
	}

	/**
	 * Sets the camera that will be used to capture video. You can call this
	 * method at any time and changes will take effect next time you start the
	 * stream.
	 * 
	 * @param camera
	 *            Can be either CameraInfo.CAMERA_FACING_BACK or
	 *            CameraInfo.CAMERA_FACING_FRONT
	 */
	public void setCamera(int camera) {
		CameraInfo cameraInfo = new CameraInfo();
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == camera) {
				mCameraId = i;
				break;
			}
		}
	}

	/**
	 * Switch between the front facing and the back facing camera of the phone.
	 * If {@link #startPreview()} has been called, the preview will be briefly
	 * interrupted. If {@link #start()} has been called, the stream will be
	 * briefly interrupted. You should not call this method from the main thread
	 * if you are already streaming.
	 * 
	 * @throws IOException
	 * @throws RuntimeException
	 **/
	public void switchCamera() throws RuntimeException, IOException {
		if (Camera.getNumberOfCameras() == 1)
			throw new IllegalStateException("Phone only has one camera !");
		boolean streaming = mStreaming;
		boolean previewing = mCamera != null && mCameraOpenedManually;
		mCameraId = (mCameraId == CameraInfo.CAMERA_FACING_BACK) ? CameraInfo.CAMERA_FACING_FRONT
				: CameraInfo.CAMERA_FACING_BACK;
		setCamera(mCameraId);
		stopPreview();
		mFlashEnabled = false;
		if (previewing)
			startPreview();
		if (streaming)
			start();
	}

	/**
	 * Returns the id of the camera currently selected. Can be either
	 * {@link CameraInfo#CAMERA_FACING_BACK} or
	 * {@link CameraInfo#CAMERA_FACING_FRONT}.
	 */
	public int getCamera() {
		return mCameraId;
	}

	/**
	 * Sets a Surface to show a preview of recorded media (video). You can call
	 * this method at any time and changes will take effect next time you call
	 * {@link #start()}.
	 */
	public synchronized void setSurfaceView(SurfaceView view) {
		mSurfaceView = view;
		if (mSurfaceHolderCallback != null && mSurfaceView != null
				&& mSurfaceView.getHolder() != null) {
			mSurfaceView.getHolder().removeCallback(mSurfaceHolderCallback);
		}
		if (mSurfaceView.getHolder() != null) {
			mSurfaceHolderCallback = new Callback() {
				@Override
				public void surfaceDestroyed(SurfaceHolder holder) {
					mSurfaceReady = false;
					stopPreview();
					Log.d(TAG, "Surface destroyed !");
				}

				@Override
				public void surfaceCreated(SurfaceHolder holder) {
					mSurfaceReady = true;
				}

				@Override
				public void surfaceChanged(SurfaceHolder holder, int format,
						int width, int height) {
					Log.d(TAG, "Surface Changed !");
				}
			};
			mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);
			mSurfaceReady = true;
		}
	}

	/** Turns the LED on or off if phone has one. */
	public synchronized void setFlashState(boolean state) {
		// If the camera has already been opened, we apply the change
		// immediately
		if (mCamera != null) {

			Parameters parameters = mCamera.getParameters();

			// We test if the phone has a flash
			if (parameters.getFlashMode() == null) {
				// The phone has no flash or the choosen camera can not toggle
				// the flash
				throw new RuntimeException("Can't turn the flash on !");
			} else {
				parameters.setFlashMode(state ? Parameters.FLASH_MODE_TORCH
						: Parameters.FLASH_MODE_OFF);
				try {
					mCamera.setParameters(parameters);
					mFlashEnabled = state;
				} catch (RuntimeException e) {
					mFlashEnabled = false;
					throw new RuntimeException("Can't turn the flash on !");
				} finally {
				}
			}
		} else {
			mFlashEnabled = state;
		}
	}

	/**
	 * Toggles the LED of the phone if it has one. You can get the current state
	 * of the flash with {@link VideoStream#getFlashState()}.
	 */
	public synchronized void toggleFlash() {
		setFlashState(!mFlashEnabled);
	}

	/** Indicates whether or not the flash of the phone is on. */
	public boolean getFlashState() {
		return mFlashEnabled;
	}

	/**
	 * Sets the orientation of the preview.
	 * 
	 * @param orientation
	 *            The orientation of the preview
	 */
	public void setPreviewOrientation(int orientation) {
		mRequestedOrientation = orientation;
		mUpdated = false;
	}

	/**
	 * Sets the configuration of the stream. You can call this method at any
	 * time and changes will take effect next time you call {@link #configure()}
	 * .
	 * 
	 * @param videoQuality
	 *            Quality of the stream
	 */
	public void setVideoQuality(VideoQuality videoQuality) {
		if (!mRequestedQuality.equals(videoQuality)) {
			mRequestedQuality = videoQuality.clone();
			mUpdated = false;
		}
	}

	/**
	 * Returns the quality of the stream.
	 */
	public VideoQuality getVideoQuality() {
		return mRequestedQuality;
	}

	/**
	 * Some data (SPS and PPS params) needs to be stored when
	 * {@link #getSessionDescription()} is called
	 * 
	 * @param prefs
	 *            The SharedPreferences that will be used to save SPS and PPS
	 *            parameters
	 */
	public void setPreferences(SharedPreferences prefs) {
		mSettings = prefs;
	}

	/**
	 * Configures the stream. You need to call this before calling
	 * {@link #getSessionDescription()} to apply your configuration of the
	 * stream.
	 */
	public synchronized void configure() throws IllegalStateException,
			IOException {
		super.configure();
		mOrientation = mRequestedOrientation;
	}

	/**
	 * Starts the stream. This will also open the camera and dispay the preview
	 * if {@link #startPreview()} has not aready been called.
	 */
	public synchronized void start() throws IllegalStateException, IOException {
		if (!mPreviewStarted)
			mCameraOpenedManually = false;
		super.start();
		Log.d(TAG, "Stream configuration: FPS: " + mQuality.framerate
				+ " Width: " + mQuality.resX + " Height: " + mQuality.resY);
	}

	/** Stops the stream. */
	public synchronized void stop() {
		if (mCamera != null) {
			if (mMode == MODE_FFMPEG_API) {
				mCamera.setPreviewCallbackWithBuffer(null);
			}
			super.stop();
			closeEncoder();
			// We need to restart the preview
			if (!mCameraOpenedManually) {
				destroyCamera();
			} else {
				try {
					startPreview();
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized void startPreview() throws CameraInUseException,
			InvalidSurfaceException, RuntimeException {

		mCameraOpenedManually = true;
		if (!mPreviewStarted) {
			createCamera();
			updateCamera();
		}
	}

	/**
	 * Stops the preview.
	 */
	public synchronized void stopPreview() {
		mCameraOpenedManually = false;
		stop();
	}

	/**
	 * Video encoding is done by a ffmpeg using libx264.
	 */
	protected void encodeWithFFmpeg() throws RuntimeException, IOException {

		Log.d(TAG, "Video encoded using the MediaCodec API with a buffer");

		PipedInputStream in = new PipedInputStream();
		final PipedOutputStream out = new PipedOutputStream(in);

		// Updates the parameters of the camera if needed
		createCamera();
		updateCamera();

		// Estimates the framerate of the camera
		measureFramerate();

		final NV21Convertor converter = new NV21Convertor();
		converter.setSize(mQuality.resX, mQuality.resY);
		converter.setPlanar(true);

		// Starts the preview if needed
		if (!mPreviewStarted) {
			try {
				mCamera.startPreview();
				mPreviewStarted = true;
			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			}
		}

		setFFmpegEncoder();

		Camera.PreviewCallback callback = new Camera.PreviewCallback() {
			// long now = System.nanoTime() / 1000, oldnow = now, i = 0;

			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				// oldnow = now;
				// now = System.nanoTime() / 1000;
				// if (i++ > 3) {
				// i = 0;
				// Log.d(TAG, "Measured: " + 1000000L / (now - oldnow)
				// + " fps.");
				// }
				try {
					if (data == null)
						Log.d(TAG, "ERRORRR");

					else {
						byte[] convertedData = converter.convert(data);
						encoder.encodeFrame(convertedData,
								convertedData.length, mCount, outBytes,
								outFrameSize);
						outData = new byte[outFrameSize[0]];
						for (int i = 0; i < outFrameSize[0]; i++)
							outData[i] = outBytes[i];
						out.write(outData, 0, outFrameSize[0]);
						mCount++;

					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					mCamera.addCallbackBuffer(data);
				}
			}
		};

		for (int i = 0; i < 10; i++)
			mCamera.addCallbackBuffer(new byte[mQuality.resX * mQuality.resY
					* 3 / 2]);
		mCamera.setPreviewCallbackWithBuffer(callback);

		// The packetizer encapsulates the bit stream in an RTP stream and send
		// it over the network

		mPacketizer.setInputStream((InputStream) in);
		mPacketizer.start();
		mStreaming = true;

	}

	public void closeEncoder() {
		try {
			encoder.close();
			encoder.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setFFmpegEncoder() {
		encoder = new frameEncoder();
		encoder.setFps(mQuality.framerate);
		encoder.setOutHeight(mQuality.resY);
		encoder.setInHeight(mQuality.resY);
		encoder.setOutWidth(mQuality.resX);
		encoder.setInWidth(mQuality.resX);
		encoder.setBFrameFrq(0);
		encoder.setBitrate(mQuality.bitrate);
		encoder.setSliceMaxSize(1390);
		encoder.setVbvBufferSize(0);
		encoder.setIThreads(10);
		if (encoder.open()) {
			Log.i(TAG, "Opened encoder");
		} else {
			Log.i(TAG, "Failed to open the encoder");
		}
	}

	/**
	 * Returns a description of the stream using SDP. This method can only be
	 * called after {@link Stream#configure()}.
	 * 
	 * @throws IllegalStateException
	 *             Thrown when {@link Stream#configure()} wa not called.
	 */
	public abstract String getSessionDescription() throws IllegalStateException;

	/**
	 * Opens the camera in a new Looper thread so that the preview callback is
	 * not called from the main thread If an exception is thrown in this Looper
	 * thread, we bring it back into the main thread.
	 * 
	 * @throws RuntimeException
	 *             Might happen if another app is already using the camera.
	 */
	private void openCamera() throws RuntimeException {
		final Semaphore lock = new Semaphore(0);
		final RuntimeException[] exception = new RuntimeException[1];
		mCameraThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				mCameraLooper = Looper.myLooper();
				try {
					mCamera = Camera.open(mCameraId);
				} catch (RuntimeException e) {
					exception[0] = e;
				} finally {
					lock.release();
					Looper.loop();
				}
			}
		});
		mCameraThread.start();
		lock.acquireUninterruptibly();
		if (exception[0] != null)
			throw new CameraInUseException(exception[0].getMessage());
	}

	protected synchronized void createCamera() throws RuntimeException {
		if (mSurfaceView == null)
			throw new InvalidSurfaceException("Invalid surface !");
		if (mSurfaceView.getHolder() == null || !mSurfaceReady)
			throw new InvalidSurfaceException("Invalid surface !");

		if (mCamera == null) {
			openCamera();
			mUpdated = false;
			mUnlocked = false;
			mCamera.setErrorCallback(new Camera.ErrorCallback() {
				@Override
				public void onError(int error, Camera camera) {
					// On some phones when trying to use the camera facing front
					// the media server will die
					// Whether or not this callback may be called really depends
					// on the phone
					if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
						// In this case the application must release the camera
						// and instantiate a new one
						Log.e(TAG, "Media server died !");
						// We don't know in what thread we are so stop needs to
						// be synchronized
						mCameraOpenedManually = false;
						stop();
					} else {
						Log.e(TAG, "Error unknown with the camera: " + error);
					}
				}
			});

			try {

				// If the phone has a flash, we turn it on/off according to
				// mFlashEnabled
				// setRecordingHint(true) is a very nice optimization if you
				// plane to only use the Camera for recording
				Parameters parameters = mCamera.getParameters();
				if (parameters.getFlashMode() != null) {
					parameters
							.setFlashMode(mFlashEnabled ? Parameters.FLASH_MODE_TORCH
									: Parameters.FLASH_MODE_OFF);
				}
				parameters.setRecordingHint(true);
				mCamera.setParameters(parameters);
				mCamera.setDisplayOrientation(mOrientation);

				try {

					mCamera.setPreviewDisplay(mSurfaceView.getHolder());

				} catch (IOException e) {
					throw new InvalidSurfaceException("Invalid surface !");
				}

			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			}

		}
	}

	protected synchronized void destroyCamera() {
		if (mCamera != null) {
			if (mStreaming)
				super.stop();
			lockCamera();
			mCamera.stopPreview();
			try {
				mCamera.release();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage() != null ? e.getMessage()
						: "unknown error");
			}
			mCamera = null;
			mCameraLooper.quit();
			mUnlocked = false;
			mPreviewStarted = false;
		}
	}

	protected synchronized void updateCamera() throws RuntimeException {

		// The camera is already correctly configured
		if (mUpdated)
			return;

		if (mPreviewStarted) {
			mPreviewStarted = false;
			mCamera.stopPreview();
		}

		Parameters parameters = mCamera.getParameters();
		mQuality = VideoQuality.determineClosestSupportedResolution(parameters,
				mQuality);
		int[] max = VideoQuality.determineMaximumSupportedFramerate(parameters);

		double ratio = (double) mQuality.resX / (double) mQuality.resY;
		mSurfaceView.requestAspectRatio(ratio);

		parameters.setPreviewFormat(mCameraImageFormat);
		parameters.setPreviewSize(mQuality.resX, mQuality.resY);
		parameters.setPreviewFpsRange(max[0], max[1]);

		try {
			mCamera.setParameters(parameters);
			mCamera.setDisplayOrientation(mOrientation);
			mCamera.startPreview();
			mPreviewStarted = true;
			mUpdated = true;
		} catch (RuntimeException e) {
			destroyCamera();
			throw e;
		}
	}

	protected void lockCamera() {
		if (mUnlocked) {
			Log.d(TAG, "Locking camera");
			try {
				mCamera.reconnect();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
			mUnlocked = false;
		}
	}

	protected void unlockCamera() {
		if (!mUnlocked) {
			Log.d(TAG, "Unlocking camera");
			try {
				mCamera.unlock();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
			mUnlocked = true;
		}
	}

	/**
	 * Computes the average frame rate at which the preview callback is called.
	 * We will then use this average framerate with the MediaCodec. Blocks the
	 * thread in which this function is called.
	 */
	private void measureFramerate() {
		final Semaphore lock = new Semaphore(0);

		final Camera.PreviewCallback callback = new Camera.PreviewCallback() {
			int i = 0, t = 0;
			long now, oldnow, count = 0;

			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				i++;
				now = System.nanoTime() / 1000;
				if (i > 3) {
					t += now - oldnow;
					count++;
				}
				if (i > 20) {
					mQuality.framerate = (int) (1000000 / (t / count) + 1);
					lock.release();
				}
				oldnow = now;
			}
		};

		mCamera.setPreviewCallback(callback);

		try {
			lock.tryAcquire(2, TimeUnit.SECONDS);
			Log.d(TAG, "Actual framerate: " + mQuality.framerate);
			if (mSettings != null) {
				Editor editor = mSettings.edit();
				editor.putInt(PREF_PREFIX + "fps" + mRequestedQuality.framerate
						+ "," + mCameraImageFormat + ","
						+ mRequestedQuality.resX + mRequestedQuality.resY,
						mQuality.framerate);
				editor.commit();
			}
		} catch (InterruptedException e) {
		}

		mCamera.setPreviewCallback(null);

	}

}
