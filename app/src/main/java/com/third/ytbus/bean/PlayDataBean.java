package com.third.ytbus.bean;

/**
 * 作者：Sky on 2018/3/6.
 * 用途：配置相关信息
 */

public class PlayDataBean {

    //默认播放地址
    private String defaultPlayPath;

    //广告时间
    private String adPlayStartTime;

    //广告视频地址
    private String adPlayPath;

    public String getDefaultPlayPath() {
        return defaultPlayPath;
    }

    public void setDefaultPlayPath(String defaultPlayPath) {
        this.defaultPlayPath = defaultPlayPath;
    }

    public String getAdPlayStartTime() {
        return adPlayStartTime;
    }

    public void setAdPlayStartTime(String adPlayStartTime) {
        this.adPlayStartTime = adPlayStartTime;
    }

    public String getAdPlayPath() {
        return adPlayPath;
    }

    public void setAdPlayPath(String adPlayPath) {
        this.adPlayPath = adPlayPath;
    }
}
