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
    private CircleProgressView c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.edit);
        c = (CircleProgressView) findViewById(R.id.circle);
        c.setOnDrawCircleListener(new OnDrawCircleListener() {
            @Override
            public void callback() {
                Toast.makeText(MainActivity.this, "一圈", Toast.LENGTH_SHORT).show();
                c.changeNeedStr(String.valueOf(new Random().nextInt(100000) + 10000));
            }
        });

        findViewById(R.id.btn).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);

    }

    EditText editText;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn:
                String text = editText.getText().toString().trim();
                if (TextUtils.isEmpty(text)) {
                    return;
                }
                long value = Long.valueOf(text);
                c.setCurrentProgress(value);
                break;
            case R.id.btn2:
                if (c.isAnimStopped()) {
                    c.startAnim();
                    ((Button) v).setText("stop");
                } else {
                    c.stopAnim();
                    ((Button) v).setText("start");
                }
                break;
        }
    }
}
