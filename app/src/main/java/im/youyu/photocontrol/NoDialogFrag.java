package im.youyu.photocontrol;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.baidu.voicerecognition.android.Candidate;
import com.baidu.voicerecognition.android.VoiceRecognitionClient;
import com.baidu.voicerecognition.android.VoiceRecognitionConfig;

import java.util.List;

/**
 * Created by fujh on 16/5/25.
 */
public class NoDialogFrag extends Fragment implements View.OnClickListener{
    private static final String TAG = NoDialogFrag.class.getSimpleName();
    private static final String API_KEY="WIWjzWhg6mcklkZN9wuv8Vri";
    private static final String SECRET_KEY="1976f8bb480c03e227a94e72170e9ce4";

    /** 界面布局元素 **/
    private TextView Status, Result;
    private ProgressBar mVolumeBar;
    private Button BtnStart, BtnCancel;

    /** 语音识别Client **/
    private VoiceRecognitionClient mClient;

    /** 语音识别配置 **/
    private VoiceRecognitionConfig config;

    /** 语音识别回调接口  **/
    private VoiceRecognitionClient.VoiceClientStatusChangeListener mListener=new VoiceRecognitionClient.VoiceClientStatusChangeListener()
    {
        public void onClientStatusChange(int status, Object obj) {
            switch (status) {
                // 语音识别实际开始，这是真正开始识别的时间点，需在界面提示用户说话。
                case VoiceRecognitionClient.CLIENT_STATUS_START_RECORDING:
                    IsRecognition = true;
                    mVolumeBar.setVisibility(View.VISIBLE);
                    BtnCancel.setEnabled(true);
                    BtnStart.setText("说完");
                    Status.setText("当前状态:请说话");
                    mHandler.removeCallbacks(mUpdateVolume);
                    mHandler.postDelayed(mUpdateVolume, UPDATE_INTERVAL);
                    break;
                case VoiceRecognitionClient.CLIENT_STATUS_SPEECH_START: // 检测到语音起点
                    Status.setText("当前状态:说话中");
                    break;
                case VoiceRecognitionClient.CLIENT_STATUS_AUDIO_DATA:
                    //这里可以什么都不用作，简单地对传入的数据做下记录
                    break;
                // 已经检测到语音终点，等待网络返回
                case VoiceRecognitionClient.CLIENT_STATUS_SPEECH_END:
                    Status.setText("当前状态:正在识别....");
                    BtnCancel.setEnabled(false);
                    mVolumeBar.setVisibility(View.INVISIBLE);
                    break;
                // 语音识别完成，显示obj中的结果
                case VoiceRecognitionClient.CLIENT_STATUS_FINISH:
                    Status.setText(null);
                    UpdateRecognitionResult(obj);

                    //finished的obj为null
                    IsRecognition = false;
                    ReSetUI();
                    break;
                // 处理连续上屏
                case VoiceRecognitionClient.CLIENT_STATUS_UPDATE_RESULTS:
                    //UpdateRecognitionResult(obj);
                    break;
                // 用户取消
                case VoiceRecognitionClient.CLIENT_STATUS_USER_CANCELED:
                    Status.setText("当前状态:已取消");
                    IsRecognition = false;
                    ReSetUI();
                    break;
                default:
                    break;
            }

        }

        @Override
        public void onError(int errorType, int errorCode) {
            IsRecognition = false;
            Result.setText("出错:"+Integer.toHexString(errorCode));
            ReSetUI();
        }

        @Override
        public void onNetworkStatusChange(int status, Object obj)
        {
            // 这里不做任何操作不影响简单识别
        }
    };

    /** 语音识别类型定义 **/
    public static final int VOICE_TYPE_INPUT=0;
    public static final int VOICE_TYPE_SEARCH=1;

    /** 音量更新时间间隔   **/
    private static final int UPDATE_INTERVAL=200;

    /** 音量更新任务   **/
    private Runnable mUpdateVolume=new Runnable()
    {
        @Override
        public void run()
        {
            if (IsRecognition)
            {
                long vol = VoiceRecognitionClient.getInstance(getActivity())
                        .getCurrentDBLevelMeter();
                mVolumeBar.setProgress((int)vol);
                mHandler.removeCallbacks(mUpdateVolume);
                mHandler.postDelayed(mUpdateVolume, UPDATE_INTERVAL);
            }

        }
    };

    /** 主线程Handler */
    private Handler mHandler;

    /** 正在识别中 */
    private boolean IsRecognition = false;

    /** 当前语音识别类型  **/
    private int mType=VOICE_TYPE_INPUT;


