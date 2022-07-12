package com.app.run;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;


public class CalendarFragment extends Fragment {
    MaterialCalendarView calendar;
    private String TAG = CalendarFragment.class.getSimpleName();
    Vector<DataPack> dataPackVector = new Vector<>();
    TextView txt_total_dist;
    TextView txt_total_avg;
    TextView txt_month_dist;
    TextView txt_month_avg;
    TextView txt_week_dist;
    TextView txt_week_avg;
    TextView txt_day_dist;
    TextView txt_day_avg;
    CalendarDay selectedDate = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.calendar, container, false);
        calendar = rootView.findViewById(R.id.calendarView);
        txt_total_dist = rootView.findViewById(R.id.txt_cld_total_dist);
        txt_total_avg = rootView.findViewById(R.id.txt_cld_total_prom);
        txt_month_dist = rootView.findViewById(R.id.txt_cld_month_dist);
        txt_month_avg = rootView.findViewById(R.id.txt_cld_month_prom);
        txt_week_dist = rootView.findViewById(R.id.txt_cld_week_dist);
        txt_week_avg = rootView.findViewById(R.id.txt_cld_week_prom);
        txt_day_dist = rootView.findViewById(R.id.txt_cld_day_dist);
        txt_day_avg = rootView.findViewById(R.id.txt_cld_day_prom);

        if(selectedDate == null) {
            selectedDate = CalendarDay.today();
        }

        if (dataPackVector.size() > 0)
            refreshAll(selectedDate);

        calendar.setDateSelected(selectedDate, true);
        calendar.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (selected) {
                    refreshDayAndWeek(date.getYear(), date.getMonth(), date.getDay());
                    selectedDate = date;
                }
            }
        });
        calendar.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                refreshMonth(date.getYear(), date.getMonth());
            }
        });
        return rootView;
    }

    public void setRegisters(Vector<DataPack> dataPacks){
        dataPackVector = dataPacks;
        if (calendar != null){
            refreshAll(selectedDate);
        }
    }

    private void refreshDayAndWeek(int year, int month, int dayOfMonth){
        double acuDistDay = 0D;
        double acuTimeDay = 0D;
        int cantDay = 0;
        double acuDistWeek = 0D;
        double acuTimeWeek = 0D;
        int cantWeek = 0;
        for(int i = 0; i < dataPackVector.size(); i++) {
            ArrayList<Integer> auxDate = decomposeDate(dataPackVector.get(i).getDate());
            if(inSameWeek(auxDate.get(2), auxDate.get(1), auxDate.get(0), year, month, dayOfMonth)){
                acuDistWeek += Double.parseDouble(dataPackVector.get(i).distance);
                acuTimeWeek += Double.parseDouble(dataPackVector.get(i).time);
                cantWeek += 1;

                if(auxDate.get(0).equals(dayOfMonth) && auxDate.get(1).equals(month) && auxDate.get(2).equals(year)){
                    acuDistDay += Double.parseDouble(dataPackVector.get(i).distance);
                    acuTimeDay += Double.parseDouble(dataPackVector.get(i).time);
                    cantDay += 1;
                }
            }
        }

        txt_day_dist.setText(trimValue(acuDistDay));
        if (cantDay != 0)
            txt_day_avg.setText(String.valueOf(calculateAverage(acuDistDay, acuTimeDay)));
        else
            txt_day_avg.setText(String.valueOf(0).concat(".00"));

        txt_week_dist.setText(trimValue(acuDistWeek));
        if (cantWeek != 0)
            txt_week_avg.setText(String.valueOf(calculateAverage(acuDistWeek, acuTimeWeek)));
        else
            txt_week_avg.setText(String.valueOf(0).concat(".00"));
    }

    private void refreshMonth(int year, int month){
        double acuDist = 0D;
        double acuTime = 0D;
        int cant = 0;
        for(int i = 0; i < dataPackVector.size(); i++) {
            ArrayList<Integer> auxDate = decomposeDate(dataPackVector.get(i).getDate());
            if(auxDate.get(1).equals(month) && auxDate.get(2).equals(year)){
                acuDist += Double.parseDouble(dataPackVector.get(i).distance);
                acuTime += Double.parseDouble(dataPackVector.get(i).time);
                cant += 1;
            }
        }
        txt_month_dist.setText(trimValue(acuDist));
        if (cant != 0)
            txt_month_avg.setText(String.valueOf(calculateAverage(acuDist, acuTime)));
        else
            txt_month_avg.setText(String.valueOf(0).concat(".00"));
    }

    private void refreshTotal(){
        double acuDist = 0D;
        double acuTime = 0D;
        int cant = 0;
        for(int i = 0; i < dataPackVector.size(); i++) {
            acuDist += Double.parseDouble(dataPackVector.get(i).distance);
            acuTime += Double.parseDouble(dataPackVector.get(i).time);
            cant += 1;
        }
        txt_total_dist.setText(trimValue(acuDist));
        if (cant != 0)
            txt_total_avg.setText(String.valueOf(calculateAverage(acuDist, acuTime)));
        else
            txt_total_avg.setText(String.valueOf(0).concat(".00"));
    }

    private ArrayList<Integer> decomposeDate(String date) {
        int firstSlash = date.indexOf('/');
        int secondSlash = firstSlash + 1 + date.substring(firstSlash + 1).indexOf('/');
        ArrayList<Integer> dayMonthYear = new ArrayList<>();
        dayMonthYear.add(Integer.valueOf(date.substring(0, firstSlash)));
        dayMonthYear.add(Integer.valueOf(date.substring(firstSlash + 1, secondSlash)));
        dayMonthYear.add(Integer.valueOf("20" + date.substring(secondSlash + 1, secondSlash + 3)));
        return dayMonthYear;
    }

    private String trimValue(Double value){
        String stringValue = String.valueOf(value);
        if (String.valueOf(value).contains(".")){
            int pointPosition = stringValue.indexOf(".");
            if (pointPosition + 3 < stringValue.length())
                return stringValue.substring(0, pointPosition + 3);
            else
                return stringValue.substring(0, pointPosition + 2).concat("0");
        }
        else return stringValue.concat(".00");
    }

    private void refreshAll(CalendarDay date){
        refreshDayAndWeek(date.getYear(), date.getMonth(), date.getDay());
        refreshMonth(date.getYear(), date.getMonth());
        refreshTotal();
    }

    private String composeDate(int year, int month, int dayOfMonth){
        return dayOfMonth+"/"+month+"/"+String.valueOf(year).substring(2);
    }

    private Double calculateAverage(Double distance, Double time){
        BigDecimal auxDistance = BigDecimal.valueOf(distance);
        BigDecimal avg = auxDistance.multiply(BigDecimal.valueOf(3600)).divide(BigDecimal.valueOf(time), 1, RoundingMode.HALF_DOWN);
        return avg.doubleValue();
    }

    public boolean inSameWeek(int year1, int month1, int dayOfMonth1, int year2, int month2, int dayOfMonth2) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year2);
        c.set(Calendar.MONTH, month2 - 1);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth2);

        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        Date monday = c.getTime();
        monday = new Date(monday.getTime()-(24*60*60*1000));
        Date nextMonday= new Date(monday.getTime()+8*24*60*60*1000);
        DateFormat DFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        ArrayList<Integer> arrayMonday = decomposeDate(DFormat.format(monday));
        ArrayList<Integer> arrayNextMonday = decomposeDate(DFormat.format(nextMonday));
        CalendarDay cldDayMonday = CalendarDay.from(arrayMonday.get(2), arrayMonday.get(1), arrayMonday.get(0));
        CalendarDay cldDayNextMonday = CalendarDay.from(arrayNextMonday.get(2), arrayNextMonday.get(1), arrayNextMonday.get(0));

        return CalendarDay.from(year1, month1, dayOfMonth1).isAfter(cldDayMonday) &&  CalendarDay.from(year1, month1, dayOfMonth1).isBefore(cldDayNextMonday);
    }
}
