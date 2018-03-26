package com.third.ytbus.activity;

import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.third.ytbus.R;
import com.third.ytbus.base.ActivityFragmentInject;
import com.third.ytbus.base.BaseActivity;

import java.io.File;

/**
 * Created by Administrator on 2018/3/25.
 * 选择哪个硬盘
 */
@ActivityFragmentInject(
        contentViewId = R.layout.activity_select_space,
        hasNavigationView = false,
        hasToolbar = false
)
public class SelectSpaceActivity extends BaseActivity {

    //硬盘地址
    private TextView txtSd;
    private ImageView imgBack;
    private RelativeLayout rlSpace0;
    private RelativeLayout rlSpace1;
    private RelativeLayout rlSpace2;
    private RelativeLayout rlSpace3;
    private RelativeLayout rlSpace4;
    private RelativeLayout rlSpace5;

    private String statSpaceUrl = "/mnt/stat";
    private String baseSpaceUrl;
    private String usb0Url = "/mnt/usbhost0";
    private String usb1Url = "/mnt/usbhost1";
    private String usb2Url = "/mnt/usbhost2";
    private String usb3Url = "/mnt/usbhost3";


    @Override
    protected void toHandleMessage(Message msg) {

    }

    @Override
    protected void findViewAfterViewCreate() {
        txtSd = (TextView) findViewById(R.id.txt_sd);
        rlSpace0 = (RelativeLayout) findViewById(R.id.rl_space0);
        rlSpace1 = (RelativeLayout) findViewById(R.id.rl_space1);
        rlSpace2 = (RelativeLayout) findViewById(R.id.rl_space2);
        rlSpace3 = (RelativeLayout) findViewById(R.id.rl_space3);
        rlSpace4 = (RelativeLayout) findViewById(R.id.rl_space4);
        rlSpace5 = (RelativeLayout) findViewById(R.id.rl_space5);
        imgBack  = (ImageView) findViewById(R.id.img_back);
    }

    @Override
    protected void initDataAfterFindView() {
        baseSpaceUrl = Environment.getExternalStorageDirectory().getAbsolutePath();
        txtSd.setText(baseSpaceUrl);
        checkExist();

        rlSpace0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toDetail(statSpaceUrl);
            }
        });
        rlSpace1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toDetail(baseSpaceUrl);
            }
        });
        rlSpace2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toDetail(usb0Url);
            }
        });
        rlSpace3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toDetail(usb1Url);
            }
        });
        rlSpace3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toDetail(usb2Url);
            }
        });
        rlSpace4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toDetail(usb3Url);
            }
        });
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void toDetail(String path){
        Intent toDetail = new Intent(this,SpaceDetailActivity.class);
        toDetail.putExtra("urlPath",path);
        startActivityForResult(toDetail,1);
    }

    /**
     * 查看地址是否存在
     */
    private void checkExist(){
        File statFile = new File(statSpaceUrl);
        if(statFile != null && statFile.exists() && statFile.isDirectory()){
            rlSpace0.setVisibility(View.VISIBLE);
        }else{
            rlSpace0.setVisibility(View.GONE);
        }
        File u0File = new File(statSpaceUrl);
        if(u0File != null && u0File.exists() && u0File.isDirectory()){
            rlSpace2.setVisibility(View.VISIBLE);
        }else{
            rlSpace2.setVisibility(View.GONE);
        }
        File u1File = new File(statSpaceUrl);
        if(u1File != null && u1File.exists() && u1File.isDirectory()){
            rlSpace3.setVisibility(View.VISIBLE);
        }else{
            rlSpace3.setVisibility(View.GONE);
        }
        File u2File = new File(statSpaceUrl);
        if(u2File != null && u2File.exists() && u2File.isDirectory()){
            rlSpace4.setVisibility(View.VISIBLE);
        }else{
            rlSpace4.setVisibility(View.GONE);
        }
        File u3File = new File(statSpaceUrl);
        if(u3File != null && u3File.exists() && u3File.isDirectory()){
            rlSpace5.setVisibility(View.VISIBLE);
        }else{
            rlSpace5.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data == null){
            return;
        }
        String urlPath = data.getStringExtra("selectPath");
        if(requestCode == 1){
            if(!TextUtils.isEmpty(urlPath)){
                Intent mIntent = new Intent();
                mIntent.putExtra("selectPath", urlPath);
                setResult(resultCode, mIntent);
                finish();
            }
        }
    }
}
