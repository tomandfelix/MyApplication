package com.tomandfelix.stapp2.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.gc.materialdesign.views.CheckBox;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;

public class SettingsView extends DrawerActivity {
    private ListView settingsList;
    private SettingsAdapter adapter;
    private Setting[] settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_settings);
        super.onCreate(savedInstanceState);

        settings = new Setting[4];
        int freq = DatabaseHelper.getInstance().getUploadFrequency() / 60000;
        settings[0] = new Setting("Upload frequency", freq + (freq == 1 ? " minute" : " minutes"));
        settings[1] = new Setting("Sensor", DatabaseHelper.getInstance().getSensor());
        settings[2] = new Setting("Upload over 3G", "If set, your phone will upload data to the server at the upload frequency", DatabaseHelper.getInstance().uploadOn3G(),
                new CheckBox.OnCheckListener() {
                    @Override
                    public void onCheck(boolean b) {
                        Log.d("Settings", "Upload3G=" + b);
                        DatabaseHelper.getInstance().setUploadOn3G(b);
                    }
                });
        settings[3] = new Setting("Notifications", null, DatabaseHelper.getInstance().getNotification(),
                new CheckBox.OnCheckListener() {
                    @Override
                    public void onCheck(boolean b) {
                        Log.d("Settings", "Notification=" + b);
                        DatabaseHelper.getInstance().setNotification(b);
                    }
                });

        settingsList = (ListView) findViewById(R.id.settings_list);
        adapter = new SettingsAdapter(this, R.layout.list_item_settings, settings);
        settingsList.setAdapter(adapter);
        settingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                if(position == 0) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(SettingsView.this);
                    alert.setMessage("Please enter a new value, in minutes").setTitle("Upload frequency");
                    final EditText input = new EditText(SettingsView.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    alert.setView(input);
                    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch(which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    int newUploadFreq = Integer.parseInt(input.getText().toString());
                                    if(newUploadFreq > 0) {
                                        DatabaseHelper.getInstance().setUploadFrequency(newUploadFreq * 60000);
                                        settings[0].subTitle = newUploadFreq + (newUploadFreq == 1 ? " minute" : " minutes");
                                        adapter.notifyDataSetChanged();
                                    }
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    break;
                            }
                        }
                    };
                    alert.setPositiveButton("CONFIRM", listener);
                    alert.setNegativeButton("CANCEL", listener);
                    alert.show();
                } else if(position == 1) {
                    Intent intent = new Intent(SettingsView.this, SensorSelection.class);
                    startActivityForResult(intent, 1);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("SettingsView", "reqCode=" + requestCode + ", resCode=" + resultCode);
        if(requestCode == 1) {
            String address = data.getExtras().getString("address");
            DatabaseHelper.getInstance().setSensor(address);
            settings[1].subTitle = address;
            adapter.notifyDataSetChanged();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class Setting {
        private String title;
        private String subTitle;
        private boolean checked;
        private CheckBox.OnCheckListener listener;

        private Setting(String title, String subTitle, boolean checked, CheckBox.OnCheckListener listener) {
            Log.d("Setting", "checked=" + checked);
            this.title = title;
            this.subTitle = subTitle;
            this.checked = checked;
            this.listener = listener;
        }

        public Setting(String title, String subTitle) {
            this(title, subTitle, false, null);
        }
    }

    private class SettingsAdapter extends ArrayAdapter<Setting> {
        private Setting[] data;
        private int resourceId;

        public SettingsAdapter(Context context, int resource, Setting[] objects) {
            super(context, resource, objects);
            this.data = objects;
            this.resourceId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(resourceId, parent, false);
            }

            Setting s = data[position];

            if(s != null) {
                ((TextView) convertView.findViewById(R.id.setting_title)).setText(s.title);
                if(s.subTitle != null) {
                    ((TextView) convertView.findViewById(R.id.settings_subtitle)).setText(s.subTitle);
                } else {
                    convertView.findViewById(R.id.settings_subtitle).setVisibility(View.GONE);
                }
                if(s.listener != null) {
                    CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.settings_checkBox);
                    checkBox.setVisibility(View.VISIBLE);
                    Log.d("Setting", position + " drawing=" + s.checked);
                    checkBox.setChecked(s.checked);
                    checkBox.setOncheckListener(s.listener);
                } else {
                    convertView.findViewById(R.id.settings_checkBox).setVisibility(View.INVISIBLE);
                }
            }
            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            return data[position].listener == null;
        }
    }
}
