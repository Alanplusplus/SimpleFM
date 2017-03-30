package com.simple.fm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * Created by Alan on 16/7/19.
 */
public class Player {

    public enum PlayStatus{
        BUFFER,PLAY, ERROR,INIT,STOP,PAUSE;

    }
    private static final Player INSTANCE = new Player();
    private Player(){

    }

    private PlayStatus mPlayStatus = PlayStatus.INIT;

    private IServiceControl mServiceControl;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceControl = IServiceControl.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceControl = null;
        }
    };

    public static Player getInstance(){
        return INSTANCE;
    }

    public void start(Context context){
        Intent bindIntent = new Intent(context,PlayService.class);
        context.bindService(bindIntent,connection,Context.BIND_AUTO_CREATE);

    }

    public void play(){
        if (mServiceControl!=null){
            try {
                setPlayStatus(PlayStatus.PLAY);
                mServiceControl.play();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void pause(){
        if (mServiceControl!=null){
            try {
                mServiceControl.pause();
                setPlayStatus(PlayStatus.PAUSE);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop(){
        if (mServiceControl!=null){
            try {
                mServiceControl.stop();
                setPlayStatus(PlayStatus.STOP);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void resume(){
        if (mServiceControl!=null){
            try {
                mServiceControl.resume();
                setPlayStatus(PlayStatus.PLAY);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void setSource(String source){
        if (mServiceControl!=null){
            try {
                mServiceControl.setSource(source);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void setPlayStatus(PlayStatus status){
        mPlayStatus = status;
    }

    public boolean isPlaying(){
        return mPlayStatus == PlayStatus.PLAY || mPlayStatus == PlayStatus.BUFFER;
    }

    public boolean isPaused(){
        return mPlayStatus == PlayStatus.PAUSE;
    }

}
