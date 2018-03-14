package com.third.ytbus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.third.ytbus.base.ActivityFragmentInject;
import com.third.ytbus.base.BaseActivity;
import com.third.ytbus.bean.PlayDataBean;
import com.third.ytbus.manager.SerialInterface;
import com.third.ytbus.utils.Contans;
import com.third.ytbus.utils.IntentUtils;
import com.third.ytbus.utils.KeyEventUtils;
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
import java.util.HashMap;
import java.util.List;

import static com.third.ytbus.utils.Contans.COM_00;
import static com.third.ytbus.utils.Contans.COM_01;
import static com.third.ytbus.utils.Contans.COM_02;
import static com.third.ytbus.utils.Contans.COM_03;
import static com.third.ytbus.utils.Contans.COM_04;
import static com.third.ytbus.utils.Contans.COM_05;
import static com.third.ytbus.utils.Contans.COM_06;
import static com.third.ytbus.utils.Contans.COM_12;

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

    private static final int WHAT_OPEN_SERIAL = 10;
    private static final int WHAT_PLAY_AD = 11;
    private static final String SP_KEY_PLAY_PATH = "playPath";
    private static final String SP_KEY_PLAY_TIME = "playTime";
    private YTVideoView ytVideoView;
    private TextView ytAdTextView;
    private PlayDataBean playDataBean;
    private String ytFileRootPath;

    @Override
    protected void toHandleMessage(Message msg) {
        switch (msg.what) {
            case WHAT_OPEN_SERIAL:
                openSerial();
                break;
            case WHAT_PLAY_AD:
                handleChangePlay();
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void findViewAfterViewCreate() {
        ytVideoView = (YTVideoView) findViewById(R.id.ytVideoView);
        ytAdTextView = (TextView) findViewById(R.id.txt_ad_content);
    }

    @Override
    protected void initDataAfterFindView() {
        PreferenceUtils.init(this);
        SerialInterface.serialInit(this);
        ytVideoViewOnCompletionListener();
        ytFileRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(ytFileRootPath,"YTBus");
        if(file == null || !file.exists()){
            file.mkdirs();
        }
        playDataBean = parseConfigFile();
        registerYTProReceiver();
        mHandler.sendEmptyMessageDelayed(WHAT_OPEN_SERIAL,2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterYTProReceiver();
        SerialInterface.closeAllSerialPort();
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

    private void doOnPauseThings() {
        if (ytVideoView != null) {
            if (ytVideoView.isPlaying()) {
                //保存当前播放的地址跟位置
                tempPlayPosition = ytVideoView.getCurrentPosition();
                tempPlayPath = ytVideoView.getVideoPath();
                if (!TextUtils.isEmpty(tempPlayPath)) {
                    PreferenceUtils.commitString(SP_KEY_PLAY_PATH, tempPlayPath);
                }
                if (tempPlayPosition > 0) {
                    PreferenceUtils.commitInt(SP_KEY_PLAY_TIME, tempPlayPosition);
                }
            }
        }
    }

    private void doOnResumeThings() {
        String lastPlayPath = PreferenceUtils.getString(SP_KEY_PLAY_PATH, "");
        int lastPlayTime = PreferenceUtils.getInt(SP_KEY_PLAY_TIME, 0);
        if (playDataBean != null) {
            playDefaultVideo(playDataBean.getDefaultPlayPath());
            calculateNextADPlayTime(playDataBean.getAdPlayStartTime());
            if (!TextUtils.isEmpty(lastPlayPath)) {
                if (lastPlayPath.contains(playDataBean.getDefaultPlayPath())) {
                    ytVideoView.seekTo(lastPlayTime);
                }
            }
        } else {
            playDefaultVideo(YTBusConfigData.DEFAULT_PLAY_PATH);
        }

    }

    //播放广告时先保存电影的位置
    private int tempPlayPosition = 0;
    private String tempPlayPath = "";

    private void handleChangePlay() {
        String adPlayPath = playDataBean.getAdPlayPath();
        if (playDataBean != null && !TextUtils.isEmpty(adPlayPath)) {
            File adFile = new File(ytFileRootPath, adPlayPath);
            if (adFile != null && adFile.exists()) {
                if (ytVideoView.isPlaying()) {
                    tempPlayPosition = ytVideoView.getCurrentPosition();
                }
                ytVideoView.setVideoPath(ytFileRootPath + adPlayPath);
            }
        }
        //播放字幕
        String adContent = playDataBean.getAdContent();
        if (!TextUtils.isEmpty(adContent)) {
            ytAdTextView.setText(adContent);
            ytAdTextView.setVisibility(View.VISIBLE);
            ytAdTextView.setFocusable(true);
            ytAdTextView.setFocusableInTouchMode(true);
        }
    }

    //设置播放完成的监听
    private void ytVideoViewOnCompletionListener() {
        ytVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (playDataBean != null) {
                    playDefaultVideo(playDataBean.getDefaultPlayPath());
                } else {
                    playDefaultVideo(YTBusConfigData.DEFAULT_PLAY_PATH);
                }
                if (tempPlayPosition > 0) {
                    ytVideoView.seekTo(tempPlayPosition);
                    tempPlayPosition = 0;
                }
                ytAdTextView.setVisibility(View.INVISIBLE);
            }
        });
    }

    //1、解析配置文件,如果解析出错，直接播放默认文件，默认文件不存在，播放内置视频
    private PlayDataBean parseConfigFile() {
        try {
            File configFile = new File(ytFileRootPath, YTBusConfigData.YTBusConfigFilePath);
            String root = "configRoot";
            List<String> files = new ArrayList<>();
            files.add("defaultPlayPath");
            files.add("adPlayStartTime");
            files.add("adPlayPath");
            files.add("adContent");
            List<String> elements = new ArrayList<>();
            elements.add("DEFAULT_PLAY");
            elements.add("AD_PLAY_TIME");
            elements.add("AD_PLAY_PATH");
            elements.add("AD_TEXT_CONTENT");
            List<PlayDataBean> playDataBeanList = ParseFileUtil.parse(new FileInputStream(configFile),
                    PlayDataBean.class, files, elements, root);
            if (playDataBeanList != null && playDataBeanList.size() > 0) {
                return playDataBeanList.get(0);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    //2、播放视频，如果视频不存在，播放内置视频
    private void playDefaultVideo(String defaultPlayFilePath) {
        File defaultVideoFile = new File(ytFileRootPath, defaultPlayFilePath);
        if (defaultVideoFile != null && defaultVideoFile.exists()) {
            ytVideoView.setVideoPath(ytFileRootPath + defaultPlayFilePath);
        } else {
//            ytVideoView.setVideoPath(ytFileRootPath + YTBusConfigData.DEFAULT_PLAY_PATH);
            Toast.makeText(this,"找不到对应视频，请检查是否存在!",Toast.LENGTH_LONG).show();
        }
    }

    //3、计算下次广告播放的时间
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

    private void calculateNextADPlayTime(String adPlayTime) {
        if (!TextUtils.isEmpty(adPlayTime)) {
            try {
                long nextAdTime = sdf.parse(adPlayTime).getTime();
                long currentTime = sdf.parse(sdf.format(new Date())).getTime();
                if (nextAdTime >= currentTime) {
                    mHandler.sendEmptyMessageDelayed(WHAT_PLAY_AD, nextAdTime - currentTime);
                    Log.e("ZM", "播放内容：" + playDataBean.getAdContent());
                }
            } catch (Exception e) {
                //不处理
                Log.e("ZM", e.getMessage());
            }
        }
    }

    /**
     * 调到文件系统
     */
    private void toFileSystem() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType(“image/*”);//选择图片
        //intent.setType(“audio/*”); //选择音频
//        intent.setType("video/*"); //选择视频 （mp4 3gp 是android支持的视频格式）
        //intent.setType(“video/*;image/*”);//同时选择视频和图片
        intent.setType("*/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String fromPath = "";
            if ("file".equalsIgnoreCase(uri.getScheme())) {//使用第三方应用打开
                fromPath = uri.getPath();
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                fromPath = IntentUtils.getPathFrom4(this, uri);
            } else {//4.4以下下系统调用方法
                fromPath = IntentUtils.getRealPathFromURI(this,uri);
            }
            if(!TextUtils.isEmpty(fromPath) && IntentUtils.isVideo(fromPath)){
                if(fromPath.contains(ytFileRootPath)){
                    fromPath = fromPath.replace(ytFileRootPath,"");
                }
                updatePlayConfig(fromPath);
            }else{
                Toast.makeText(this,"请选择视频文件！",Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 打开串口,固件串口3，波特率115200
     */
    public static String USEING_PORT = "/dev/ttyS3";
    private void openSerial(){
        try {
            SerialInterface.openSerialPort(USEING_PORT,115200);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"串口打开错误，请检查串口是否正常！",Toast.LENGTH_LONG).show();
        }

    }


    private YTProReceiver ytProReceiver;
    private void registerYTProReceiver(){
        ytProReceiver = new YTProReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Contans.INTENT_YT_COM);
        registerReceiver(ytProReceiver,intentFilter);
    }

    private void unRegisterYTProReceiver(){
        if(ytProReceiver != null){
            unregisterReceiver(ytProReceiver);
        }
    }

    private class YTProReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(Contans.INTENT_YT_COM.equals(action)){
                int comValue = intent.getIntExtra("comValue",-1);
                handleCom(comValue);
            }
        }
    }

    /**
     * 处理协议
     * @param comValue
     */
    private void handleCom(int comValue){
        switch (comValue){
            case COM_00:
                KeyEventUtils.sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP);
                break;
            case COM_01:
                KeyEventUtils.sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN);
                break;
            case COM_02:
                KeyEventUtils.sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT);
                break;
            case COM_03:
                KeyEventUtils.sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT);
                break;
            case COM_04:
                KeyEventUtils.sendKeyEvent(KeyEvent.KEYCODE_ENTER);
                break;
            case COM_05:
                doHandle05();
                break;
            case COM_06:
                KeyEventUtils.sendKeyEvent(KeyEvent.KEYCODE_BACK);
                break;
            case COM_12:
                toFileSystem();
                break;
        }
    }

    /**
     * 暂停/播放
     */
    private void doHandle05(){
        if(ytVideoView != null){
            if(ytVideoView.isPlaying()){
                ytVideoView.pause();
            }else{
                ytVideoView.start();;
            }
        }
    }

    /**
     * 更改播放配置
     * @param path
     */
    private void updatePlayConfig(String path){
        HashMap<String,String> hs = new HashMap<>();
        hs.put("DEFAULT_PLAY",path);
        String root = "configRoot";
        File configFile = new File(ytFileRootPath, YTBusConfigData.YTBusConfigFilePath);
        ParseFileUtil.updateTagContents(configFile,hs,root);
        playDataBean = parseConfigFile();
        PreferenceUtils.commitString(SP_KEY_PLAY_PATH, "");
        PreferenceUtils.commitInt(SP_KEY_PLAY_TIME, 0);
    }


    private long timeLimit = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - timeLimit < 1500) {
                finish();
            } else {
                timeLimit = System.currentTimeMillis();
                Toast.makeText(this,"再按一次退出应用",Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}
