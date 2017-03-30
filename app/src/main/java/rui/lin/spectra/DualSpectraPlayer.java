/**
 * DualSpectraPlayer: a fully functional player capable of preloading
 */
package rui.lin.spectra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rui.lin.spectra.Spectra.SpectraEvent;
import rui.lin.spectra.Spectra.SpectraEventListener;
import rui.lin.spectra.Spectra.State;
import android.util.Log;

/**
 * @author Rui Lin
 * 
 */
public class DualSpectraPlayer {
    /*
     * inner classes
     */
    class DualSpectraEventFilter implements Spectra.SpectraEventListener {

        @Override
        public void onSpectraEvent(Spectra spectra, SpectraEvent event) {
            // TODO Auto-generated method stub
            if (spectra == mMainPlayer) {
                for (SpectraEventListener event_listener : mEventListeners) {
                    event_listener.onSpectraEvent(null, event);
                }
            }
        }

    }

    /*
     * static fields
     */
    protected static int sLogLevel = Log.DEBUG;
    protected static String sLogTag = "DualSpectraPlayer";
    protected static Logger sLogger = new Logger(sLogLevel, sLogTag);

    /*
     * dynamic members
     */
    protected Spectra mSpectraA;
    protected Spectra mSpectraB;
    protected List<SpectraEventListener> mEventListeners;
    protected DualSpectraEventFilter mEventFilter;

    /*
     * state variables
     * need synchronized access
     */
    protected Spectra mVacantPlayer; // never be null if initialized
    protected Spectra mLoadedPlayer;
    protected Spectra mMainPlayer; // never be the same with mVacantPlayer

    protected boolean mIsInitialized;

    public DualSpectraPlayer() {
        mSpectraA = new Spectra();
        mSpectraB = new Spectra();
        if (mSpectraA.queryState() != Spectra.State.UNINITIALIZED
                && mSpectraB.queryState() != Spectra.State.UNINITIALIZED) {
            mVacantPlayer = mSpectraA;
            mLoadedPlayer = null;
            mMainPlayer = null;
            mEventListeners = Collections.synchronizedList(new ArrayList<SpectraEventListener>());
            mEventFilter = new DualSpectraEventFilter();
            mSpectraA.addEventListener(mEventFilter);
            mSpectraB.addEventListener(mEventFilter);
            mIsInitialized = true;
            sLogger.info("----- player initialized ------");
        } else {
            mSpectraA = null;
            mSpectraB = null;
            mVacantPlayer = null;
            mLoadedPlayer = null;
            mMainPlayer = null;
            sLogger.error("----- player initialization failed ------");
            mIsInitialized = false;
        }
    }

    public State queryState() {
        Spectra.State state;
        if (mMainPlayer != null) {
            state = mMainPlayer.queryState();
        } else {
            state = mVacantPlayer.queryState();
        }
        if (state == Spectra.State.LOADED) {
            state = Spectra.State.STOPPED;
        }
        return state;
    }

    public ArrayList<String> queryUrls() {
        if (mMainPlayer != null) {
            return mMainPlayer.queryUrls();
        } else {
            return mVacantPlayer.queryUrls();
        }
    }

    public String querySelectedUrl() {
        if (mMainPlayer != null) {
            return mMainPlayer.querySelectedUrl();
        } else {
            return mVacantPlayer.querySelectedUrl();
        }
    }

   public ArrayList<String> queryPreloadedUrls() {
        if (mLoadedPlayer != null) {
            return mLoadedPlayer.queryUrls();
        } else {
            return null;
        }
    }

    public boolean isLiveStream() {
        if (mMainPlayer != null) {
            return mMainPlayer.isLiveStream();
        } else {
            return mVacantPlayer.isLiveStream();
        }
    }

    public String queryContainerFormat() {
        if (mMainPlayer != null) {
            return mMainPlayer.queryContainerFormat();
        } else {
            return mVacantPlayer.queryContainerFormat();
        }
    }

    public String queryCompressionFormat() {
        if (mMainPlayer != null) {
            return mMainPlayer.queryCompressionFormat();
        } else {
            return mVacantPlayer.queryCompressionFormat();
        }
    }

    public String querySampleFormat() {
        if (mMainPlayer != null) {
            return mMainPlayer.querySampleFormat();
        } else {
            return mVacantPlayer.querySampleFormat();
        }
    }

