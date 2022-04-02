package com.example.wifidemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ESP8266ClientActivity extends AppCompatActivity {

    private String mIp;//硬件的IP
    private int mPort = 5000;//硬件的端口
    private EditText et_ip;//输入硬件对应的IP
    private EditText et_msg;//输入要发送的消息
    private Button btn_confirm;//进行连接
    private Button btn_send;//发送消息
//    Socket mSocket = null;//连接成功可得到的Socket
//    OutputStream outputStream = null;//定义输出流
//    InputStream inputStream = null;//定义输入流
    private StringBuffer sb = new StringBuffer();//消息
    private TextView tv_msg;//显示消息
    private boolean connectFlage = true;//连接成功或连接3s后变false
    private TextView connetStatusTextView;//显示连接状态
    private int ShowPointSum = 0;//连接时显示 连接中.. 后面点的计数

    private final String TAG = "WifiDemoLogESP8266ClientActivity";
    private LocalBroadcastManager localBroadcastManager;//本地广播管理器
    private MyLocalBroadcastReceiver localBroadcastReceiver;//广播接收者
    private int connectingCount=0;//用来刷新 正在连接 与 正 在 连 接


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esp8266_client);
        initView();//初始化控件
        setListener();//设置Button的点击事件
        registerBroadcastReceiver();//广播注册
    }

    /**
     * 初始化控件
     */
    private void initView() {
        et_ip = (EditText) findViewById(R.id.et_ipESP8266);
        et_msg = (EditText) findViewById(R.id.et_msgESP8266);
        btn_send = findViewById(R.id.btn_sendESP8266);
        btn_confirm = findViewById(R.id.btn_confirmESP8266);
        tv_msg = (TextView) findViewById(R.id.tv_msgESP8266);
        connetStatusTextView = findViewById(R.id.connetStatusTV);
    }

    /**
     * 设置Button的点击事件
     */
    private void setListener() {
        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_confirm.setEnabled(false);//防止正在连接时再次点击连接
                WiFiModeUtil.connectFlage=true;
                mIp = et_ip.getText().toString();//得到IP
                WiFiModeUtil.connetByTCP(mIp,mPort);//进行连接
            }
        });
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (WiFiModeUtil.mSocket == null) {
                    Toast.makeText(ESP8266ClientActivity.this, "未连接任何设备~~", Toast.LENGTH_SHORT).show();
                    return;
                }
                WiFiModeUtil.sendData(et_msg.getText().toString());//发送数据
            }
        });
    }


    @Override
    protected void onDestroy() {
        localBroadcastManager.unregisterReceiver(localBroadcastReceiver);//注销广播
        WiFiModeUtil.closeSocketAndStream();//关闭Socket释放资源
        super.onDestroy();
    }

    /**
     * 广播注册
     */
    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("WiFiModeUtil.Connecting");//正在连接
        intentFilter.addAction("WiFiModeUtil.Connect.Succeed");//连接成功
        intentFilter.addAction("WiFiModeUtil.Connect.Fail");//连接失败
        intentFilter.addAction("WiFiModeUtil.Connect.ReceiveMessage");//接收到数据
        intentFilter.addAction("WiFiModeUtil.Disconnected");//接收到数据
        localBroadcastReceiver = new MyLocalBroadcastReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        WiFiModeUtil.localBroadcastManager=localBroadcastManager;//给WiFiModeUtil工具类中的本地广播管理器赋值
        localBroadcastManager.registerReceiver(localBroadcastReceiver,intentFilter);
    }

    /**
     * 本地广播接收者
     */
    class MyLocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "WiFiModeUtil.Connecting":
                    connectingCount++;
                    if(connectingCount%2==0)
                        connetStatusTextView.setText("正在连接");
                    else
                        connetStatusTextView.setText("正 在 连 接");
                    break;
                case "WiFiModeUtil.Connect.Succeed":
                    connetStatusTextView.setText("连接成功");
                    btn_confirm.setEnabled(true);
                    break;
                case "WiFiModeUtil.Connect.Fail":
                    connetStatusTextView.setText("连接失败");
                    btn_confirm.setEnabled(true);
                    break;
                case "WiFiModeUtil.Connect.ReceiveMessage":
                    tv_msg.setText(WiFiModeUtil.DataRecivice.toString());
                    break;
                case "WiFiModeUtil.Disconnected":
                    tv_msg.setText("连接已断开，请重新进行连接");
                    break;
            }
        }
    }
}