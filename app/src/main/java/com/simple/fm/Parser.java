package com.simple.fm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.simple.fm.model.Channel;
import com.simple.fm.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alan on 16/5/8.
 */
public class Parser {

    public enum CLASSTYPE {
        PINGINFO,
        SEARCH,
        CHANNEL;
    }
    public static Object parse(CLASSTYPE classtype,String json){
        switch (classtype){
            case PINGINFO:
                return parsePingInfos(json);
            case SEARCH:
                return parseSearchResult(json);
            case CHANNEL:
                return parseChannel(json);
        }
        return null;
    }

    private static Channel parseChannel(String json){
        return null;
    }

    private static List<SearchResult> parseSearchResult(String json){
        JSONObject object = (JSONObject) JSON.parse(json);
        JSONArray data = object.getJSONArray("data");
        if (data == null || data.size() <1){
            return null;
        }
        JSONObject docList = data.getJSONObject(0).getJSONObject("doclist");
        if (docList!=null){
            List<SearchResult> result = new ArrayList<>();
            JSONArray docs = docList.getJSONArray("docs");
            if (docs!=null && docs.size() > 0){
                for (int i=0;i<docs.size();i++){
                    JSONObject doc = docs.getJSONObject(i);
                    SearchResult temp = new SearchResult();
                    temp.setName(doc.getString("title"));
                    temp.setChannelId(doc.getIntValue("id"));
                    result.add(temp);
                }
            }
            return result;
        }
        return null;

    }

    private static List<PingInfo> parsePingInfos(String json){
        JSONObject object = (JSONObject) JSON.parse(json);
        JSONObject data = object.getJSONObject("data");
        JSONObject hls = data.getJSONObject("radiostations_hls");
        JSONArray hls_mediacenters = hls.getJSONArray("mediacenters");
        List<PingInfo> result = new ArrayList<>();
        if (hls_mediacenters!=null && hls_mediacenters.size() > 0){
            for (int i=0;i<hls_mediacenters.size();i++){
                PingInfo temp = parsePingInfo(hls_mediacenters.getJSONObject(i));
                if (temp!=null){
                    result.add(temp);
                }
            }
        }
//        JSONObject http = data.getJSONObject("radiostations_http");
//        JSONArray http_mediacenters = http.getJSONArray("mediacenters");
//        if (http_mediacenters!=null && http_mediacenters.size() > 0){
//            for (int i=0;i<http_mediacenters.size();i++){
//                PingInfo temp = parsePingInfo(http_mediacenters.getJSONObject(i));
//                if (temp!=null){
//                    result.add(temp);
//                }
//            }
//        }
        return result;
    }

    private static PingInfo parsePingInfo(JSONObject object){
        PingInfo info = new PingInfo();
        info.setAccess(object.getString("access"));
        info.setDomain(object.getString("domain"));
        info.setTestPath(object.getString("test_path"));
        info.setBackupIps(object.getString("backup_ips"));
        info.setWeight(object.getIntValue("weight"));
        return info;
    }
}
