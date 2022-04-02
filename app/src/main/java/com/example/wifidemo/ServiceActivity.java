package com.example.wifidemo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServiceActivity extends AppCompatActivity {

    private Button clearBtn;
    private Button showIPBtn;
    private TextView ipTV;
    private TextView msgTV;
    private ServerSocket mServerSocket;
    private Socket mSocket;
    private StringBuffer stringBuffer = new StringBuffer();
    private final String TAG = "WifiDemoLogServiceActivity";


    @SuppressLint("HandlerLeak")
    public Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                stringBuffer.append(msg.obj);
                stringBuffer.append("\n");
                msgTV.setText(stringBuffer.toString());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        initView();
        setListener();

        try {
            mServerSocket = new ServerSocket(5000);//端口号5000
        } catch (IOException e) {
            e.printStackTrace();
        }
        //启动服务线程
        SocketAcceptThread socketAcceptThread = new SocketAcceptThread();
        socketAcceptThread.start();
    }

    /**
     * 获得控件实例
     */
    private void initView() {
        clearBtn = findViewById(R.id.clearBtn);
        showIPBtn = findViewById(R.id.showIPBtn);
        ipTV = findViewById(R.id.ipTV);
        msgTV = findViewById(R.id.msgTV);
    }

    /**
     * 为控件设置监听
     */
    private void setListener() {
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stringBuffer.setLength(0);
                msgTV.setText("");
            }
        });
        showIPBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                ipTV.setText(getLocalIpAddress(ServiceActivity.this) + ":5000");
            }
        });

    }


    /**
     * 连接线程
     * 得到Socket
     */
    class SocketAcceptThread extends Thread {
        @Override
        public void run() {
            try {
                //等待客户端的连接，Accept会阻塞，直到建立连接，
                //所以需要放在子线程中运行
                mSocket = mServerSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            //启动消息接收线程
            startReader(mSocket);
        }
    }

    /**
     * 从参数的Socket里获取最新的消息
     */
    private void startReader(final Socket socket) {

        new Thread() {
            @Override
            public void run() {
                DataInputStream reader;
                try {
                    // 获取读取流
                    reader = new DataInputStream(socket.getInputStream());
                    while (true) {
                        // 读取数据
                        String msg = reader.readUTF();
                        Log.d(TAG, "客户端的信息:" + msg);

                        //告知客户端消息收到
                        DataOutputStream writer = new DataOutputStream(mSocket.getOutputStream());
                        writer.writeUTF("收到:" + msg); // 写一个UTF-8的信息

                        //发消息更新UI
                        Message message = new Message();
                        message.what = 1;
                        message.obj=msg;
                        handler.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(mSocket!=null){
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    /**
     * 将ip的整数形式转换成ip形式
     *
     * @param ipInt
     * @return
     */
    public static String int2ip(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    /**
     * 获取当前ip地址
     *
     * @param context
     * @return
     */
    public static String getLocalIpAddress(Context context) {
        try {

            WifiManager wifiManager = (WifiManager) context
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int i = wifiInfo.getIpAddress();
            return int2ip(i);
        } catch (Exception ex) {
            return " 获取IP出错鸟!!!!请保证是WIFI,或者请重新打开网络!\n" + ex.getMessage();
        }
        // return null;
    }
}