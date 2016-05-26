package im.youyu.photocontrol;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.baidu.voicerecognition.android.ui.BaiduASRDigitalDialog;
import com.baidu.voicerecognition.android.ui.DialogRecognitionListener;

import java.util.ArrayList;

/**
 * Created by fujh on 16/5/25.
 */
public class DialogFrag extends Fragment{
    private static final String TAG = DialogFrag.class.getSimpleName();
    private static final String API_KEY="WIWjzWhg6mcklkZN9wuv8Vri";
    private static final String SECRET_KEY="1976f8bb480c03e227a94e72170e9ce4";

    private Button btn_voice;
    private BaiduASRDigitalDialog mDialog = null;

    public static Fragment newInstance(){
        Fragment fm = new DialogFrag();
        return fm;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_dialog, container, false);
        btn_voice = (Button)view.findViewById(R.id.btn_voice);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mDialog == null) {
            if (mDialog != null) {
                mDialog.dismiss();
            }
            Bundle params = new Bundle();
            params.putString(BaiduASRDigitalDialog.PARAM_API_KEY, API_KEY);
            params.putString(BaiduASRDigitalDialog.PARAM_SECRET_KEY, SECRET_KEY);
            params.putInt(BaiduASRDigitalDialog.PARAM_DIALOG_THEME, BaiduASRDigitalDialog.THEME_BLUE_LIGHTBG);
            mDialog = new BaiduASRDigitalDialog(getActivity(), params);
            //设置百度语音识别回调接口
            DialogRecognitionListener mDialogListener = new DialogRecognitionListener() {
                @Override
                public void onResults(Bundle mResults) {
                    ArrayList<String> rs = mResults != null ? mResults.getStringArrayList(RESULTS_RECOGNITION) : null;
                    if (rs != null && rs.size() > 0) {
                        ((MainActivity)getActivity()).cmd(rs.get(0));
                    }
                }
            };

            mDialog.setDialogRecognitionListener(mDialogListener);
        }

        mDialog.setSpeechMode(BaiduASRDigitalDialog.SPEECH_MODE_INPUT);
        mDialog.getParams().putBoolean(BaiduASRDigitalDialog.PARAM_NLU_ENABLE, false);

        btn_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.show();
            }
        });
    }
}
