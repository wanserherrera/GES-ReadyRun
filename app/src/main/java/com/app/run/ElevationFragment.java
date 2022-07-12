package com.app.run;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.fragment.app.Fragment;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class ElevationFragment extends Fragment {

    GraphView graphView;
    private LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
    private static final double DISTANCE_TO_CALCULATE_IN_KM = 0.1;

    public ElevationFragment(){
        series.setColor(Color.BLACK);
        series.setBackgroundColor(Color.YELLOW);
        series.setThickness(7);
        series.setDrawBackground(true);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.elevation, container, false);
        graphView = rootView.findViewById(R.id.graph_elevation);
        graphView.onDataChanged(true,true);
        graphView.addSeries(series);
        graphView.setBackgroundColor(Color.TRANSPARENT);
        graphView.getGridLabelRenderer().setHorizontalAxisTitle("Distancia en kilómetros");
        graphView.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graphView.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);

        return rootView;
    }

    public void addPoint(Double distance, Double altitude){
        series.appendData(new DataPoint(distance, altitude), true, Integer.MAX_VALUE);
    }

    public void setPoints(ArrayList<PolyNode> polyNodes){
        resetPoints();
        double acuDist = 0;
        double acuAltitude = 0;
        int nodesCount = 0;
        for (int i = 0; i < polyNodes.size(); i++) {
            double distAnt = 0;
            if (i > 0)
                distAnt = polyNodes.get(i - 1).getDistance();
            acuDist += polyNodes.get(i).getDistance() - distAnt;
            acuAltitude += polyNodes.get(i).getAltitude();
            nodesCount += 1;
            if (acuDist >= DISTANCE_TO_CALCULATE_IN_KM) {
                series.appendData(new DataPoint(polyNodes.get(i).getDistance(), acuAltitude / nodesCount), true, Integer.MAX_VALUE);
                acuDist = 0;
                acuAltitude = 0;
                nodesCount = 0;
            }
        }
    }

    public void resetPoints(){
        series.resetData(new DataPoint[0]);
    }

}
