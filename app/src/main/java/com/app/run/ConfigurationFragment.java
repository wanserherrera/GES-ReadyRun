package com.app.run;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;

public class ConfigurationFragment extends Fragment implements
        SharedPreferences.OnSharedPreferenceChangeListener{

    private ArrayList<String> modes = new ArrayList<>(Arrays.asList(Utils.MODES.PEDESTRISMO.toString(), Utils.MODES.CICLISMO.toString(), Utils.MODES.AUTOMOVILISMO.toString()));
    private String[] modesString = {Utils.MODES.PEDESTRISMO.toString(), Utils.MODES.CICLISMO.toString(), Utils.MODES.AUTOMOVILISMO.toString()};

    private MainActivity mainActivity;
    private Context context;

    SwitchCompat swt_follow;
    SwitchCompat swt_pause;
    SwitchCompat swt_marker;
    AppCompatImageButton btn_mode;
    AppCompatImageButton btn_logout;

    public void setMainActivity(MainActivity mainActivity){this.mainActivity = mainActivity;}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.configuration, container, false);
        swt_follow = rootView.findViewById(R.id.switch_follow);
        swt_pause = rootView.findViewById(R.id.switch_pause);
        swt_marker = rootView.findViewById(R.id.switch_marker);
        btn_mode = rootView.findViewById(R.id.btn_mode);
        btn_logout = rootView.findViewById(R.id.btn_logout);

        context = getContext();
        swt_follow.setChecked(Utils.getTracking(getContext()));
        swt_follow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Utils.setTracking(context, isChecked);
            }
        });

        swt_pause.setChecked(Utils.getLeisurelyTime(getContext()));
        swt_pause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Utils.setLeisurelyTime(context, isChecked);
            }
        });

        swt_marker.setChecked(Utils.getMarkers(getContext()));
        swt_marker.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Utils.setMarkers(context, isChecked);
            }
        });

        btn_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Seleccione el tipo de actividad")
                    .setSingleChoiceItems(modesString, modes.indexOf(Utils.getMode(context)), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            Utils.setMode(context, modes.get(which));
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    dialog.cancel();
                                }
                            }, 400);

                        }
                    })
                    .setCancelable(false);
                //Creating dialog box
                AlertDialog dialog  = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        });

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.shutdown(true);
            }
        });
        return rootView;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Utils.TRACKING)){
            swt_follow.setChecked(Utils.getTracking(context));
        }
    }
}
