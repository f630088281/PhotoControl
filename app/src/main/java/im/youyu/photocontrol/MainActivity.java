package im.youyu.photocontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends FragmentActivity {
    private final Context mContext = MainActivity.this;

    private Button startButton;
    private EditText IPText;

    private TextView recvText;

    private boolean isConnecting = false;
    private Thread mThreadClient = null;
    private Socket mSocketClient = null;
    static BufferedReader mBufferedReaderClient	= null;
    static PrintWriter mPrintWriterClient = null;
    private  String recvMessageClient = "";
    private  String recvMessageServer = "";

    private Button btn_online, btn_offline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .penaltyDeath()
                .build());

        IPText= (EditText)findViewById(R.id.IPText);
        IPText.setText("192.168.0.1:8081");
        startButton= (Button)findViewById(R.id.StartConnect);
        startButton.setOnClickListener(StartClickListener);
        recvText = (TextView)findViewById(R.id.RecvText);

        btn_online = (Button)findViewById(R.id.btn1);
        btn_offline = (Button)findViewById(R.id.btn2);

        btn_online.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firstModel();
            }
        });

        btn_offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                secondModel();
            }
        });
    }

    public void firstModel(){
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        Fragment fm = DialogFrag.newInstance();
        transaction.replace(R.id.layout_content, fm);
        transaction.commit();
    }

    public void secondModel(){
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        Fragment fm = NoDialogFrag.newInstance();
        transaction.replace(R.id.layout_content, fm);
        transaction.commit();
    }

    public void cmd(String resoutStr){
        if ( isConnecting && mSocketClient!=null) {
            String cmdStr = "";
            if (resoutStr.contains("前进")){
                cmdStr = "forward";
            }else if (resoutStr.contains("后退")) {
                cmdStr = "back";
            }else if (resoutStr.contains("左转")) {
                cmdStr = "left";
            }else if (resoutStr.contains("右转")) {
                cmdStr = "right";
            }else if (resoutStr.contains("停止")) {
                cmdStr = "stop";
            }else if (resoutStr.contains("暂停")) {
                cmdStr = "astop";
            }else if (resoutStr.contains("加速")) {
                cmdStr = "speedup";
            }else if (resoutStr.contains("减速")) {
                cmdStr = "slowdown";
            }else if (resoutStr.contains("寻找线路")){
                cmdStr = "tracking";
            }else if (resoutStr.contains("避开障碍")){
                cmdStr = "avoidance";
            }else {
                Toast.makeText(mContext, "没有这个指令", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.e("cmd", cmdStr);
            try {
                mPrintWriterClient.print(cmdStr);
                mPrintWriterClient.flush();
            } catch (Exception e) {
                // TODO: handle exception
                Toast.makeText(mContext, "发送异常：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
        }
    }

    private View.OnClickListener StartClickListener = new View.OnClickListener() {
        //@Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
            if (isConnecting)
            {
                isConnecting = false;
                try {
                    if(mSocketClient!=null)
                    {
                        mSocketClient.close();
                        mSocketClient = null;

                        mPrintWriterClient.close();
                        mPrintWriterClient = null;
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mThreadClient.interrupt();

                startButton.setText("开始连接");
                IPText.setEnabled(true);
                recvText.setText("信息:\n");
            }
            else
            {
                isConnecting = true;
                startButton.setText("停止连接");
                IPText.setEnabled(false);

                mThreadClient = new Thread(mRunnable);
                mThreadClient.start();
            }
        }
    };

    private Runnable mRunnable = new Runnable() {
        public void run()
        {
            String msgText =IPText.getText().toString();
            if(msgText.length()<=0)
            {  //Toast.makeText(mContext, "IP≤ªƒ‹Œ™ø’£°", Toast.LENGTH_SHORT).show();
                recvMessageClient = "IP不能为空！\n";//œ˚œ¢ªª––
                Message msg = new Message();
                msg.what = 1;
                mHandler.sendMessage(msg);
                return;
            }
            int start = msgText.indexOf(":");
            if( (start == -1) ||(start+1 >= msgText.length()) )
            {
                recvMessageClient = "IP地址不合法\n";//消息换行
                Message msg = new Message();
                msg.what = 1;
                mHandler.sendMessage(msg);
                return;
            }
            String sIP = msgText.substring(0, start);
            String sPort = msgText.substring(start+1);
            int port = Integer.parseInt(sPort);

            Log.d("gjz", "IP:" + sIP + ":" + port);

            try
            {
                //¡¨Ω”∑˛ŒÒ∆˜
                mSocketClient = new Socket(sIP, port);	//portnum
                //»°µ√ ‰»Î°¢ ‰≥ˆ¡˜
                mBufferedReaderClient=new BufferedReader(new InputStreamReader(mSocketClient.getInputStream(),"utf8"));
                //mBufferedReaderClient = new BufferedReader(new InputStreamReader(mSocketClient.getInputStream()));
                mPrintWriterClient = new PrintWriter(mSocketClient.getOutputStream(), true);
                //mInputStream=mSocketClient.getInputStream();
                recvMessageClient = "已经连接server!\n";//消息换行
                Message msg = new Message();
                msg.what = 1;
                mHandler.sendMessage(msg);
                //break;
            }
            catch (Exception e)
            {
                recvMessageClient = "连接IP异常:" + e.toString() + e.getMessage() + "\n";//消息换行
                Message msg = new Message();
                msg.what = 1;
                mHandler.sendMessage(msg);
                return;
            }

            char[] buffer = new char[256];
            int count = 0;
            while (isConnecting)
            {
                try
                {
                    //if ( (recvMessageClient = mBufferedReaderClient.readLine()) != null )
                    if((count = mBufferedReaderClient.read(buffer))>0)
                    {
                        recvMessageClient = getInfoBuff(buffer, count) + "\n";//œ˚œ¢ªª––


                        Message msg = new Message();
                        // msg.what = 1;
                        msg.what = 1;
                        mHandler.sendMessage(msg);
                    }
                }
                catch (Exception e)
                {
                    recvMessageClient = "接收异常:" + e.getMessage() + "\n";//消息换行
                    Message msg = new Message();
                    //msg.what = 0;
                    msg.what = 1;
                    mHandler.sendMessage(msg);
                }
            }
        }
    };

    Handler mHandler = new Handler()

    {
        @SuppressLint("HandlerLeak") public void handleMessage(Message msg)
        //public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            if(msg.what == 0)
            {

                recvText.append("Server: "+recvMessageServer);	// 刷新
            }
            else if(msg.what == 1)
            {
                recvText.append("服务器: "+recvMessageClient);	// 刷新
                //recvText.setText(recvMessageClient);

            }
        }
    };

    private String getInfoBuff(char[] buff, int count)
    {
        char[] temp = new char[count];
        for(int i=0; i<count; i++)
        {
            temp[i] = buff[i];
        }
        return new String(temp);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConnecting) {
            isConnecting = false;
            try {
                if(mSocketClient!=null)
                {
                    mSocketClient.close();
                    mSocketClient = null;

                    mPrintWriterClient.close();
                    mPrintWriterClient = null;
                    mBufferedReaderClient.close();
                    mBufferedReaderClient=null;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mThreadClient.interrupt();
        }
    }
}
