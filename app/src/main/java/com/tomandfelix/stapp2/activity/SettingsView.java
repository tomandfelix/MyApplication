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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

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

        settings = new Setting[6];
        int freq = DatabaseHelper.getInstance().getUploadFrequency() / 60000;
        settings[0] = new Setting("Upload frequency", freq + (freq == 1 ? " minute" : " minutes"));
        settings[1] = new Setting("Sensor", DatabaseHelper.getInstance().getSensor());
        settings[2] = new Setting("Account settings", DatabaseHelper.getInstance().getOwner().getUsername());
        settings[3] = new Setting("Mobile data", "Allow communications over mobile data", DatabaseHelper.getInstance().uploadOn3G(),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        DatabaseHelper.getInstance().setUploadOn3G(isChecked);
                    }
                });
        settings[4] = new Setting("Notifications", null, DatabaseHelper.getInstance().getNotification(),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        DatabaseHelper.getInstance().setNotification(isChecked);
                    }
                });
        settings[5] = new Setting("Logout", null);

        settingsList = (ListView) findViewById(R.id.settings_list);
        adapter = new SettingsAdapter(this, R.layout.list_item_settings, settings);
        settingsList.setAdapter(adapter);
        settingsList.setOnItemClickListener(new SettingsListener());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case 1:
                String address = data.getExtras().getString("address");
                DatabaseHelper.getInstance().setSensor(address);
                settings[1].subTitle = address;
                adapter.notifyDataSetChanged();
                break;
            case 2:
                settings[2].subTitle = app.getProfile().getUsername();
                adapter.notifyDataSetChanged();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class Setting {
        private String title;
        private String subTitle;
        private boolean checked;
        private CompoundButton.OnCheckedChangeListener listener;

        private Setting(String title, String subTitle, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
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

            final Setting s = data[position];

            if(s != null) {
                ((TextView) convertView.findViewById(R.id.setting_title)).setText(s.title);
                TextView subTitle = (TextView) convertView.findViewById(R.id.settings_subtitle);
                if(s.subTitle != null && !s.subTitle.equals("")) {
                    subTitle.setText(s.subTitle);
                    subTitle.setVisibility(View.VISIBLE);
                } else {
                    subTitle.setVisibility(View.GONE);
                }
                if(s.listener != null) {
                    CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.settings_checkBox);
                    checkBox.setVisibility(View.VISIBLE);
                    checkBox.setChecked(s.checked);
                    checkBox.setOnCheckedChangeListener(s.listener);
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

    private class SettingsListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch(position) {
                case 0:
                    AlertDialog.Builder alert = new AlertDialog.Builder(SettingsView.this);
                    alert.setMessage("Please enter a new value, in minutes").setTitle("Upload frequency");
                    final EditText input = new EditText(SettingsView.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    alert.setView(input);
                    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    int newUploadFreq = Integer.parseInt(input.getText().toString());
                                    if (newUploadFreq > 0) {
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
                    break;
                case 1:
                    Intent sensorIntent = new Intent(SettingsView.this, SensorSelection.class);
                    startActivityForResult(sensorIntent, 1);
                    break;
                case 2:
                    Intent accountIntent = new Intent(SettingsView.this, AccountSettings.class);
                    startActivityForResult(accountIntent, 2);
                    break;
                case 5:
                    DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    DatabaseHelper.getInstance().setToken("");
                                    index = INITIAL;
                                    Intent intent = new Intent(SettingsView.this, FragmentViewer.class);
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.enter_bottom, R.anim.leave_top);
                                    finish();
                                    break;
                            }
                        }
                    };
                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(SettingsView.this);
                    alertDialog.setMessage("Are you sure you want to log out?");
                    alertDialog.setPositiveButton("Logout", dialogListener);
                    alertDialog.setNegativeButton("Cancel", dialogListener);
                    alertDialog.show();
                    break;
            }
        }
    }
}
