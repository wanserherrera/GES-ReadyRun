package com.app.run;

import android.os.Parcel;
import android.os.Parcelable;

public class PolyNode implements Parcelable {
    private double latitude;
    private double longitude;
    private double altitude;
    private double distance;

    public PolyNode(){};

    public PolyNode(double latitude, double longitude, double altitude, double distance){
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.distance = distance;
    }

    protected PolyNode(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
        altitude = in.readDouble();
        distance = in.readDouble();
    }

    public static final Creator<PolyNode> CREATOR = new Creator<PolyNode>() {
        @Override
        public PolyNode createFromParcel(Parcel in) {
            return new PolyNode(in);
        }

        @Override
        public PolyNode[] newArray(int size) {
            return new PolyNode[size];
        }
    };

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(getLatitude());
        dest.writeDouble(getLongitude());
        dest.writeDouble(getAltitude());
        dest.writeDouble(getDistance());
    }
}
