package com.third.ytbus.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.third.ytbus.manager.SerialInterface;
import com.third.ytbus.manager.SerialManager;
import com.third.ytbus.utils.Contans;
import com.third.ytbus.utils.SerialUtils;
import com.third.ytbus.utils.YTBusProData;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作者：Sky on 2018/3/5.
 * 用途：//TODO
 */

public class SerialService extends Service {

    /**
     * 用来保存串口映射
     */
    private ConcurrentHashMap<String,SerialManager> mSerialConnections = new ConcurrentHashMap<String,SerialManager>();

    public static SerialService mSerialService;
    private SerialDataReceiver mSerialDataReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSerialService = this;
        changeActionReceiver(SerialInterface.getActions("/dev/ttyS3"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("ZOM", "onDestroy");
        mSerialService = null;
        unRegistSerialDataReceiver();
    }

    	/*------------------------------ 功能一、基本串口服务  ------------------------------*/

    /**
     * 打开串口
     * @throws Exception
     */
    public synchronized void openSerialPort(String serialPath,int iBaudRate) throws Exception{
        Log.d("ZOM", "openSerialPort");
        SerialManager skySerialManager = mSerialConnections.get(serialPath);

        if(skySerialManager == null){
            Log.d("ZOM", "skySerialManager == null");
            SerialManager manager = new SerialManager(this,new File(serialPath), iBaudRate);
            manager.openSerial();
            Log.d("ZOM", "openSerialPort   serialPath" + serialPath);
            mSerialConnections.put(serialPath, manager);
        }else{
            boolean open = skySerialManager.isOpen();
            if(!open){
                skySerialManager.openSerial();
            }
        }
    }

    /**
     * 关闭串口
     * @param serialPath
     */
    public synchronized void closeSerialPort(String serialPath){
        Log.d("ZOM", "closeSerialPort");
        SerialManager skySerialManager = mSerialConnections.get(serialPath);
        if(skySerialManager != null){
            skySerialManager.closeSerial();
            mSerialConnections.remove(serialPath, skySerialManager);
        }
    }

    /**
     * 关闭串口连接
     */
    public synchronized void closeAllSerialPort(){
        for(Map.Entry<String,SerialManager> entry : mSerialConnections.entrySet()){
            SerialManager sm = entry.getValue();
            sm.closeSerial();
        }
        mSerialConnections.clear();
    }

    /**
     * 发送字符串
     * @param msg
     */
    public void sendMsg2SerialPort(String serialPath,String msg){
        SerialManager skySerialManager = mSerialConnections.get(serialPath);
        if(skySerialManager != null){
            skySerialManager.sendMessageString(msg);
        }
    }

    /**
     * 发送字符数组
     * @param msg
     */
    public void sendMsg2SerialPort(String serialPath,byte[] msg){
        Log.d("ZOM", "sendMsg2SerialPort   serialPath"+serialPath);
        SerialManager skySerialManager = mSerialConnections.get(serialPath);
        if(skySerialManager != null){
            System.out.println("skySerialManager");
            skySerialManager.sendMessaegByteArray(msg);
        }
    }

    /**
     * 发送字符串消息(16进制形式)到串口
     * @param msg
     */
    public void sendHexMsg2SerialPort(String serialPath,String msg){
        SerialManager skySerialManager = mSerialConnections.get(serialPath);
        if(skySerialManager != null){
            skySerialManager.sendMessageHexString(msg);
        }
    }

	/*------------------------------ end  ------------------------------*/


    public void changeActionReceiver(String actions){
        unRegistSerialDataReceiver();
        registSerialDataReceiver(actions);
    }

    /**
     * 注册串口广播
     */
    private void registSerialDataReceiver(String actions){
        if(mSerialDataReceiver == null){
            mSerialDataReceiver = new SerialDataReceiver();
            registerReceiver(mSerialDataReceiver, new IntentFilter(actions));
        }
    }

    /**
     * 取消串口广播
     */
    private void unRegistSerialDataReceiver(){
        if(mSerialDataReceiver != null){
            unregisterReceiver(mSerialDataReceiver);
            mSerialDataReceiver = null;
        }
    }



    private class SerialDataReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] arrayExtra = intent.getByteArrayExtra(SerialInterface.EXTRA_NAME);
            proFilter(arrayExtra);
        }

    }


    /**
     * 协议过滤
     */
    private static final int LIMIT_LENGTH = 6 * 2;
    private String reciverString = "";
    public void proFilter(byte[] data) {
        doSomething(SerialUtils.bytes2HexString(data));
    }

    /**
     * 协议处理
     * @param buf
     */
    private void doSomething(String buf) {
        reciverString += buf;
        if (!reciverString.startsWith(YTBusProData.PRO_HEAD)
                && !reciverString.contains(YTBusProData.PRO_HEAD)) {
            reciverString = "";
            return;
        } else {
            int startIndex = reciverString.indexOf(YTBusProData.PRO_HEAD);
            reciverString = reciverString.substring(startIndex);
        }

        //4059 0200 0100 01
        System.out.println("reciverString-->"+reciverString);

        if (reciverString.length() > LIMIT_LENGTH) {
            String low = reciverString.substring(8, 10);// 数据长度低位
            String high = reciverString.substring(10, 12); // 数据长度高位
            String hexadecimal = high + low;
            Log.e("ZM", "数据长度：" + hexadecimal);
            int decimalism = Integer.valueOf(hexadecimal, 16);
            // 数据包最低长度
            if (reciverString.length() >= (decimalism + 6) * 2) {
                String result = reciverString.substring(0,
                        (decimalism + 6) * 2);
                String validData = result.substring(3 * 4,
                        (decimalism + 6) * 2);// 有效数据

                String cmdLow = reciverString.substring(4, 6);// 数据长度低位
                String cmdHeigh = reciverString.substring(6, 8); // 数据长度高位
                String cmd = cmdHeigh + cmdLow;

                Log.e("ZM", "密令头：" + cmd);
                Log.e("ZM", "有效数据：" + validData);

                toHandler(validData);

                reciverString = reciverString.substring((decimalism + 6) * 2,
                        reciverString.length());

                if (reciverString.length() > 0) {
                    doSomething("");
                }
            }
        }
    }

    private void toHandler(String data){
        Intent intent = new Intent(Contans.INTENT_YT_COM);
        intent.putExtra("comValue",Integer.valueOf(data));
        sendBroadcast(intent);
    }

}
