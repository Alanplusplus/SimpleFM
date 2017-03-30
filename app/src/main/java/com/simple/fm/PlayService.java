package com.simple.fm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import rui.lin.spectra.DualSpectraPlayer;
import rui.lin.spectra.Spectra;

/**
 * Created by Alan on 16/7/19.
 */
public class PlayService extends Service{

    private DualSpectraPlayer mDualSpectraPlayer = new DualSpectraPlayer();
    private Spectra.SpectraEventListener mSpectraEventListener = new Spectra.SpectraEventListener() {
        @Override
        public void onSpectraEvent(Spectra spectra, Spectra.SpectraEvent event) {

        }
    };

    private final IServiceControl.Stub mBinder = new IServiceControl.Stub() {
        @Override
        public void play() throws RemoteException {
            mDualSpectraPlayer.play();
        }

        @Override
        public void stop() throws RemoteException {
            mDualSpectraPlayer.stop();
        }

        @Override
        public void resume() throws RemoteException {
            mDualSpectraPlayer.resume();
        }

        @Override
        public void pause() throws RemoteException {
            mDualSpectraPlayer.pause();
        }

        @Override
        public void setSource(String source) throws RemoteException {
            setSourceUrls(source);
        }

        @Override
        public String getSource() throws RemoteException {
            return null;
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();

        mDualSpectraPlayer.addEventListener(mSpectraEventListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private boolean setSourceUrls(String urls) {
        if (urls != null) {
            String[] sources = urls.split(";;");
            ArrayList<String> list = new ArrayList<String>();

            for (String src : sources) {
                int ps = src.indexOf("://");
                String p = null;
                if (ps > 0) {
                    p = src.substring(0, ps);
                }

                if (p == null) {
                    continue;
                }
                String s = src.substring(ps + 3);

                if (p.equals("rtspt") || p.equals("rtsp")) {
                    list.add("rtsp://" + s);
                    list.add("rtsp://" + s);
                    list.add("mmsh://" + s);
                    list.add("mmst://" + s);
                } else if (p.equals("mmsh")) {
                    list.add("mmsh://" + s);
                    list.add("mmsh://" + s);
                    list.add("rtsp://" + s);
                    list.add("mmst://" + s);
                } else if (p.equals("mmst")) {
                    list.add("mmst://" + s);
                    list.add("mmst://" + s);
                    list.add("rtsp://" + s);
                    list.add("mmsh://" + s);
                } else if (p.equals("mms")) {
                    list.add("rtsp://" + s);
                    list.add("mmsh://" + s);
                    list.add("mmst://" + s);
                } else if (p.equals("rtmp")) {
                    list.add("rtmp://" + s + " live=1");
                    list.add("rtmp://" + s);
                } else {
                    // set proxy head for "http", "https"
                    // should not set proxy if p=="file"
                    list.add(src);
                }
            }

            return mDualSpectraPlayer.load(list);
        } else {
            return false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
