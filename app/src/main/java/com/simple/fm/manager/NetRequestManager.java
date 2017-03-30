package com.simple.fm.manager;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Alan on 16/5/8.
 */
public class NetRequestManager {

    private static NetRequestManager sInstance;

    private OkHttpClient mClient;

    private NetRequestManager() {

    }

    public static NetRequestManager getInstance() {
        if (sInstance == null) {
            sInstance = new NetRequestManager();
        }
        return sInstance;
    }

    public String get(String url) {
        checkClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try {
            Response response = mClient.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean testResponse(String url){
        checkClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try {
            Response response = mClient.newCall(request).execute();
            if (response.isSuccessful()) {
                response.body().close();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void get(String url, Callback callback){
        checkClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        mClient.newCall(request).enqueue(callback);
    }

    private void checkClient() {
        if (mClient == null) {
            mClient = new OkHttpClient();
        }
    }

    public enum  Config{
        MEDIA_CENTER { String URL= "http://api2.qingting.fm/v6/media/mediacenterlist";

            @Override
            public String getUrl(String... params) {
                return URL;
            }
        },
        SEARCH { String URL = "http://search.qingting.fm/api/newsearch/" +
                "findvt?k=${KEY}&groups=channel_live&type=newcms&curpage=1&pagesize=30&deviceid=${DEVICEID}&city=";

            @Override
            public String getUrl(String... params){
                return URL.replace("${KEY}",params[0]).replace("${DEVICEID}",params[1]);
            }
        },
        CHANNEL{ String URL = "http://api2.qingting.fm/v6/media/channellives/${ID}";

            @Override
            public String getUrl(String... params) {
                return URL.replace("${ID}",params[0]);
            }
        };

        public String getUrl(String... params){
            return null;
        }
    }

}
