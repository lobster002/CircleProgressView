package com.guangyu.ptcrile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private CircleProgressView circleProgressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.edit);
        circleProgressView = (CircleProgressView) findViewById(R.id.circle);
        circleProgressView.setCenterText("第一圈");
        circleProgressView.setOnDrawCircleListener(new OnDrawCircleListener() {
            @Override
            public void callback() {
                circleProgressView.setCenterText(String.valueOf(new Random().nextInt(100000) + 10000));
            }
        });

        findViewById(R.id.btn).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);

    }

    EditText editText;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        circleProgressView.stopAnim();
    }

    private long cyrrentProgess = 0;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn:
                String text = editText.getText().toString().trim();
                if (TextUtils.isEmpty(text)) {
                    return;
                }
                long value = Long.valueOf(text);
                circleProgressView.setCurrentProgress(value);
                break;
            case R.id.btn2:
                if (circleProgressView.isAnimStopped()) {
                    circleProgressView.startAnim(cyrrentProgess);
                    ((Button) v).setText("stop");
                } else {
                    cyrrentProgess = circleProgressView.stopAnim();
                    ((Button) v).setText("start");
                }
                break;
        }
    }
}
