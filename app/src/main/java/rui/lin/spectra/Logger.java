/**
 * @author Rui Lin
 * 
 */
package rui.lin.spectra;

import android.util.Log;

public class Logger {
	private int mLogLevel = Log.INFO;
	private String mLogTag = "Logger";

	public Logger(int log_level, String log_tag) {
		mLogLevel = log_level;
		mLogTag = log_tag;
	}

	public void debug(String format, Object... args) {
		if (mLogLevel <= Log.DEBUG) {
			Log.d(mLogTag, String.format(format, args));
		}
	}

	public void info(String format, Object... args) {
//		if (mLogLevel <= Log.iNFO) {
//			//Log.i(mLogTag, String.format(format, args));
//		}
	}

	public void warning(String format, Object... args) {
		if (mLogLevel <= Log.WARN) {
			Log.w(mLogTag, String.format(format, args));
		}
	}

	public void error(String format, Object... args) {
		if (mLogLevel <= Log.ERROR) {
			Log.e(mLogTag, String.format(format, args));
		}
	}
}
