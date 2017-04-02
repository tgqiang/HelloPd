package com.example.tay.hellopd;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private RadioGroup radioGroup;
    private Button proceedButton;

    private int SELECTED_INSTRUMENT = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radioGroup = (RadioGroup) findViewById(R.id.instrumentSelections);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                SELECTED_INSTRUMENT = i;
            }
        });

        proceedButton = (Button) findViewById(R.id.proceedButton);
        proceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                proceedToApp(view);
            }
        });
    }

    public void proceedToApp(View view) {
        if (SELECTED_INSTRUMENT == -1) {
            Toast.makeText(getApplicationContext(), "Please make sure you have selected an instrument to emulate.", Toast.LENGTH_SHORT)
                    .show();
        } else {
            Intent intent = new Intent(this, SoundMakerActivity.class);

            switch (SELECTED_INSTRUMENT) {
                case R.id.bellSelect:
                    intent.putExtra("Instrument Name", "Bell");
                    break;

                case R.id.drumSelect:
                    intent.putExtra("Instrument Name", "Drum");
                    break;

                case R.id.pianoSelect:
                    intent.putExtra("Instrument Name", "Piano");
                    break;

                default:
                    // do nothing
            }

            startActivity(intent);
        }
    }
}
