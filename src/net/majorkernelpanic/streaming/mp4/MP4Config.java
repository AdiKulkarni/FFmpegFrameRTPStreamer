package net.majorkernelpanic.streaming.mp4;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.util.Base64;
import android.util.Log;

/**
 * Finds SPS & PPS parameters in mp4 file.
 */
public class MP4Config {

	public final static String TAG = "MP4Config";
	
	private MP4Parser mp4Parser;
	private String mProfilLevel, mPPS, mSPS;

	public MP4Config(String profil, String sps, String pps) {
		mProfilLevel = profil; 
		mPPS = pps; 
		mSPS = sps;
	}

	public MP4Config(String sps, String pps) {
		mPPS = pps;
		mSPS = sps;
		mProfilLevel = MP4Parser.toHexString(Base64.decode(sps, Base64.NO_WRAP),1,3);
	}	
	
	public MP4Config(byte[] sps, byte[] pps) {
		mPPS = Base64.encodeToString(pps, 0, pps.length, Base64.NO_WRAP);
		mSPS = Base64.encodeToString(sps, 0, sps.length, Base64.NO_WRAP);
		mProfilLevel = MP4Parser.toHexString(sps,1,3);
	}
	
	/**
	 * Finds sps & pps parameters inside a .mp4.
	 * @param path Path to the file to analyze
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public MP4Config (String path) throws IOException, FileNotFoundException {

		StsdBox stsdBox; 
		
		// We open the mp4 file and parse it
		try {
			mp4Parser = MP4Parser.parse(path);
		} catch (IOException ignore) {
			// Maybe enough of the file has been parsed and we can get the stsd box
		}

		// We find the stsdBox
		stsdBox = mp4Parser.getStsdBox();
		mPPS = stsdBox.getB64PPS();
		mSPS = stsdBox.getB64SPS();
		mProfilLevel = stsdBox.getProfileLevel();

		mp4Parser.close();
		
	}

	public String getProfileLevel() {
		return mProfilLevel;
	}

	public String getB64PPS() {
		Log.d(TAG, "PPS: "+mPPS);
		return mPPS;
	}

	public String getB64SPS() {
		Log.d(TAG, "SPS: "+mSPS);
		return mSPS;
	}

}