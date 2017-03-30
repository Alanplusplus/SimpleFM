package com.simple.fm;

import android.os.SystemClock;

import com.simple.fm.manager.NetRequestManager;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Alan on 16/7/20.
 */
public class MediaCenter {

    private static final MediaCenter INSTANCE = new MediaCenter();

    private MediaCenter(){}

    private List<PingInfo> mPingInfos;


    public static MediaCenter getInstance(){
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public void fetchList(){
        NetRequestManager.getInstance().get(NetRequestManager.Config.MEDIA_CENTER.getUrl(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()){
                    List<PingInfo> infos = (List<PingInfo>) Parser.parse(Parser.CLASSTYPE.PINGINFO,
                            response.body().string());
                    if (infos!=null && infos.size() > 0){
                        mPingInfos = infos;
                        for (int i=0;i<mPingInfos.size();i++){
//                            Output.show(mPingInfos.get(i).toString());
                            testSpeed(mPingInfos.get(i));
                        }
                    }
                }
            }
        });
    }

    private void testSpeed(final PingInfo info){
        ExecutorService service = Executors.newCachedThreadPool();
        service.execute(new Runnable() {
            @Override
            public void run() {
                long startTime = SystemClock.uptimeMillis();
                boolean success = NetRequestManager.getInstance().testResponse(buildTestPath(info));
                if (success){
                    info.setTime(SystemClock.uptimeMillis() - startTime);
                    Output.show(info.toString());
                    resortBySpeed();
                }
            }
        });
    }

    private void resortBySpeed(){
        if (mPingInfos!=null){
            Collections.sort(mPingInfos, new Comparator<PingInfo>() {
                @Override
                public int compare(PingInfo lhs, PingInfo rhs) {
                    return (int) (lhs.getTime() - rhs.getTime());
                }
            });
        }
    }

    private String buildTestPath(PingInfo info){
        return "http://" + info.getDomain() + info.getTestPath();
    }

    public String getUrl(int resId,int bitrate,String deviceId){
        if (mPingInfos == null || mPingInfos.size() == 0){
            return null;
        }
        boolean addSeperator = false;
        String url = "";
        for (PingInfo info:mPingInfos){
            if (addSeperator){
                url +=";;";
            }
            addSeperator = true;
            url += buildUrl(info,resId,bitrate,deviceId);
        }

        return url;
    }

    private String buildUrl(PingInfo info, int resId, int bitrate,String deviceId){
        return  "http://" + info.getDomain() +
                info.getAccess().replace("${res_id}",String.valueOf(resId))
                .replace("${BITRATE}",String.valueOf(bitrate))
                .replace("${DEVICEID}",deviceId);
    }

}

class PingInfo{
    private String domain;
    private String backupIps;
    private String access;
    private String testPath;
    private int weight;
    private long time;

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getBackupIps() {
        return backupIps;
    }

    public void setBackupIps(String backupIps) {
        this.backupIps = backupIps;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getTestPath() {
        return testPath;
    }

    public void setTestPath(String testPath) {
        this.testPath = testPath;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "PingInfo{" +
                "access='" + access + '\'' +
                ", domain='" + domain + '\'' +
                ", backupIps='" + backupIps + '\'' +
                ", testPath='" + testPath + '\'' +
                ", weight=" + weight +
                ", time=" + time +
                '}';
    }
}
