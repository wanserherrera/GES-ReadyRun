package com.app.run;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Context mContext;
    private Activity mActivity;

    static float DISTANCE_BETWEEN_MARKERS = 1f;
    //Latitud y longitud de donde se situa la camara
    private double lat;
    private double lon;
    private CameraPosition lastViewedLocation = null;
    FusedLocationProviderClient fusedLocationClient = null;

    private boolean startLocation = true;
    ArrayList<PolyNode> polyNodeArray = new ArrayList<>();
    ArrayList<Integer> pauseNodes = new ArrayList<>();

    private BitmapDrawable bitmapdraw;
    private View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        view = inflater.inflate(R.layout.maps, container, false);
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        this.lat = 0;
        this.lon = 0;
        bitmapdraw = (BitmapDrawable) ResourcesCompat.getDrawable(mContext.getResources(), R.mipmap.ic_marker, null);

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            mMap.setMapType(Utils.getMapType(getContext()));
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                        Utils.setMapType(getContext(), GoogleMap.MAP_TYPE_HYBRID);
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    }
                    else{
                        Utils.setMapType(getContext(), GoogleMap.MAP_TYPE_NORMAL);
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    }

                }
            });
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            getLastLocation();

            if (lastViewedLocation != null && startLocation)
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(lastViewedLocation));
        }
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public void setActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        lastViewedLocation = mMap.getCameraPosition();
    }

    //Setters, getters y demas utilidades
    public void setLat(double l) {
        this.lat = l;
    }

    public void setLon(double l) {
        this.lon = l;
    }

    public boolean getMapState() {
        return mMap != null;
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(mActivity, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            if (startLocation && lastViewedLocation == null) {
                                setLat(location.getLatitude());
                                setLon(location.getLongitude());
                                moveCamera();
                            } else
                                startLocation = true;
                        } else {
                            final Runnable r = new Runnable() {
                                public void run() {
                                    getLastLocation();
                                }
                            };
                            Handler handler = new Handler();
                            handler.postDelayed(r, 100);
                        }
                    }
                });
    }

    public void setStartLocation(boolean startLocation){
        this.startLocation = startLocation;
    }

    public void updatePolyline(ArrayList<PolyNode> PolyNodeArray, ArrayList<Integer> PauseNodes){
        if (PolyNodeArray != null)
            polyNodeArray = PolyNodeArray;
        if (PauseNodes != null)
            pauseNodes = PauseNodes;

        if (getMapState()){
            clearMap();
            if (pauseNodes.size() != 0){
                int init;
                int end;
                for (int j = 0; j < pauseNodes.size(); j++) {
                    PolylineOptions polyline = new PolylineOptions().width(15).color(Color.RED);
                    if (j == 0)
                        init = 0;
                    else
                        init = pauseNodes.get(j - 1);

                    if (j == pauseNodes.size() - 1)
                        end = polyNodeArray.size();
                    else
                        end = pauseNodes.get(j);

                    for (int i = init; i < end; i++) {
                        polyline.add(new LatLng(polyNodeArray.get(i).getLatitude(), polyNodeArray.get(i).getLongitude()) );
                    }
                    mMap.addPolyline(polyline);
                }
            }
            else{
                PolylineOptions polyline = new PolylineOptions().width(15).color(Color.RED);
                for (int i = 0; i < polyNodeArray.size(); i++) {
                    polyline.add(new LatLng(polyNodeArray.get(i).getLatitude(), polyNodeArray.get(i).getLongitude()) );
                }
                mMap.addPolyline(polyline);
            }
        }

    }

    public void addMarkers() {
        float presentGoalDistance = DISTANCE_BETWEEN_MARKERS;
        Bitmap smallMarker = Bitmap.createScaledBitmap(bitmapdraw.getBitmap(), 70, 70, false);
        for (int i = 0; i < polyNodeArray.size(); i++) {
            if (polyNodeArray.get(i).getDistance() >= presentGoalDistance) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(polyNodeArray.get(i).getLatitude(), polyNodeArray.get(i).getLongitude()))
                        .title((int) presentGoalDistance + " KM")
                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
                presentGoalDistance += DISTANCE_BETWEEN_MARKERS;
            }
        }

    }

    public void clearMap(){
        mMap.clear();
    }

    public void moveCamera() {
        LatLng position = new LatLng(lat, lon);
        if (getMapState()){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 16));
        }
    }

}
