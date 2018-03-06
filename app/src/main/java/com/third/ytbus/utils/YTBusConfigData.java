package com.third.ytbus.utils;

/**
 * 作者：Sky on 2018/3/6.
 * 用途：保存配置
 */

public interface YTBusConfigData {

    //内置视频文件
    String DEFAULT_PLAY_PATH = "test.mp4";

    //配置文件相对地址
    String YTBusConfigFilePath = "testConfig.xml";

    //默认视频地址TAG
    String DEFAULT_PLAY_PATH_TAG = "DEFAULT_PLAY";

    //广告视频开始时间TAG
    String AD_PLAY_TIME_TAG = "AD_PLAY_TIME";

    //广告视频地址TAG
    String AD_PLAY_PATH_TAG = "AD_PLAY_PATH";

    //广告字幕TAG
    String AD_TEXT_CONTENT_TAG = "AD_PLAY_TEXT";



}
