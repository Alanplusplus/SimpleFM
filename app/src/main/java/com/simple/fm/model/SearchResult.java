package com.simple.fm.model;

/**
 * Created by Alan on 16/7/19.
 */
public class SearchResult {
    private String name;
    private int channelId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "channelId=" + channelId +
                ", name='" + name + '\'' +
                '}';
    }
}
