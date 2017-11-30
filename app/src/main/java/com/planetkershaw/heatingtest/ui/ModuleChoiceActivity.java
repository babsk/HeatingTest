package com.planetkershaw.heatingtest.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.planetkershaw.heatingtest.R;
import com.planetkershaw.heatingtest.zwayservice.TimeService;

public class ModuleChoiceActivity extends AppCompatActivity {

    private Button heatingBtn;
    private Button lightingBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_choice);

        heatingBtn = (Button)findViewById(R.id.heating);
        lightingBtn = (Button)findViewById(R.id.lighting);

        heatingBtn.setOnClickListener (new View.OnClickListener() {
            public void onClick(View v) {
                Context context = v.getContext();
                Intent i = new Intent("com.planetkershaw.ROOMLIST");
                context.startActivity(i);
            }
        });

        lightingBtn.setOnClickListener (new View.OnClickListener() {
            public void onClick(View v) {
                Context context = v.getContext();
                Intent i = new Intent("com.planetkershaw.LIGHTLIST");
                context.startActivity(i);
            }
        });

        // start the service which retrieves the main data set
        startService(new Intent(this, TimeService.class));
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        // TODO:
        stopService(new Intent(this, TimeService.class));
    }
}
