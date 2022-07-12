package com.app.run;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.LinkedHashMap;

public class RegisterAdapter extends BaseAdapter {

    private LinkedHashMap<String, DataPack> dataMap;
    private LayoutInflater inflater = null;
    String [] arrayKey;

    private static class ViewHolder {
        TextView txtDistance;
        TextView txtTime;
        TextView txtAvg;
        TextView txtDate;
        TextView txtDistanceTitle;
    }

    public RegisterAdapter(Context context, LinkedHashMap<String, DataPack> dataMap) {
        inflater = LayoutInflater.from(context);
        this.dataMap = dataMap;
        refreshKeys();
    }

    public Pair<String, LinkedHashMap<String, DataPack>> remove(int position) {
        String keyToRemove = arrayKey[position];
        dataMap.remove(keyToRemove);
        refreshKeys();
        return new Pair<>(keyToRemove, dataMap);
    }

    public void setDataMap(LinkedHashMap<String, DataPack> dataMap) {
        this.dataMap = dataMap;
        refreshKeys();
    }

    private void refreshKeys(){
        arrayKey = dataMap.keySet().toArray(new String[0]);
    }

    @Override
    public void notifyDataSetChanged(){
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dataMap.size();
    }

    @Override
    public DataPack getItem(int position) {
        return dataMap.get(arrayKey[position]);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null)
        {
            convertView = inflater.inflate(R.layout.historic_item, null);

            holder = new ViewHolder();

            convertView.setTag(holder);

        } else
            holder = (ViewHolder) convertView.getTag();

        holder.txtDistance = convertView.findViewById(R.id.txt_item_dist);
        holder.txtTime = convertView.findViewById(R.id.txt_item_time);
        holder.txtAvg = convertView.findViewById(R.id.txt_item_avg);
        holder.txtDate = convertView.findViewById(R.id.txt_item_date);
        holder.txtDistanceTitle = convertView.findViewById(R.id.txt_title_item_dist);

        if (dataMap.get(arrayKey[position]).getDistance() != null)
            holder.txtDistance.setText(String.format("%s km", dataMap.get(arrayKey[position]).getDistance()));
        if (dataMap.get(arrayKey[position]).getTime() != null)
            holder.txtTime.setText(DateUtils.formatElapsedTime(Long.parseLong(dataMap.get(arrayKey[position]).getTime())));
        if (dataMap.get(arrayKey[position]).getAverage() != null)
            holder.txtAvg.setText(String.format("%s km/h", dataMap.get(arrayKey[position]).getAverage()));
        if (dataMap.get(arrayKey[position]).getDate() != null)
            holder.txtDate.setText(dataMap.get(arrayKey[position]).getDate());
        if (dataMap.get(arrayKey[position]).getMode() != null) {
            switch (dataMap.get(arrayKey[position]).getMode()) {
                case "Pedestrismo": {
                    holder.txtDistanceTitle.setCompoundDrawablesWithIntrinsicBounds(null, null,
                            ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_mode_2), null);
                    break;
                }
                case "Ciclismo": {
                    holder.txtDistanceTitle.setCompoundDrawablesWithIntrinsicBounds(null, null,
                            ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_mode_1), null);
                    break;
                }
                case "Automovilismo": {
                    holder.txtDistanceTitle.setCompoundDrawablesWithIntrinsicBounds(null, null,
                            ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_mode_3), null);
                    break;
                }
                default:{
                    holder.txtDistanceTitle.setCompoundDrawablesWithIntrinsicBounds(null, null,
                            null, null);
                    break;
                }
            }
        }
        else
            holder.txtDistanceTitle.setCompoundDrawablesWithIntrinsicBounds(null, null,
                    null, null);

        return convertView;
    }
}
