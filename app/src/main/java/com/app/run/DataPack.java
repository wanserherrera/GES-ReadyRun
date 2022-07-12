package com.app.run;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class DataPack implements Parcelable {

    public String distance;
    public String time;
    public String date;
    public String average;
    public String mode;
    public ArrayList<Integer> pause;
    public ArrayList<PolyNode> polyline;

    public DataPack(){}

    public DataPack(String distance, String time, String date, String average, String mode, ArrayList<Integer> pause, ArrayList<PolyNode> polyline){
        this.distance = distance;
        this.time = time;
        this.date = date;
        this.average = average;
        this.mode = mode;
        this.pause = pause;
        this.polyline = polyline;
    }

    protected DataPack(Parcel in) {
        distance = in.readString();
        time = in.readString();
        date = in.readString();
        average = in.readString();
        mode = in.readString();
        in.readList(pause, Integer.class.getClassLoader());
        polyline = in.createTypedArrayList(PolyNode.CREATOR);
    }

    public static final Creator<DataPack> CREATOR = new Creator<DataPack>() {
        @Override
        public DataPack createFromParcel(Parcel in) {
            return new DataPack(in);
        }

        @Override
        public DataPack[] newArray(int size) {
            return new DataPack[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(distance);
        dest.writeString(time);
        dest.writeString(date);
        dest.writeString(average);
        dest.writeString(mode);
        dest.writeList(pause);
        dest.writeTypedList(polyline);
    }

    public String getDistance(){ return distance; }

    public String getTime(){ return time; }

    public String getDate(){ return date; }

    public String getAverage(){ return average; }

    public String getMode() { return mode; }

    public ArrayList<PolyNode> getPolyline(){ return polyline; }

    public ArrayList<Integer> getPause() { return pause;}

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setAverage(String average) {
        this.average = average;
    }

    public void setMode(String mode) { this.mode = mode; }

    public void setPause(ArrayList<Integer> pause) { this.pause = pause; }

    public void setPolyline(ArrayList<PolyNode> polyline) {
        this.polyline = polyline;
    }


}
