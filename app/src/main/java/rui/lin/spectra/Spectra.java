/**
 * Spectra: a fully functional player
 */
package rui.lin.spectra;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.os.SystemClock.elapsedRealtime;

/**
 * @author Rui Lin
 * 
 */
public class Spectra {

	// --------------------------- inner classes --------------------------- //

	protected class BufferEngine extends Thread {
		public boolean shouldStop = false;
		public boolean shouldWait = false;
		public boolean isWaiting = false;
		private long pre_reading_success_time = 0;

		public BufferEngine() {
			super();
			setPriority(NORM_PRIORITY);
		}

		@Override
		public void run() {
			int ret;
			BUFFER_ENGINE_ROAR: while (!shouldStop) {
				if (shouldWait) {
					isWaiting = true;
					sLogger.info("BufferEngine is waiting");
					while (shouldWait) {
						yield();
					}
					isWaiting = false;
					sLogger.info("BufferEngine continues execution");
				}

				switch (mState) {
				case LOADED:
				case PLAYING:
				case PAUSED:
					ret = readPacket();
					if (ret == 0) {
						pre_reading_success_time = elapsedRealtime();
						if (mLowCPUCostRetry) {
							mLowCPUCostRetry = false;
							sLogger.info("exit low-CPU-cost-retry state");
						}
						yield();
					} else if (ret == 2001) {
						sLogger.debug("buffer full, sleep(%d)", BUFFER_ENGINE_BUFFER_FULL_SLEEP_TIME);
						try {
							sleep(BUFFER_ENGINE_BUFFER_FULL_SLEEP_TIME);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else if (ret == -541478725 && !isLiveStream()) { // AVERROR_EOF: - 0x20 0x46 0x4F 0x45
						// branch for true EOF. readPacket is able to return AVERROR_EOF correctly now, but just for HLS.
						if (mStateLock.tryLock()) {
							if (mState == Spectra.State.PLAYING) {
								mState = Spectra.State.STOPPING;
								mStateLock.unlock();
								sLogger.info("true EOF reached in PLAYING state, switch state to STOPPING");
								mEventDispatcher.sendEmptyMessage(SpectraEvent.STOPPING.ordinal());
							} else {
								mStateLock.unlock();
								sLogger.warning("true EOF reached in " + mState + "state, ignore EOF and sleep(%d)",
										BUFFER_ENGINE_PAUSED_STATE_EOF_SLEEP_TIME);
								try {
									sleep(BUFFER_ENGINE_PAUSED_STATE_EOF_SLEEP_TIME);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						} else {
							sLogger.warning("true EOF is reached but tryLock failed (state is switching). Ignore EOF and yield");
							yield();
						}
					} else {
						if (!mLowCPUCostRetry
								&& elapsedRealtime() - pre_reading_success_time > LOW_CPU_COST_RETRY_TRIGGER) {
							mLowCPUCostRetry = true;
							sLogger.info("enter low-CPU-cost-retry state");
						}
						sLogger.warning("readPacket error #%d, yield", ret);
						mEventDispatcher.sendEmptyMessage(SpectraEvent.READ_PACKET_ERROR.ordinal());
						if (mLowCPUCostRetry) {
							try {
								sleep(LOW_CPU_COST_RETRY_INTERVAL);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else {
							yield();
						}
					}
					break;
				case STOPPING:
					sLogger.debug("BufferEngine running in STOPPING state, sleep(%d)",
							BUFFER_ENGINE_STOPPING_STATE_SLEEP_TIME);
					try {
						sleep(BUFFER_ENGINE_STOPPING_STATE_SLEEP_TIME);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				default:
					sLogger.warning("BufferEngine running in wrong state: " + mState + ", stop BufferEngine");
					break BUFFER_ENGINE_ROAR;
				}
			}
			sLogger.info("BufferEngine stopped");
		}
	}

	protected class PlaybackEngine extends Thread {
		public boolean shouldStop = false;
		public boolean shouldWait = false;
		public boolean isWaiting = false;

		public PlaybackEngine() {
			super();
			setPriority(MAX_PRIORITY);
		}

		@Override
		public void run() {
			try
			{

				int ret;
				PLAYBACK_ENGINE_ROAR: while (!shouldStop) {
					if (shouldWait) {
						isWaiting = true;
						sLogger.info("PlaybackEngine is waiting");
						while (shouldWait) {
							yield();
						}
						sLogger.info("PlaybackEngine continues execution");
						isWaiting = false;
					}

					switch (mState) {
					case PLAYING:
					case STOPPING:
						ret = decodeFrame();
						if (ret == 0) { // This is an audio frame.
							mAudioTrack.write(mWaveform, 0, mWaveformLength);
							sLogger.debug("feed %d bytes to mAudioTrack", mWaveformLength);
							mEventDispatcher.sendEmptyMessage(SpectraEvent.PLAYING.ordinal());
						} else if (ret == 1) { // This is an ID3v2 tag.
							sLogger.info("%d bytes ID3v2 tag stored in mMetadata", mMetadataLength);
							mEventDispatcher.sendEmptyMessage(SpectraEvent.ID3V2.ordinal());
						} else if (ret == 2000 && mState == Spectra.State.STOPPING) {
							// branch for true EOF
							if (mStateLock.tryLock()) {
								try {
									if (mState == Spectra.State.STOPPING) {
										mEventDispatcher.sendEmptyMessage(SpectraEvent.END_OF_STREAM.ordinal());
										sLogger.info("packet buffer empty in STOPPING state, switch state to STOPPED and stop PlaybackEngine");
										mAudioTrack.stop();
										mBufferEngine.shouldStop = true;
										try {
											mBufferEngine.join();
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										mBufferEngine = null;
										mPlaybackEngine = null;
										closeStream();
										mState = Spectra.State.STOPPED;
										sLogger.info("state reset to STOPPED");
										mEventDispatcher.sendEmptyMessage(SpectraEvent.STOPPED.ordinal());
										break PLAYBACK_ENGINE_ROAR;
									} else {
										sLogger.warning("tryLock succeeded but state is no longer STOPPING. Ignore");
									}
								} finally {
									mStateLock.unlock();
								}
							} else {
								sLogger.warning("packet buffer empty happened in STOPPING state, but tryLock failed (state is switching). Yield");
								yield();
							}
						} else if (ret == 2000) {
	                        if(shouldOpt){
	                            sLogger.debug("packet buffer empty, yield execution to another thread");
	                            mEventDispatcher.sendEmptyMessage(SpectraEvent.BUFFER_EMPTY.ordinal());
	                            try {
	                                sLogger.error("packet buffer empty,sleep %d", PLAY_LOW_CPU_COST_RETRY_INTERVAL);
	                                sleep(PLAY_LOW_CPU_COST_RETRY_INTERVAL);
	                            } catch (InterruptedException e) {
	                                e.printStackTrace();
	                            }
	                        }else{
	                            sLogger.debug("packet buffer empty, yield execution to another thread");
	                            mEventDispatcher.sendEmptyMessage(SpectraEvent.BUFFER_EMPTY.ordinal());
	                            if (mLowCPUCostRetry) {
	                                try {
	                                    sleep(LOW_CPU_COST_RETRY_INTERVAL);
	                                } catch (InterruptedException e) {
	                                    // TODO Auto-generated catch block
	                                    e.printStackTrace();
	                                }
	                            } else {
	                                yield();
	                            }
	                        }
	                    } else {
	                        sLogger.warning("decode frame error #" + ret);
	                        mEventDispatcher.sendEmptyMessage(SpectraEvent.DECODE_FRAME_ERROR.ordinal());
	                    }
	                    break;
	                case PAUSED:
	                    sLogger.debug("PlaybackEngine running in PAUSED state, sleep(%d)",
	                            PLAYBACK_ENGINE_PAUSED_STATE_SLEEP_TIME);
	                    try {
	                        sleep(PLAYBACK_ENGINE_PAUSED_STATE_SLEEP_TIME);
	                    } catch (InterruptedException e) {
	                        // TODO Auto-generated catch block
	                        e.printStackTrace();
	                    }
	                    break;
	                default:
	                    sLogger.warning("PlaybackEngine running in wrong state: " + mState + ", stop PlaybackEngine");
	                    break PLAYBACK_ENGINE_ROAR;
	                }
	            }
	            sLogger.info("PlaybackEngine stopped");	        
			}
			catch(Exception e)
			{
				
			}
		}
    }

    protected class StateResetTask extends TimerTask {
        boolean shouldCancel = false;

        @Override
        public void run() {
            mStateLock.lock();
            try {
                if (!shouldCancel && mState == State.LOADED) {
                    mBufferEngine.shouldStop = true;
                    try {
                        mBufferEngine.join();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mBufferEngine = null;
                    closeStream();
                    mStateResetTask = new StateResetTask();
                    mState = State.STOPPED;
                    sLogger.info("time elapsed, state reset to STOPPED");
                    mEventDispatcher.sendEmptyMessage(SpectraEvent.STOPPED.ordinal());
                }
            } finally {
                mStateLock.unlock();
            }
        }
    }

    class SpectraEventDispatcher extends Handler {
        private SpectraEvent pre_event = null;

        public SpectraEventDispatcher(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            SpectraEvent event = SpectraEvent.values()[msg.what];
            if (event != pre_event) { // suppress duplicate event
                sLogger.debug(Spectra.this + " is dispatching event " + event);
                for (SpectraEventListener event_listener : mEventListeners) {
                    try {
                        event_listener.onSpectraEvent(Spectra.this, event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                pre_event = event;
            }
        }
    };

    /*
     * event listener interface
     */
    public interface SpectraEventListener {
        public void onSpectraEvent(Spectra spectra, SpectraEvent event);
    }

    /*
     * events
     * events should be a superset of states with all the state-like-events possess the same ordinal as their corresponding states
     */
    public enum SpectraEvent {
        UNINITIALIZED, STOPPED, LOADED, PLAYING, PAUSED, STOPPING,
        DO_LOAD, DO_PLAY, DO_STOP, DO_PAUSE, DO_RESUME, DO_SEEK,
        SEEK_SUCCEEDED, SEEK_FAILED, BUFFER_EMPTY, OPEN_STREAM_FAILED, READ_PACKET_ERROR, DECODE_FRAME_ERROR, END_OF_STREAM,
        ID3V2
    }

    /*
     * internal states
     * STOPPING is a special derivative state from PLAYING when true EOF is reached.
     */
    public enum State {
        UNINITIALIZED, STOPPED, LOADED, PLAYING, PAUSED, STOPPING
    }

    // --------------------------- static fields --------------------------- //

    protected static final int LIVE_STREAM_LOADED_STATE_TIME = 15000; // ms
    protected static final int LIVE_STREAM_CRITCAL_DURATION = 5; // s
    protected static final long OPEN_STREAMS_TIMEOUT_FOR_LOAD = 3500; // ms
    protected static final long OPEN_STREAMS_TIMEOUT_FOR_PLAY = 45000; // ms
    protected static final long READ_PACKET_TIMEOUT = 10000000; // us
    protected static final int BUFFER_ENGINE_BUFFER_FULL_SLEEP_TIME = 300; // ms
    protected static final int BUFFER_ENGINE_STOPPING_STATE_SLEEP_TIME = 100; // ms
    protected static final int BUFFER_ENGINE_PAUSED_STATE_EOF_SLEEP_TIME = 100; // ms
    protected static final int PLAYBACK_ENGINE_PAUSED_STATE_SLEEP_TIME = 100; // ms
    protected static final long LOW_CPU_COST_RETRY_TRIGGER = 20000; // ms
    protected static final long LOW_CPU_COST_RETRY_INTERVAL = 1500; // ms
    protected static final long PLAY_LOW_CPU_COST_RETRY_INTERVAL=500;
    protected static final int PACKET_BUFFER_LENGTH = 500; // packet
    protected static final int AUDIO_TRACK_BUFFER_SIZE_COEFFICIENT = 2;
    protected static final int EVENT_LISTENERS_CAPACITY = 5;

    protected static int sLogLevel = Log.INFO;
    protected static String sLogTag = "Spectra";
    protected static Logger sLogger;

    // --------------------------- static initialization block --------------------------- //

    static {
        System.loadLibrary("opencore");
        System.loadLibrary("rtmp");

        System.loadLibrary("v6vfp");
        System.loadLibrary("spectra6vfp");

        // initialize native static data
        clinit();

        // setup logger
        sLogger = new Logger(sLogLevel, sLogTag);
    }

    // --------------------------- dynamic member fields --------------------------- //

    private State mState = State.UNINITIALIZED;
    private Lock mStateLock = new ReentrantLock();
    private boolean mLowCPUCostRetry = false;

    protected byte[] mSpectraCtx;
    protected byte[] mMetadata;
    protected int mMetadataLength;
    protected byte[] mWaveform;
    protected int mWaveformLength;
    protected BufferEngine mBufferEngine;
    protected PlaybackEngine mPlaybackEngine; // the main playback PlaybackEngine
    protected AudioTrack mAudioTrack;
    protected Timer mStateResetTimer; // a daemon thread
    protected StateResetTask mStateResetTask;
    protected List<SpectraEventListener> mEventListeners;
    protected HandlerThread mEventDispatcherThread;
    protected SpectraEventDispatcher mEventDispatcher;

    protected ArrayList<String> mUrls;
    protected String mSelectedUrl;

    protected String mContainerFmt;
    protected String mCompressionFmt;
    protected String mSampleFmt;
    protected int mSampleRate;
    protected int mBitRate;
    protected int mChannels;
    protected int mDuration; // second
    protected int mPosition; // second

    // --------------------------- native functions --------------------------- //

    private static native int clinit();

    private native int init(int packet_buffer_length);

    private native int openStream(String url, String authorization, long timeout_us); // us

    private native int readPacket();

    private native int decodeFrame();

    private native int seekStream(int seek_position_s); // second

    private native int closeStream();

    private native int deinit();

    private native int forceInterruption(boolean should_interrupt);

    private native boolean forceIntEnabled();

    // --------------------------- functions --------------------------- //

    /*
     * <init>: construct a new instance if init(length) succeed, set state to STOPPED, else reset to UNINITIALIZED.
     */
    public Spectra() {
        if (init(PACKET_BUFFER_LENGTH) == 0) {
            mStateResetTimer = new Timer(true);
            mStateResetTask = new StateResetTask();
            int buffer_size = AUDIO_TRACK_BUFFER_SIZE_COEFFICIENT
                    * AudioTrack
                            .getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, buffer_size, AudioTrack.MODE_STREAM);
            mEventListeners = Collections
                    .synchronizedList(new ArrayList<SpectraEventListener>(EVENT_LISTENERS_CAPACITY));
            mEventDispatcherThread = new HandlerThread("SpectraEventDispatcherThread");
            mEventDispatcherThread.start();
            mEventDispatcher = new SpectraEventDispatcher(mEventDispatcherThread.getLooper());
            mState = State.STOPPED;
            sLogger.info(
                    "Spectra initialized (with AudioTrack buffer size defaulted to %d bytes), switch state to STOPPED",
                    buffer_size);
            mEventDispatcher.sendEmptyMessage(SpectraEvent.STOPPED.ordinal());
        } else {
            deinit();
            mState = State.UNINITIALIZED;
            sLogger.info("native init() failed, keep state UNINITIALIZED");
        }
    }

    public State queryState() {
        return mState;
    }

    public ArrayList<String> queryUrls() {
        return mUrls;
    }

    public String querySelectedUrl() {
    	return mSelectedUrl != null ? mSelectedUrl : "none";
    }

    public boolean isLiveStream() {
        return mDuration < LIVE_STREAM_CRITCAL_DURATION;
    }

    public String queryContainerFormat() {
        return mContainerFmt != null ? mContainerFmt : "unknown";
    }

    public String queryCompressionFormat() {
        return mCompressionFmt != null ? mCompressionFmt : "unknown";
    }

    public String querySampleFormat() {
        return mSampleFmt != null ? mSampleFmt : "unknown";
    }

    public int querySampleRate() {
        return mSampleRate;
    }

    public int queryBitRate() {
        return mBitRate;
    }

    public int queryChannels() {
        return mChannels;
    }

    public int queryDuration() {
        return mDuration;
    }

    public int queryPosition() {
        return mPosition;
    }

    public byte[] queryMetadata() {
        try {
            if (mMetadata != null && mMetadata.length > 0 && mMetadataLength > 0) {
                byte[] metadata = new byte[mMetadataLength];
                System.arraycopy(mMetadata, 0, metadata, 0, mMetadataLength);
                return metadata;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addEventListener(SpectraEventListener event_listener) {
        if (mEventListeners != null) {
            mEventListeners.add(event_listener);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeEventListener(SpectraEventListener event_listener) {
        if (mEventListeners != null) {
            return mEventListeners.remove(event_listener);
        } else {
            return true;
        }
    }

    /*
     * openStreams calls openStream on each url before timeout.
     * if openStream succeed, true is returned immediately
     * else it will call closeStream and try to open another url.
     */
    protected boolean openStreams(List<String> urls, long timeout_ms) { // ms
        if (urls == null || urls.size() == 0) {
            return false;
        }

        int ret;
        Long begin_time = elapsedRealtime();

        StringBuffer sb = new StringBuffer();
        String digestTemplate = "&digest=";
        for (String url : urls) {
            int ps = url.lastIndexOf(digestTemplate); //locate the digest parameter from pass in url
            String digest = "";
            if (ps > 0) {
                digest = url.substring(ps + digestTemplate.length(), url.length());
                sLogger.info("#######openStream digest: (%s) ", digest);
                //Only set the digest ENV when find it in the URL
                url = url.substring(0, ps); // the underline ffmpeg doesn't need the digest in the url
            }

            sb.append(url).append("|");
            ret = openStream(url, digest, timeout_ms * 1000);

            if (ret == 0) {
                sLogger.info("openStream(%s) succeeded", url);
                mSelectedUrl = url;
                return true;
            } else {
                closeStream();
                mEventDispatcher.sendEmptyMessage(SpectraEvent.OPEN_STREAM_FAILED.ordinal());
                SpectraBean bean = new SpectraBean();
                bean.openUrl = url;
                bean.time = System.currentTimeMillis() / 1000;
                bean.jniReturnCode = ret;

            }
            if (elapsedRealtime() > begin_time + timeout_ms) {
                break;
            }
        }
        return false;
    }

    protected void customizeAudioTrack() {
        int sampleRate;
        int channelConfig;
        int audioFormat;

        if (mSampleRate < 4000) {
            sampleRate = 4000;
            sLogger.warning("sample rate unsuppported: " + sampleRate);
        } else if (mSampleRate > 48000) {
            sampleRate = 48000;
            sLogger.warning("sample rate unsuppported: " + sampleRate);
        } else {
            sampleRate = mSampleRate;
        }

        switch (mChannels) {
        case 1:
            channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            break;
        case 2:
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
            break;
        default:
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
            sLogger.warning("channel config unsupported: " + mChannels);
        }

        if (mSampleFmt == null) {
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            sLogger.warning("sample format null");
        } else if (mSampleFmt.equals("s16")) {
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        } else if (mSampleFmt.equals("u8")) {
            audioFormat = AudioFormat.ENCODING_PCM_8BIT;
        } else {
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            sLogger.warning("sample format unsupported: " + mSampleFmt);
        }

        if (sampleRate != mAudioTrack.getSampleRate() || channelConfig != mAudioTrack.getChannelConfiguration()
                || audioFormat != mAudioTrack.getAudioFormat()) {
            mAudioTrack.release();
            int buffer_size = AUDIO_TRACK_BUFFER_SIZE_COEFFICIENT
                    * AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat,
                    buffer_size, AudioTrack.MODE_STREAM);
            sLogger.info("customize AudioTrack with SR=" + mSampleRate + " CH=" + mChannels + " FMT=" + mSampleFmt
                    + " BUFF=" + buffer_size);
        } else {
            sLogger.info("no need to customize AudioTrack");
        }
    }

    /*
     * functional states: STOPPED LOADED
     * load the urls: 1. mUrls are guaranteed to be set. 2. closeStream and
     * openStreams/openStream may be called.
     */
    public boolean load(List<String> urls) {
        mStateLock.lock();
        try {
            sLogger.info("#------> load(%s)", urls);
            mEventDispatcher.sendEmptyMessage(SpectraEvent.DO_LOAD.ordinal());
            boolean retv = false;
            switch (mState) {
            case LOADED:
                if (isLiveStream()) {
                    mStateResetTask.shouldCancel = true;
                    mStateResetTask.cancel();
                    mStateResetTimer.purge();
                    mStateResetTask = new StateResetTask();
                    sLogger.info("previous StateResetTask cancelled");
                }
                mBufferEngine.shouldStop = true;
                try {
                    mBufferEngine.join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mBufferEngine = null;
                closeStream();
                // no break here. Execution should continue to the next section.
            case STOPPED:
                mUrls = new ArrayList<String>(urls);
                mSelectedUrl = null;
                if (openStreams(mUrls, OPEN_STREAMS_TIMEOUT_FOR_LOAD)) {
                    customizeAudioTrack();
                    mBufferEngine = new BufferEngine();
                    if (isLiveStream()) {
                        mStateResetTimer.schedule(mStateResetTask, LIVE_STREAM_LOADED_STATE_TIME);
                        sLogger.info("live stream opened, switch state to LOADED and start the state reset timer");
                    } else {
                        sLogger.info("on-demand stream opened, switch state to LOADED");
                    }
                    mState = State.LOADED;
                    mBufferEngine.start();
                    mEventDispatcher.sendEmptyMessage(SpectraEvent.LOADED.ordinal());
                    retv = true;
                } else {
                    mState = State.STOPPED;
                    sLogger.warning("openStreams failed, reset state to STOPPED");
                    mEventDispatcher.sendEmptyMessage(SpectraEvent.STOPPED.ordinal());
                }
                break;
            default:
                sLogger.warning("load(%s) called in wrong state, ignore", urls);
                mEventDispatcher.sendEmptyMessage(mState.ordinal());
            }
            return retv;
        } finally {
            mStateLock.unlock();
        }
    }

    /*
     * functional states: LOADED STOPPED
     * turn on PlaybackEngine and start playback
     */
    public boolean play() {
        mStateLock.lock();
        try {
            sLogger.info("#------> play()");
            mEventDispatcher.sendEmptyMessage(SpectraEvent.DO_PLAY.ordinal());
            boolean retv = false;
            switch (mState) {
            case LOADED:
                if (isLiveStream()) {
                    mStateResetTask.shouldCancel = true;
                    mStateResetTask.cancel();
                    mStateResetTimer.purge();
                    mStateResetTask = new StateResetTask();
                    sLogger.info("previous StateResetTask cancelled");
                }
                mPlaybackEngine = new PlaybackEngine();
                mState = State.PLAYING;
                mPlaybackEngine.start();
                mAudioTrack.play();
                sLogger.info("playback started, switch state to PLAYING");
                mEventDispatcher.sendEmptyMessage(SpectraEvent.PLAYING.ordinal());
                retv = true;
                break;
            case STOPPED:
                if (mUrls != null && mUrls.size() > 0) {
                    if (openStreams(mUrls, OPEN_STREAMS_TIMEOUT_FOR_PLAY)) {
                        customizeAudioTrack();
                        mBufferEngine = new BufferEngine();
                        mPlaybackEngine = new PlaybackEngine();
                        mState = State.PLAYING;
                        mBufferEngine.start();
                        mPlaybackEngine.start();
                        mAudioTrack.play();
                        sLogger.info("playback started, switch state to PLAYING");
                        mEventDispatcher.sendEmptyMessage(SpectraEvent.PLAYING.ordinal());
                        retv = true;
                    } else {
                        mState = State.STOPPED;
                        sLogger.warning("openStreams failed, reset state to STOPPED");
                        mEventDispatcher.sendEmptyMessage(SpectraEvent.STOPPED.ordinal());
                    }
                } else {
                    mState = State.STOPPED;
                    sLogger.warning("mUrls not set, reset state to STOPPED");
                    mEventDispatcher.sendEmptyMessage(SpectraEvent.STOPPED.ordinal());
                }
                break;
            default:
                sLogger.warning("play() called in wrong state, ignore");
                mEventDispatcher.sendEmptyMessage(mState.ordinal());
            }
            return retv;
        } finally {
            mStateLock.unlock();
        }
    }

    /*
     * functional states: LOADED PLAYING PAUSED STOPPING
     * reset state to STOPPED
     */
    public boolean stop() {
        mStateLock.lock();
        try {
            sLogger.info("#------> stop()");
            mEventDispatcher.sendEmptyMessage(SpectraEvent.DO_STOP.ordinal());
            boolean retv = false;
            switch (mState) {
            case PAUSED:
            case PLAYING:
            case STOPPING:
                mAudioTrack.stop();
                mBufferEngine.shouldStop = true;
                mPlaybackEngine.shouldStop = true;
                try {
                    mBufferEngine.join();
                    mPlaybackEngine.join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mBufferEngine = null;
                mPlaybackEngine = null;
                closeStream();
                mState = State.STOPPED;
                sLogger.info("reset state to STOPPED");
                mEventDispatcher.sendEmptyMessage(SpectraEvent.STOPPED.ordinal());
                retv = true;
                break;
            case LOADED:
                if (isLiveStream()) {
                    mStateResetTask.shouldCancel = true;
                    mStateResetTask.cancel();
                    mStateResetTimer.purge();
                    mStateResetTask = new StateResetTask();
                    sLogger.info("previous StateResetTask cancelled");
                }
                mBufferEngine.shouldStop = true;
                try {
                    mBufferEngine.join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mBufferEngine = null;
                closeStream();
                mState = State.STOPPED;
                sLogger.info("reset state to STOPPED");
                mEventDispatcher.sendEmptyMessage(SpectraEvent.STOPPED.ordinal());
                retv = true;
                break;
            default:
                sLogger.warning("stop() called in wrong state, ignore");
                mEventDispatcher.sendEmptyMessage(mState.ordinal());
            }
            return retv;
        } finally {
            mStateLock.unlock();
        }
    }

    /*
     * functional states: PLAYING STOPPING
     * pause an On-Demond audio stream
     */
    public boolean pause() {
        mStateLock.lock();
        try {
            sLogger.info("#------> pause()");
            mEventDispatcher.sendEmptyMessage(SpectraEvent.DO_PAUSE.ordinal());
            boolean retv = false;
            switch (mState) {
            case PLAYING:
            case STOPPING:
                if (!isLiveStream()) {
                    mState = State.PAUSED;
                    mAudioTrack.pause();
                    mPlaybackEngine.shouldWait = true;
                    while (!mPlaybackEngine.isWaiting) {
                        Thread.yield();
                    }
                    mPlaybackEngine.shouldWait = false;
                    sLogger.info("switch state to PAUSED");
                    mEventDispatcher.sendEmptyMessage(SpectraEvent.PAUSED.ordinal());
                    retv = true;
                } else {
                    sLogger.warning("live audio, cannot pause");
                }
                break;
            default:
                sLogger.warning("pause() called in wrong state, ignore");
                mEventDispatcher.sendEmptyMessage(mState.ordinal());
            }
            return retv;
        } finally {
            mStateLock.unlock();
        }
    }

    /*
     * functional states: PAUSED
     * resume a paused On-Demond audio stream
     */
    public boolean resume() {
        mStateLock.lock();
        try {
            sLogger.info("#------> resume()");
            mEventDispatcher.sendEmptyMessage(SpectraEvent.DO_RESUME.ordinal());
            boolean retv = false;
            switch (mState) {
            case PAUSED:
                mState = State.PLAYING;
                mAudioTrack.play();
                sLogger.info("playback resumed, switch state to PLAYING");
                mEventDispatcher.sendEmptyMessage(SpectraEvent.PLAYING.ordinal());
                retv = true;
                break;
            default:
                sLogger.warning("resume() called in wrong state, ignore");
                mEventDispatcher.sendEmptyMessage(mState.ordinal());
            }
            return retv;
        } finally {
            mStateLock.unlock();
        }
    }

    /*
     * functional states: PAUSED LOADED
     * seek to a position in On-Demond audio stream
     */
    public boolean seek(int seek_position_s) { // second
        mStateLock.lock();
        try {
            sLogger.info("#------> seek(" + seek_position_s + ")");
            mEventDispatcher.sendEmptyMessage(SpectraEvent.DO_SEEK.ordinal());
            boolean retv = false;
            switch (mState) {
            case LOADED:
                if (!isLiveStream()) {
                    mBufferEngine.shouldWait = true;
                    while (!mBufferEngine.isWaiting) {
                        Thread.yield();
                    }
                    if (seekStream(seek_position_s) == 0) {
                        sLogger.info("seeking to " + seek_position_s + "s succeeded");
                        mEventDispatcher.sendEmptyMessage(SpectraEvent.SEEK_SUCCEEDED.ordinal());
                        retv = true;
                    } else {
                        sLogger.warning("seeking to " + seek_position_s + "s failed");
                        mEventDispatcher.sendEmptyMessage(SpectraEvent.SEEK_FAILED.ordinal());
                    }
                    mBufferEngine.shouldWait = false;
                } else {
                    sLogger.warning("seek() called for live stream, ignore");
                }
                break;
            case PLAYING:
            case STOPPING:
                if (!isLiveStream()) {
                    mAudioTrack.pause();
                    mBufferEngine.shouldWait = true;
                    mPlaybackEngine.shouldWait = true;
                    mAudioTrack.flush();
                    while (!(mBufferEngine.isWaiting && mPlaybackEngine.isWaiting)) {
                        Thread.yield();
                    }
                    if (seekStream(seek_position_s) == 0) {
                        retv = true;
                        sLogger.info("seeking to " + seek_position_s + "s succeeded");
                        mEventDispatcher.sendEmptyMessage(SpectraEvent.SEEK_SUCCEEDED.ordinal());
                    } else {
                        sLogger.warning("seeking to " + seek_position_s + "s failed");
                        mEventDispatcher.sendEmptyMessage(SpectraEvent.SEEK_FAILED.ordinal());
                    }
                    mPlaybackEngine.shouldWait = false;
                    mBufferEngine.shouldWait = false;
                    mAudioTrack.play();
                    mState = State.PLAYING;
                    sLogger.info("switch state to PLAYING");
                    mEventDispatcher.sendEmptyMessage(SpectraEvent.PLAYING.ordinal());
                } else {
                    sLogger.warning("seek() called for live stream, ignore");
                }
                break;
            case PAUSED:
                mBufferEngine.shouldWait = true;
                mPlaybackEngine.shouldWait = true;
                mAudioTrack.flush();
                while (!(mBufferEngine.isWaiting && mPlaybackEngine.isWaiting)) {
                    Thread.yield();
                }
                if (seekStream(seek_position_s) == 0) {
                    retv = true;
                    sLogger.info("seeking to " + seek_position_s + "s succeeded");
                    mEventDispatcher.sendEmptyMessage(SpectraEvent.SEEK_SUCCEEDED.ordinal());
                } else {
                    sLogger.warning("seeking to " + seek_position_s + "s failed");
                    mEventDispatcher.sendEmptyMessage(SpectraEvent.SEEK_FAILED.ordinal());
                }
                mPlaybackEngine.shouldWait = false;
                mBufferEngine.shouldWait = false;
                break;
            default:
                sLogger.warning("seek(" + seek_position_s + ") called in wrong state, ignore");
                mEventDispatcher.sendEmptyMessage(mState.ordinal());
            }
            return retv;
        } finally {
            mStateLock.unlock();
        }
    }

    /*
     * set/reset force interruption flag
     */
    public void interrupt(boolean should_interrupt) {
        forceInterruption(should_interrupt);
    }

    /*
     * return force interruption flag
     */
    public boolean interrupt() {
        return forceIntEnabled();
    }

    @Override
    protected void finalize() throws Throwable {
        // TODO Auto-generated method stub
        stop();
        deinit();
        mAudioTrack.release();
        super.finalize();
    }

    private boolean shouldOpt=true;
    public boolean isShouldOpt() {
        return shouldOpt;
    }

    public void setShouldOpt(boolean shouldOpt) {
        this.shouldOpt = shouldOpt;
    }
}
