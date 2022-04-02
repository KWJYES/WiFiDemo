package com.example.wifidemo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientActivity extends AppCompatActivity {

    private EditText ipET;
    private EditText msgET;
    private Button confirmBtn;
    private Button sendBtn;

    private Socket mSocket;
//    private OutputStream mOutStream;
//    private InputStream mInStream;
    private SocketConnectThread socketConnectThread;
    private StringBuffer stringBuffer = new StringBuffer();
    private TextView msgTV;

    private final String TAG="WifiDemoLogClientActivity";

    @SuppressLint("HandlerLeak")
    public Handler handler = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1){
                stringBuffer.append(msg.obj);
                stringBuffer.append("\n");
                msgTV.setText(stringBuffer.toString());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        socketConnectThread = new SocketConnectThread();
        initView();
        setListener();
    }

    private void initView() {
        ipET = (EditText) findViewById(R.id.ipET);
        msgET = (EditText) findViewById(R.id.msgET);
        sendBtn =  findViewById(R.id.btn_send);
        confirmBtn =  findViewById(R.id.btn_confirm);
        msgTV = (TextView) findViewById(R.id.msgTV);
    }

    private void setListener() {
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSocket==null) {
                    Toast.makeText(ClientActivity.this, "未进行连接", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendMessage(msgET.getText().toString());
            }
        });
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                socketConnectThread.start();
                confirmBtn.setEnabled(false);//连接只点一次
            }
        });
    }

    /**
     * 连接线程
     */
    class SocketConnectThread extends Thread{
        public void run(){
            try {
                //指定ip地址和端口号
                mSocket = new Socket(ipET.getText().toString(), 1989);
                //获取输出流、输入流
//                mOutStream = mSocket.getOutputStream();
//                mInStream = mSocket.getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            startReader(mSocket);
        }

    }

    /**
     * 发送消息
     * @param msg
     */
    public void sendMessage(final String msg) {
        if (msg.length() == 0){
            return;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    DataOutputStream writer = new DataOutputStream(mSocket.getOutputStream());
                    writer.writeUTF(msg); // 写一个UTF-8的信息
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 接收消息
     */
    private void startReader(final Socket socket) {
        new Thread(){
            @Override
            public void run() {
                DataInputStream reader;
                try {
                    // 获取读取流
                    reader = new DataInputStream(socket.getInputStream());
                    while (true) {
                        // 读取数据
                        String msg = reader.readUTF();
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
        if(mSocket!=null){
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}