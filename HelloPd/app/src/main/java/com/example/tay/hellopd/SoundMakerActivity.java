package com.example.tay.hellopd;

import android.annotation.TargetApi;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;

public class SoundMakerActivity extends AppCompatActivity implements SensorEventListener {

    private static final int BASE_MIDI_NUMBER = 80;

    private PdUiDispatcher dispatcher;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private String instrument;
    private TextView instrumentInput;

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

    // Control variables for generating piano sounds
    private float oldVal;
    private float newVal;

    private int MIDI_TO_PLAY = 0;

    // Control variables for generating drum sounds.
    private static final float SHAKE_ENTER_THRESHOLD = 15;
    private static final float SHAKE_LEAVE_THRESHOLD = 12;

    private float oldZVal;
    private float newZVal;

    private String getInstrument() {
        Intent intent = getIntent();

        instrumentInput = (TextView) findViewById(R.id.instrumentInput);
        String instrumentName = intent.getStringExtra("Instrument Name");
        instrumentInput.setText(instrumentName);
        return instrumentName;
    }

    private void initSensor() {
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        uiValueX = (TextView) findViewById(R.id.accXValue);
        uiValueY = (TextView) findViewById(R.id.accYValue);
        uiValueZ = (TextView) findViewById(R.id.accZValue);

        uiValueXAdj = (TextView) findViewById(R.id.adjustedX);
        uiValueYAdj = (TextView) findViewById(R.id.adjustedY);

        oldVal = 0.0f;
        newVal = 0.0f;

        oldZVal = 0.0f;
        newZVal = 0.0f;
    }

    private void initPD() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        PdAudio.initAudio(sampleRate, 0, 2, 8, true);

        dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(dispatcher);
    }

    private void loadPdPatch(String instrument) throws IOException {
        File dir = getFilesDir();
        File pdPatch = null;

        switch (instrument) {
            case "Bell":
                // TODO: load bell patch.
                IoUtils.extractZipResource(getResources().openRawResource(R.raw.simplepatch), dir, true);
                pdPatch = new File(dir, "simplepatch.pd");
                break;

            case "Drum":
                // TODO: load drum patch.
                IoUtils.extractZipResource(getResources().openRawResource(R.raw.snaredrum), dir, true);
                pdPatch = new File(dir, "snaredrum.pd");
                break;

            case "Piano":
                // TODO: load piano patch.
                IoUtils.extractZipResource(getResources().openRawResource(R.raw.piano), dir, true);
                pdPatch = new File(dir, "piano.pd");
                Log.i("Instrument loaded", "Piano");
                break;

            default:
                // do nothing.
        }

        PdBase.openPatch(pdPatch.getAbsolutePath());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_maker);

        try {
            instrument = getInstrument();
            Log.i("Instrument to load:", instrument);
            initSensor();

            initPD();
            loadPdPatch(instrument);
        } catch (IOException e) {
            finish();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (instrument.equals("Piano")) {
                triggerNote(MIDI_TO_PLAY);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            PdBase.release();
            finish();
        }
        return super.onKeyDown(keyCode, event);
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
        instrument = "";
        mSensorManager.unregisterListener(this);
        PdAudio.stopAudio();
    }

    private void triggerNote(int n) {
        if (instrument.equals("Bell")) {
            // TODO: send relevant data to the patch.
        } else if (instrument.equals("Drum")) {
            PdBase.sendBang("trigger");
        } else if (instrument.equals("Piano")) {
            PdBase.sendFloat("accelX", n);
            PdBase.sendBang("trigger");
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        oldVal = newVal;        // updates previous value for Piano
        oldZVal = newZVal;      // updates previous value for Drum

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                float valueX = event.values[0];
                float valueY = event.values[1];
                float valueZ = event.values[2];

                double valueXAdjusted = Math.ceil(valueX / 2.0);
                double valueYAdjusted = Math.ceil(valueY);

                uiValueX.setText(String.valueOf(valueX));
                uiValueY.setText(String.valueOf(valueY));
                uiValueZ.setText(String.valueOf(valueZ));

                uiValueXAdj.setText(String.valueOf(valueXAdjusted));
                uiValueYAdj.setText(String.valueOf(valueYAdjusted));

                Log.i("triggerNote", String.valueOf(valueXAdjusted));
                newVal = (float) valueXAdjusted;    // gets latest value for Piano
                newZVal = valueZ;

                if (instrument.equals("Piano")) {
                    /*
                     * This is to ensure that unintentional tilting of the phone
                     * will not trigger the Piano note.
                     */
                    if (oldVal == newVal) {
                        // Set the MIDI number to be passed to the patch.
                        // The playing of the sound should trigger only when user touch the screen.
                        MIDI_TO_PLAY = ((int) valueXAdjusted + BASE_MIDI_NUMBER);
                    }
                } else if (instrument.equals("Drum")) {
                    if (oldZVal > SHAKE_ENTER_THRESHOLD && newZVal < SHAKE_ENTER_THRESHOLD) {
                        triggerNote(0);
                    }
                }

                break;

            default:
                // do nothing
        }
    }
}