    public int querySampleRate() {
        if (mMainPlayer != null) {
            return mMainPlayer.querySampleRate();
        } else {
            return mVacantPlayer.querySampleRate();
        }
    }

    public int queryBitRate() {
        if (mMainPlayer != null) {
            return mMainPlayer.queryBitRate();
        } else {
            return mVacantPlayer.queryBitRate();
        }
    }

    public int queryChannels() {
        if (mMainPlayer != null) {
            return mMainPlayer.queryChannels();
        } else {
            return mVacantPlayer.queryChannels();
        }
    }

    public int queryDuration() { // second
        if (mMainPlayer != null) {
            return mMainPlayer.queryDuration();
        } else {
            return mVacantPlayer.queryDuration();
        }
    }

    public int queryPosition() { // second
        if (mMainPlayer != null) {
            return mMainPlayer.queryPosition();
        } else {
            return mVacantPlayer.queryPosition();
        }
    }

    public byte[] queryMetadata() {
        if (mMainPlayer != null) {
            return mMainPlayer.queryMetadata();
        } else {
            return mVacantPlayer.queryMetadata();
        }
    }

    public boolean addEventListener(SpectraEventListener event_listener) {
        if (mIsInitialized) {
            return mEventListeners.add(event_listener);
        } else {
            return false;
        }
    }

    public boolean removeEventListener(SpectraEventListener event_listener) {
        if (mIsInitialized) {
            return mEventListeners.remove(event_listener);
        } else {
            return false;
        }
    }

    public void switchVacantPlayer() {
        mVacantPlayer = mVacantPlayer == mSpectraA ? mSpectraB : mSpectraA;
    }

    public synchronized boolean load(List<String> urls) {
        sLogger.info("#-> load");
        if (mIsInitialized) {
            mLoadedPlayer = mVacantPlayer;
            return mLoadedPlayer.load(urls);
        } else {
            return false;
        }
    }

    public synchronized boolean play() {
        sLogger.info("#-> play");
        if (mIsInitialized && mLoadedPlayer != null) {
            if (mMainPlayer != null) {
                mMainPlayer.stop();
            }
            mMainPlayer = mLoadedPlayer;
            if (mVacantPlayer == mMainPlayer) {
                switchVacantPlayer();
            }
            return mMainPlayer.play();
        } else {
            return false;
        }
    }

    public synchronized boolean stop() {
        sLogger.info("#-> stop");
        if (mIsInitialized && mMainPlayer != null) {
            return mMainPlayer.stop();
        } else {
            return false;
        }
    }

    public synchronized boolean pause() {
        sLogger.info("#-> pause");
        if (mIsInitialized && mMainPlayer != null) {
            return mMainPlayer.pause();
        } else {
            return false;
        }
    }

    public synchronized boolean resume() {
        sLogger.info("#-> resume");
        if (mIsInitialized && mMainPlayer != null) {
            return mMainPlayer.resume();
        } else {
            return false;
        }
    }

    public synchronized boolean seek(int seek_position_s) {
        sLogger.info("#-> seek");
        if (mIsInitialized && mMainPlayer != null) {
            return mMainPlayer.seek(seek_position_s);
        } else {
            return false;
        }
    }

    public synchronized boolean reconnect() {
        sLogger.info("#-> reconnect");
        if (mIsInitialized && mMainPlayer != null) {
            if (mMainPlayer.isLiveStream()) {
                mMainPlayer.stop();
                return mMainPlayer.play();
            } else {
                int pos = mMainPlayer.queryPosition();
                mMainPlayer.stop();
                mMainPlayer.play();
                return mMainPlayer.seek(pos);
            }
        } else {
            return false;
        }
    }

    public void interrupt(boolean should_interrupt) {
        mSpectraA.interrupt(should_interrupt);
        mSpectraB.interrupt(should_interrupt);
    }

    public boolean interrupt() {
        return mSpectraA.interrupt();
    }
    
    public void enableOpt(){
        mSpectraA.setShouldOpt(true);
        mSpectraB.setShouldOpt(true);
    }
    public void disableOpt(){
        mSpectraA.setShouldOpt(false);
        mSpectraB.setShouldOpt(false);
    }
}
