package com.simple.fm;

import com.simple.fm.manager.NetRequestManager;
import com.simple.fm.model.Channel;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Alan on 16/7/20.
 */
public class ChannelHelper {
    private static final ChannelHelper INSTANCE = new ChannelHelper();

    private HashMap<Integer,Channel> mCache = new HashMap<>();
    private ChannelHelper(){}

    public static ChannelHelper getInstance(){
        return INSTANCE;
    }

    public Channel getChannel(int channelId){
        return mCache.get(channelId);
    }

    public void loadChannel(int channelId){
        NetRequestManager.getInstance().get(
                NetRequestManager.Config.CHANNEL.getUrl(String.valueOf(channelId)), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()){

                }
            }
        });
    }
}
