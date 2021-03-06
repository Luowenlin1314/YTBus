package com.third.ytbus;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.third.ytbus.activity.SelectSpaceActivity;
import com.third.ytbus.base.ActivityFragmentInject;
import com.third.ytbus.base.BaseActivity;
import com.third.ytbus.bean.ADPlayBean;
import com.third.ytbus.bean.PlayDataBean;
import com.third.ytbus.manager.SerialInterface;
import com.third.ytbus.utils.Contans;
import com.third.ytbus.utils.KeyEventUtils;
import com.third.ytbus.utils.ParseFileUtil;
import com.third.ytbus.utils.PreferenceUtils;
import com.third.ytbus.utils.YTBusConfigData;
import com.third.ytbus.widget.YTVideoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
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
    private ImageView imgError;

    private PlayDataBean playDataBean;
    private List<ADPlayBean> adPlayBeanList;
    private ADPlayBean adPlayBean;
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
        imgError = (ImageView) findViewById(R.id.img_error);
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
        mHandler.sendEmptyMessageDelayed(WHAT_OPEN_SERIAL,3000);
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
                mHandler.removeMessages(WHAT_PLAY_AD);
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
            if(adPlayBeanList != null && adPlayBeanList.size() > 0){
                adPlayBean = adPlayBeanList.get(0);
                calculateNextADPlayTime(adPlayBean.getAdPlayStartTime());
            }
            if (!TextUtils.isEmpty(lastPlayPath)) {
                if (lastPlayPath.contains(playDataBean.getDefaultPlayPath())) {
                    ytVideoView.seekTo(lastPlayTime);
                }
            }
        } else {
            someError();
            Toast.makeText(this,"配置文件错误",Toast.LENGTH_LONG).show();
        }

    }

    private void someError(){
        imgError.setVisibility(View.VISIBLE);
    }

    //播放广告时先保存电影的位置
    private int tempPlayPosition = 0;
    private String tempPlayPath = "";

    private void handleChangePlay() {
        String adPlayPath = adPlayBean.getAdPlayPath();
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
        String adContent = adPlayBean.getAdContent();
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
            List<PlayDataBean> playDataBeanList = ParseFileUtil.parsePlayData(new FileInputStream(configFile));
            if (playDataBeanList != null && playDataBeanList.size() > 0) {
                adPlayBeanList = playDataBeanList.get(0).getAdPlayBeanList();
                return playDataBeanList.get(0);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this,"配置文件有问题，请检查！",Toast.LENGTH_LONG).show();
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
            someError();
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
                    mHandler.removeMessages(WHAT_PLAY_AD);
                    mHandler.sendEmptyMessageDelayed(WHAT_PLAY_AD, nextAdTime - currentTime);
                    Log.e("ZM", "播放内容：" + adPlayBean.getAdContent());
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
        String topName = getRunningActivityName();
        if(topName.contains("SelectSpaceActivity")
                || topName.contains("SelectSpaceActivity")){
            return;
        }
        Intent toDetail = new Intent(this,SelectSpaceActivity.class);
        startActivityForResult(toDetail,1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data == null){
            return;
        }
        String urlPath = data.getStringExtra("selectPath");
        if(requestCode == 1){
            if(!TextUtils.isEmpty(urlPath)){
                updatePlayConfig(urlPath);
            }
        }
    }

    /**
     * 打开串口,固件串口3，波特率115200
     */
    public static String USEING_PORT = "/dev/ttyS2";
    public static int USEING_RATE = 115200;
    private void openSerial(){
        try {
            if(playDataBean != null){
                String serialPort = playDataBean.getDefaultSerialPort();
                String serialRate = playDataBean.getDefaultSerialRate();
                USEING_PORT = serialPort;
                USEING_RATE = Integer.valueOf(serialRate);
                SerialInterface.openSerialPort(USEING_PORT,USEING_RATE);
                SerialInterface.changeActionReceiver(SerialInterface.getActions(USEING_PORT));
            }else{
                SerialInterface.openSerialPort(USEING_PORT,USEING_RATE);
            }
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

    /**
     * 获取顶部activity
     * @return
     */
    private String getRunningActivityName(){
        ActivityManager activityManager=(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        String runningActivity=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
        return runningActivity;
    }
}
