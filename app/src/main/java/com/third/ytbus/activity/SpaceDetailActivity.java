package com.third.ytbus.activity;

import android.content.Intent;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.third.ytbus.R;
import com.third.ytbus.adapter.FileDetailAdapter;
import com.third.ytbus.base.ActivityFragmentInject;
import com.third.ytbus.base.BaseActivity;
import com.third.ytbus.utils.SpaceFileUtil;

import java.io.File;
import java.util.List;

/**
 * Created by Administrator on 2018/3/25.
 * 文件详细
 */
@ActivityFragmentInject(
        contentViewId = R.layout.activity_space_detail,
        hasNavigationView = false,
        hasToolbar = false
)
public class SpaceDetailActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private TextView txtTitle;
    private ImageView imgBack;

    private FileDetailAdapter fileDetailAdapter;

    //首层url
    private String urlPath = "";

    private String currentPath = "";

    @Override
    protected void toHandleMessage(Message msg) {

    }

    @Override
    protected void findViewAfterViewCreate() {
        recyclerView = (RecyclerView) findViewById(R.id.recycleview);
        txtTitle = (TextView) findViewById(R.id.txt_path_name);
        imgBack  = (ImageView) findViewById(R.id.img_back);
    }

    @Override
    protected void initDataAfterFindView() {
        urlPath = getIntent().getStringExtra("urlPath");
        txtTitle.setText(urlPath);
        currentPath = urlPath;


        initAdapter();

        updateDate(urlPath);

        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(urlPath.equals(currentPath)){
                    finish();
                }else{
                    File file = new File(currentPath);
                    updateDate(file.getParent());
                }
            }
        });

    }

    private void initAdapter(){
        fileDetailAdapter = new FileDetailAdapter(this,R.layout.item_file,null);
        LinearLayoutManager lm = new LinearLayoutManager(this, 1, false);
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(fileDetailAdapter);
        fileDetailAdapter.setItemClickListener(new FileDetailAdapter.ItemClickListener() {
            @Override
            public void onClick(String path) {
                File file = new File(path);
                if(file != null && file.isDirectory()){
                    updateDate(path);
                }else{
                    int type = SpaceFileUtil.getFileType(path);
                    if(type >= 300 && type < 400){
                        Intent mIntent = new Intent();
                        String relPath = path.replace(urlPath,"");
                        mIntent.putExtra("selectPath", relPath);
                        setResult(1, mIntent);
                        Toast.makeText(SpaceDetailActivity.this,"选择了:" + relPath,Toast.LENGTH_LONG).show();
                        finish();
                    }else{
                        Toast.makeText(SpaceDetailActivity.this,"只能选择视频文件!",Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void updateDate(String path){
        txtTitle.setText(path);
        currentPath = path;
        List<String> files = SpaceFileUtil.getFileByPath(path);
        fileDetailAdapter.updateDatas(files);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(urlPath.equals(currentPath)){
                finish();
            }else{
                File file = new File(currentPath);
                updateDate(file.getParent());
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
