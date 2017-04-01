package com.example.tay.hellopd;

import android.annotation.TargetApi;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int BASE_MIDI_NUMBER = 69;

    private PdUiDispatcher dispatcher;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    /*
     * UI elements to pass sensor readings to.
     */
    // For Accelerometer sensor
    private TextView uiValueX;
    private TextView uiValueY;
    private TextView uiValueZ;

    // For display normalized sensor readings.
    private TextView uiValueXAdj;
    private TextView uiValueYAdj;
    private TextView uiValueZAdj;

    private float oldVal;
    private float newVal;

    private void initPD() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        PdAudio.initAudio(sampleRate, 0, 2, 8, true);

        dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(dispatcher);
    }

    private void initGUI() {
        Switch toggleSwitch = (Switch) findViewById(R.id.soundToggleSwitch);
        /*
        toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                float val = (b) ? 1.0f : 0.0f;
                PdBase.sendFloat("soundToggle", val);
            }
        });
        */
    }

    private void loadPdPatch() throws IOException {
        File dir = getFilesDir();
        IoUtils.extractZipResource(getResources().openRawResource(R.raw.simplepatch), dir, true);
        File pdPatch = new File(dir, "simplepatch.pd");
        PdBase.openPatch(pdPatch.getAbsolutePath());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSensor();

        try {
            initPD();
            loadPdPatch();
        } catch (IOException e) {
            finish();
        }

        initGUI();
    }

    private void initSensor() {
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        uiValueX = (TextView) findViewById(R.id.accXValue);
        uiValueY = (TextView) findViewById(R.id.accYValue);
        uiValueZ = (TextView) findViewById(R.id.accZValue);

        uiValueXAdj = (TextView) findViewById(R.id.adjustedX);
        uiValueYAdj = (TextView) findViewById(R.id.adjustedY);
        uiValueZAdj = (TextView) findViewById(R.id.adjustedZ);

        oldVal = 0.0f;
        newVal = 0.0f;
    }

    @Override @TargetApi(19)
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, 100000, SensorManager.SENSOR_DELAY_NORMAL);
        PdAudio.startAudio(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        PdAudio.stopAudio();
    }

    private void triggerNote(int n) {
        PdBase.sendFloat("accelX", n);
        PdBase.sendBang("trigger");
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        oldVal = newVal;

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                float valueX = event.values[0];
                float valueY = event.values[1];
                float valueZ = event.values[2];

                double valueXAdjusted = Math.ceil(valueX);
                double valueYAdjusted = Math.ceil(valueY);
                float valueZAdjusted = valueZ;

                uiValueX.setText(String.valueOf(valueX));
                uiValueY.setText(String.valueOf(valueY));
                uiValueZ.setText(String.valueOf(valueZ));

                uiValueXAdj.setText(String.valueOf(valueXAdjusted));
                uiValueYAdj.setText(String.valueOf(valueYAdjusted));
                uiValueZAdj.setText(String.valueOf(valueZAdjusted));

                Log.i("triggerNote", String.valueOf(valueXAdjusted));
                newVal = (float) valueXAdjusted;
                if (oldVal != newVal) {
                    triggerNote(((int) valueXAdjusted + BASE_MIDI_NUMBER));
                }

                break;

            default:

        }
    }
}
