package net.majorkernelpanic.streaming.rtp;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.util.Log;

/**
 * 
 * RFC 3984.
 * 
 * H.264 streaming over RTP.
 * 
 * Must be fed with an InputStream containing H.264 NAL units preceded by their
 * length (4 bytes). The stream must start with mpeg4 or 3gpp header, it will be
 * skipped.
 * 
 */
public class H264Packetizer extends AbstractPacketizer implements Runnable {

	public final static String TAG = "H264Packetizer";

	private Thread t = null;
	private int naluLength = 0;
	private long delay = 0, oldtime = 0;
	private Statistics stats = new Statistics();
	private byte[] sps = null, pps = null, stapa = null;
	byte[] header = new byte[5];
	private int count = 0;

	public H264Packetizer() {
		super();
		socket.setClockFrequency(90000);
	}

	public void start() {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}

	public void stop() {
		if (t != null) {
			try {
				is.close();
			} catch (IOException e) {
			}
			t.interrupt();
			try {
				t.join();
			} catch (InterruptedException e) {
			}
			t = null;
		}
	}

	public void setStreamParameters(byte[] pps, byte[] sps) {
		this.pps = pps;
		this.sps = sps;

		if (pps != null && sps != null) {
			// A STAP-A NAL (NAL type 24) containing the sps and pps of the
			// stream
			stapa = new byte[sps.length + pps.length + 5];
			stapa[0] = 24;
			stapa[1] = (byte) (sps.length >> 8);
			stapa[2] = (byte) (sps.length & 0xFF);
			stapa[sps.length + 1] = (byte) (pps.length >> 8);
			stapa[sps.length + 2] = (byte) (pps.length & 0xFF);
			System.arraycopy(sps, 0, stapa, 3, sps.length);
			System.arraycopy(pps, 0, stapa, 5 + sps.length, pps.length);
		}
	}

	public void run() {
		long duration = 0;
		Log.d(TAG, "H264 packetizer started !");
		stats.reset();
		count = 0;

		socket.setCacheSize(1);

		try {
			while (!Thread.interrupted()) {

				// long beforeTime = System.currentTimeMillis();
				oldtime = System.nanoTime();
				// We read a NAL units from the input stream and we send them
				send();

				// We measure how long it took to receive NAL units from the
				// phone
				duration = System.nanoTime() - oldtime;

				stats.push(duration);
				// Computes the average duration of a NAL unit
				delay = stats.average();

			}
		} catch (IOException e) {
		} catch (InterruptedException e) {
		}

		Log.d(TAG, "H264 packetizer stopped !");

	}

	/**
	 * Reads a NAL unit in the FIFO and sends it. If it is too big, we split it
	 * in FU-A units (RFC 3984).
	 */
	@SuppressLint("NewApi")
	private void send() throws IOException, InterruptedException {
		int sum = 1, len = 0, type;

		// NAL units are preceeded by their length, we parse the length
		fill(header, 0, 5);
		ts += delay;
		naluLength = header[3] & 0xFF | (header[2] & 0xFF) << 8
				| (header[1] & 0xFF) << 16 | (header[0] & 0xFF) << 24;
		// Log.i(TAG, "naluLength: " + naluLength);
		if (naluLength > 100000 || naluLength < 0)
			resync();

		// Parses the NAL unit type
		type = header[4] & 0x1F;

		// The stream already contains NAL unit type 7 or 8, we don't need
		// to add them to the stream ourselves
		if (type == 7 || type == 8) {
			Log.v(TAG, "SPS or PPS present in the stream.");
			count++;
			if (count > 4) {
				sps = null;
				pps = null;
			}
		}

		// We send two packets containing NALU type 7 (sps) and 8 (pps)
		// Those should allow the H264 stream to be decoded even if no SDP was
		// sent to the decoder.
		if (type == 5 && sps != null && pps != null) {
			buffer = socket.requestBuffer();
			socket.markNextPacket();
			socket.updateTimestamp(ts);
			System.arraycopy(stapa, 0, buffer, rtphl, stapa.length);
			super.send(rtphl + stapa.length);
		}

		// Log.d(TAG, "- Nal unit length: " + naluLength + " delay: " + delay
		// / 1000000 + " type: " + type);

		// Small NAL unit => Single NAL unit
		if (naluLength <= MAXPACKETSIZE - rtphl - 2) {
			buffer = socket.requestBuffer();
			buffer[rtphl] = header[4];
			len = fill(buffer, rtphl + 1, naluLength - 1);
			socket.updateTimestamp(ts);
			socket.markNextPacket();
			super.send(naluLength + rtphl);
			Log.d(TAG, "----- Single NAL unit - len:" + len + " delay: "
					+ delay);
		}
		// Large NAL unit => Split nal unit
		else {

			// Set FU-A header
			header[1] = (byte) (header[4] & 0x1F); // FU header type
			header[1] += 0x80; // Start bit
			// Set FU-A indicator
			header[0] = (byte) ((header[4] & 0x60) & 0xFF); // FU indicator NRI
			header[0] += 28;

			while (sum < naluLength) {
				buffer = socket.requestBuffer();
				buffer[rtphl] = header[0];
				buffer[rtphl + 1] = header[1];
				socket.updateTimestamp(ts);
				if ((len = fill(
						buffer,
						rtphl + 2,
						naluLength - sum > MAXPACKETSIZE - rtphl - 2 ? MAXPACKETSIZE
								- rtphl - 2
								: naluLength - sum)) < 0)
					return;
				sum += len;
				// Last packet before next NAL
				if (sum >= naluLength) {
					// End bit on
					buffer[rtphl + 1] += 0x40;
					socket.markNextPacket();
				}
				super.send(len + rtphl + 2);
				// Switch start bit
				header[1] = (byte) (header[1] & 0x7F);
				// Log.d(TAG,"----- FU-A unit, sum:"+sum);
			}
		}
	}

	private int fill(byte[] buffer, int offset, int length) throws IOException {
		int sum = 0, len;
		while (sum < length) {
			len = is.read(buffer, offset + sum, length - sum);
			if (len < 0) {
				throw new IOException("End of stream");
			} else
				sum += len;
		}
		return sum;
	}

	private void resync() throws IOException {
		int type;

		Log.e(TAG,
				"Packetizer out of sync ! Let's try to fix that...(NAL length: "
						+ naluLength + ")");

		while (true) {

			header[0] = header[1];
			header[1] = header[2];
			header[2] = header[3];
			header[3] = header[4];
			header[4] = (byte) is.read();

			type = header[4] & 0x1F;

			if (type == 5 || type == 1) {
				naluLength = header[3] & 0xFF | (header[2] & 0xFF) << 8
						| (header[1] & 0xFF) << 16 | (header[0] & 0xFF) << 24;
				if (naluLength > 0 && naluLength < 100000) {
					oldtime = System.nanoTime();
					Log.e(TAG,
							"A NAL unit may have been found in the bit stream !");
					break;
				}
				if (naluLength == 0) {
					Log.e(TAG, "NAL unit with NULL size found...");
				} else if (header[3] == 0xFF && header[2] == 0xFF
						&& header[1] == 0xFF && header[0] == 0xFF) {
					Log.e(TAG, "NAL unit with 0xFFFFFFFF size found...");
				}
			}

		}

	}

}