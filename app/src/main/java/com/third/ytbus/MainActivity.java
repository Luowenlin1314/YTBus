package com.third.ytbus;

import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.third.ytbus.base.ActivityFragmentInject;
import com.third.ytbus.base.BaseActivity;
import com.third.ytbus.bean.PlayDataBean;
import com.third.ytbus.utils.ParseFileUtil;
import com.third.ytbus.utils.PreferenceUtils;
import com.third.ytbus.utils.YTBusConfigData;
import com.third.ytbus.widget.YTVideoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ActivityFragmentInject(
        contentViewId = R.layout.activity_main,
        hasNavigationView = false,
        hasToolbar = false
)
/**
 * 1、解析配置文件-->保存到sp(用作备份，防止配置文件丢失)
 * 2、播放视频，计算下次广告播放的时间
 * 3、到点播放广告
 * 4、广告回来继续播放视频
 */
public class MainActivity extends BaseActivity {

    private static final int WHAT_PLAY_AD = 11;

    private YTVideoView ytVideoView;
    private PlayDataBean playDataBean;
    private String ytFileRootPath;

    @Override
    protected void toHandleMessage(Message msg) {
        switch (msg.what){
            case WHAT_PLAY_AD:
                handleChangePlay();
                break;
        }
    }

    @Override
    protected void findViewAfterViewCreate() {
        ytVideoView = (YTVideoView) findViewById(R.id.ytVideoView);
    }

    @Override
    protected void initDataAfterFindView() {
        PreferenceUtils.init(this);
        ytVideoViewOnCompletionListener();
        ytFileRootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DownLoad/";
        playDataBean = parseConfigFile();
    }

    @Override
    protected void onPause() {
        super.onPause();
        doOnPauseThings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        doOnResumeThings();
    }

    private void doOnPauseThings(){
        if(ytVideoView != null){
            if(ytVideoView.isPlaying()){
                tempPlayPosition = ytVideoView.getCurrentPosition();

            }
        }
    }

    private void doOnResumeThings(){
        if(playDataBean != null){
            playDefaultVideo(playDataBean.getDefaultPlayPath());
            calculateNextADPlayTime(playDataBean.getAdPlayStartTime());
        }else{
            playDefaultVideo(YTBusConfigData.DEFAULT_PLAY_PATH);
        }
    }

    //播放广告时先保存电影的位置
    private int tempPlayPosition = 0;
    private void handleChangePlay(){
        String adPlayPath = playDataBean.getAdPlayPath();
        if(playDataBean != null && !TextUtils.isEmpty(adPlayPath)){
            File adFile = new File(ytFileRootPath, adPlayPath);
            if(adFile != null && adFile.exists()){
                if(ytVideoView.isPlaying()){
                    tempPlayPosition = ytVideoView.getCurrentPosition();
                }
                ytVideoView.setVideoPath(ytFileRootPath + adPlayPath);
            }
        }
    }

    //设置播放完成的监听
    private void ytVideoViewOnCompletionListener(){
        ytVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(playDataBean != null){
                    playDefaultVideo(playDataBean.getDefaultPlayPath());
                }else{
                    playDefaultVideo(YTBusConfigData.DEFAULT_PLAY_PATH);
                }
                if(tempPlayPosition > 0){
                    ytVideoView.seekTo(tempPlayPosition);
                    tempPlayPosition = 0;
                }
            }
        });
    }

    //1、解析配置文件,如果解析出错，直接播放默认文件，默认文件不存在，播放内置视频
    private PlayDataBean parseConfigFile(){
        try {
            File configFile = new File(ytFileRootPath, YTBusConfigData.YTBusConfigFilePath);
            String root = "configRoot";
            List<String> files = new ArrayList<>();
            files.add("defaultPlayPath");
            files.add("adPlayStartTime");
            files.add("adPlayPath");
            List<String> elements = new ArrayList<>();
            elements.add("DEFAULT_PLAY");
            elements.add("AD_PLAY_TIME");
            elements.add("AD_PLAY_PATH");
            List<PlayDataBean> playDataBeanList = ParseFileUtil.parse(new FileInputStream(configFile),
                    PlayDataBean.class,files,elements,root);
            if(playDataBeanList != null && playDataBeanList.size() > 0){
                return playDataBeanList.get(0);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    //2、播放视频，如果视频不存在，播放内置视频
    private void playDefaultVideo(String defaultPlayFilePath){
        File defaultVideoFile = new File(ytFileRootPath, defaultPlayFilePath);
        if(defaultVideoFile != null && defaultVideoFile.exists()){
            ytVideoView.setVideoPath(ytFileRootPath + defaultPlayFilePath);
        }else{
            ytVideoView.setVideoPath(ytFileRootPath + YTBusConfigData.DEFAULT_PLAY_PATH);
        }
    }

    //3、计算下次广告播放的时间
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private void calculateNextADPlayTime(String adPlayTime){
        if(!TextUtils.isEmpty(adPlayTime)){
            try{
                long nextAdTime = sdf.parse(adPlayTime).getTime();
                long currentTime = sdf.parse(sdf.format(new Date())).getTime();
                if(nextAdTime >= currentTime){
                    mHandler.sendEmptyMessageDelayed(WHAT_PLAY_AD,nextAdTime - currentTime);
                }
            }catch (Exception e){
                //不处理
                Log.e("ZM",e.getMessage());
            }
        }
    }

    public void play(View v){
        ytVideoView.start();
    }

    public void pause(View v){
        ytVideoView.pause();
    }

    public void seekTo(View v){
        ytVideoView.seekTo(ytVideoView.getCurrentPosition() + 10 * 1000);
    }
}
