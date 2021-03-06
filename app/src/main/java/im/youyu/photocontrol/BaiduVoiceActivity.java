package im.youyu.photocontrol;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
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
public class BaiduVoiceActivity extends Activity implements View.OnClickListener{

    /** 应用授权信息 **/
    private static final String API_KEY="WIWjzWhg6mcklkZN9wuv8Vri";
    private static final String SECRET_KEY="1976f8bb480c03e227a94e72170e9ce4";

    /** 界面布局元素 **/
    private TextView Status,Result;
    private ProgressBar mVolumeBar;
    private Button BtnStart,BtnCancel;

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
                    IsRecognition = false;
                    ReSetUI();
                    break;
                // 处理连续上屏
                case VoiceRecognitionClient.CLIENT_STATUS_UPDATE_RESULTS:
                    UpdateRecognitionResult(obj);
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
            Result.setText("出错: 0x%1$s"+Integer.toHexString(errorCode));
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
                long vol = VoiceRecognitionClient.getInstance(BaiduVoiceActivity.this)
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
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_voice);
        InitView();
        //获取mClent
        mClient=VoiceRecognitionClient.getInstance(this);
        //设置应用授权信息
        mClient.setTokenApis(API_KEY, SECRET_KEY);
        //初始化主线程
        mHandler=new Handler();
    }

    /*
     * 界面初始化
     */
    private void InitView()
    {
        Status=(TextView)findViewById(R.id.Status);
        Result=(TextView)findViewById(R.id.Result);
        mVolumeBar=(ProgressBar)findViewById(R.id.VolumeProgressBar);
        BtnStart=(Button)findViewById(R.id.Start);
        BtnStart.setOnClickListener(this);
        BtnCancel=(Button)findViewById(R.id.Cancel);
        BtnCancel.setOnClickListener(this);

    }
    @Override
    protected void onDestroy()
    {
        VoiceRecognitionClient.releaseInstance(); // 释放识别库
        super.onDestroy();
    }

    @Override
    protected void onPause()
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
                    int code = VoiceRecognitionClient.getInstance(this).startVoiceRecognition(
                            mListener, config);
                    if (code == VoiceRecognitionClient.START_WORK_RESULT_WORKING)
                    { // 能够开始识别，改变界面
                        BtnStart.setEnabled(false);
                        BtnStart.setText("说完");
                        BtnCancel.setEnabled(true);
                    } else {
                        Result.setText("启动失败: 0x%1$s"+code);
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
                    Result.setText(sb.toString());
                }
            }
        }
    }
}
