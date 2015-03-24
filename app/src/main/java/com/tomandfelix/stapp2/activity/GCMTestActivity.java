package com.tomandfelix.stapp2.activity;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.persistency.GCMMessage;
import com.tomandfelix.stapp2.persistency.ServerHelper;

public class GCMTestActivity extends DrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_gcm_test);
        super.onCreate(savedInstanceState);
    }

    public void toSend(View v) {
        EditText idField = (EditText) findViewById(R.id.internet_send_id);
        String[] idStrings = idField.getText().toString().split(",");
        int[] ids =  new int[idStrings.length];
        for(int i = 0; i < idStrings.length; i++) {
            ids[i] = Integer.parseInt(idStrings[i]);
        }
        ServerHelper.getInstance().sendMessage(new GCMMessage(ids, -1, GCMMessage.TEST_TOAST, 0, ((EditText) findViewById(R.id.internet_send_data)).getText().toString()), new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if(!volleyError.getMessage().equals("none")) {
                    Log.e("GCMTestActivity", volleyError.getMessage());
                }
            }
        });
    }
}