    public static Fragment newInstance(){
        Fragment fm = new NoDialogFrag();
        return fm;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_nodialog, container, false);
        Status=(TextView)view.findViewById(R.id.Status);
        Result=(TextView)view.findViewById(R.id.Result);
        mVolumeBar=(ProgressBar)view.findViewById(R.id.VolumeProgressBar);
        BtnStart=(Button)view.findViewById(R.id.Start);
        BtnStart.setOnClickListener(this);
        BtnCancel=(Button)view.findViewById(R.id.Cancel);
        BtnCancel.setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mClient=VoiceRecognitionClient.getInstance(getActivity());
        //设置应用授权信息
        mClient.setTokenApis(API_KEY, SECRET_KEY);
        //初始化主线程
        mHandler=new Handler();
    }

    @Override
    public void onDestroy()
    {
        VoiceRecognitionClient.releaseInstance(); // 释放识别库
        super.onDestroy();
    }

    @Override
    public void onPause()
    {
        if (IsRecognition) {
            mClient.stopVoiceRecognition(); // 取消识别
        }
        super.onPause();
    }

    /*
     * 处理Click事件
     */
    @Override
    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.Start:
                if (IsRecognition) { // 用户说完
                    mClient.speakFinish();
                } else { // 用户重试，开始新一次语音识别
                    Result.setText(null);
                    // 需要开始新识别,首先设置参数
                    config = new VoiceRecognitionConfig();
                    if (mType == VOICE_TYPE_INPUT)
                    {
                        config.setSpeechMode(VoiceRecognitionConfig.SPEECHMODE_MULTIPLE_SENTENCE);
                    } else {
                        config.setSpeechMode(VoiceRecognitionConfig.SPEECHMODE_SINGLE_SENTENCE);

                    }
                    //开启语义解析
                    config.enableNLU();
                    //开启音量反馈
                    config.enableVoicePower(true);
                    config.enableBeginSoundEffect(R.raw.bdspeech_recognition_start); // 设置识别开始提示音
                    config.enableEndSoundEffect(R.raw.bdspeech_speech_end); // 设置识别结束提示音
                    config.setSampleRate(VoiceRecognitionConfig.SAMPLE_RATE_8K); //设置采样率
                    //使用默认的麦克风作为音频来源
                    config.setUseDefaultAudioSource(true);
                    // 下面发起识别
                    int code = VoiceRecognitionClient.getInstance(getActivity()).startVoiceRecognition(
                            mListener, config);
                    if (code == VoiceRecognitionClient.START_WORK_RESULT_WORKING)
                    { // 能够开始识别，改变界面
                        BtnStart.setEnabled(false);
                        BtnStart.setText("说完");
                        BtnCancel.setEnabled(true);
                    } else {
                        Result.setText("启动失败:"+ code);
                    }
                }
                break;
            case R.id.Cancel:
                mClient.stopVoiceRecognition();
                break;
        }
    }

    /*
     * 重置界面
     */
    private void ReSetUI()
    {
        BtnStart.setEnabled(true); // 可以开始重试
        BtnStart.setText("重试");
        BtnCancel.setEnabled(false); // 还没开始不能取消
    }

    /*
     *将识别结果显示到界面上
     */
    private void UpdateRecognitionResult(Object result) {
        if (result != null && result instanceof List) {
            @SuppressWarnings("rawtypes")
            List results = (List) result;
            if (results.size() > 0) {
                if (mType==VOICE_TYPE_SEARCH) {
                    Result.setText(results.get(0).toString());
                } else if (mType == VOICE_TYPE_INPUT) {
                    @SuppressWarnings("unchecked")
                    List<List<Candidate>> sentences = ((List<List<Candidate>>) result);
                    StringBuffer sb = new StringBuffer();
                    for (List<Candidate> candidates : sentences) {
                        if (candidates != null && candidates.size() > 0) {
                            sb.append(candidates.get(0).getWord());
                        }
                    }
                    ((MainActivity)getActivity()).cmd(sb.toString());
                    perdeal(sb.toString());
                }
            }
        }
    }

    private void perdeal(String resoutStr){
        String cmdStr = "";
        if (resoutStr.contains("前进")){
            cmdStr = "前进";
        }else if (resoutStr.contains("后退")) {
            cmdStr = "后退";
        }else if (resoutStr.contains("左转")) {
            cmdStr = "向左转";
        }else if (resoutStr.contains("右转")) {
            cmdStr = "向右转";
        }else if (resoutStr.contains("停止")) {
            cmdStr = "停止";
        }else if (resoutStr.contains("暂停")) {
            cmdStr = "暂停";
        }else if (resoutStr.contains("加速")) {
            cmdStr = "加速";
        }else if (resoutStr.contains("减速")) {
            cmdStr = "减速";
        }else if (resoutStr.contains("寻找线路")){
            cmdStr = "寻线";
        }else if (resoutStr.contains("避开障碍")){
            cmdStr = "避障";
        }
        Result.setText(cmdStr);
    }

}
