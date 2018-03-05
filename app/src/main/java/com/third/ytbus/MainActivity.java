package com.third.ytbus;

import android.os.Environment;
import android.os.Message;
import android.view.View;

import com.third.ytbus.base.ActivityFragmentInject;
import com.third.ytbus.base.BaseActivity;
import com.third.ytbus.widget.YTVideoView;

@ActivityFragmentInject(
        contentViewId = R.layout.activity_main,
        hasNavigationView = false,
        hasToolbar = false
)
public class MainActivity extends BaseActivity {

    private YTVideoView ytVideoView;
    private String testPath;

    @Override
    protected void toHandleMessage(Message msg) {

    }

    @Override
    protected void findViewAfterViewCreate() {
        ytVideoView = (YTVideoView) findViewById(R.id.ytVideoView);
    }

    @Override
    protected void initDataAfterFindView() {
        testPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DownLoad/test.mp4";
        ytVideoView.setVideoPath(testPath);
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
