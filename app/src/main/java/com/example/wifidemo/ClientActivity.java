package com.example.wifidemo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientActivity extends AppCompatActivity {

    private EditText et_ip;
    private EditText et_msg;
    private Button btn_confirm;
    private Button btn_send;

    private Socket mSocket;
    private OutputStream mOutStream;
    private InputStream mInStream;
    private SocketConnectThread socketConnectThread;
    private StringBuffer sb = new StringBuffer();
    private TextView tv_msg;

    private final String TAG="WifiDemoLogClientActivity";

    @SuppressLint("HandlerLeak")
    public Handler handler = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1){
                Bundle data = msg.getData();
                sb.append(data.getString("msg"));
                sb.append("\n");
                tv_msg.setText(sb.toString());
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
        et_ip = (EditText) findViewById(R.id.et_ip);
        et_msg = (EditText) findViewById(R.id.et_msg);
        btn_send =  findViewById(R.id.btn_send);
        btn_confirm =  findViewById(R.id.btn_confirm);
        tv_msg = (TextView) findViewById(R.id.tv_msg);
    }

    private void setListener() {
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSocket==null) {
                    Toast.makeText(ClientActivity.this, "未进行连接", Toast.LENGTH_SHORT).show();
                    return;
                }
                send(et_msg.getText().toString());
            }
        });
        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                socketConnectThread.start();
                btn_confirm.setEnabled(false);
            }
        });
    }


    class SocketConnectThread extends Thread{
        public void run(){
            Log.d(TAG, "run: ============线程启动" );
            try {
                //指定ip地址和端口号
                mSocket = new Socket(et_ip.getText().toString(), 1989);
                if(mSocket != null){
                    //获取输出流、输入流
                    mOutStream = mSocket.getOutputStream();
                    mInStream = mSocket.getInputStream();
                }else {
                    Log.d(TAG, "run: =========scoket==null");
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            Log.d(TAG,"connect success========================================");
            startReader(mSocket);
        }

    }

    public void send(final String str) {
        if (str.length() == 0){
            return;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    // socket.getInputStream()
                    DataOutputStream writer = new DataOutputStream(mSocket.getOutputStream());
                    writer.writeUTF(str); // 写一个UTF-8的信息
                    Log.d(TAG,"发送消息");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 从参数的Socket里获取最新的消息
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
                        Log.d(TAG,"*等待客户端输入*");
                        // 读取数据
                        String msg = reader.readUTF();
                        Log.d(TAG,"获取到客户端的信息：=" + msg);
                        Message message = new Message();
                        message.what = 1;
                        Bundle bundle = new Bundle();
                        bundle.putString("msg", msg);
                        message.setData(bundle);
                        handler.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}