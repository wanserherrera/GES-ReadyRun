package com.app.run;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.abs;


public class LocationUpdatesService extends Service implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String PACKAGE_NAME =
            "package com.tonulab.velostracker";

    private static final String TAG = LocationUpdatesService.class.getSimpleName();

    // Nombre del canal para las notificaciones
    private static final String CHANNEL_ID = "channel_01";
    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";
    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    static final String EXTRA_DATAPACK = PACKAGE_NAME + ".datapack";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = ".started_from_notification";
    private static final String NOTIFICATION_STOP = PACKAGE_NAME + ".notification_stop";
    private static final String NOTIFICATION_PAUSE = PACKAGE_NAME + ".notification_pause";
    private static final String NOTIFICATION_RESUME = PACKAGE_NAME + ".notification_resume";

    private final IBinder mBinder = new LocalBinder();


    // Intervalo en el cual se realizan actualizaciones
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    // Intervalo mas rapido de actualizacion, estas nuncas seran mas rapidas que este valor
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    // Minimo desplazamineto para que se realice una actualizacion de ubicacion
    private static final float MINIMUN_DISPLACEMENT_IN_METERS = 1.0f;
    // Minima distancia en metros recorridos para que actualice la polilinea y cuente como metro recorrido
    private static float MINIMUN_DISTANCE_TO_REFRESH;
    private static final float MINIMUN_DISTANCE_TO_PAUSE_TIME = 3.0f;
    private static final long TIME_TO_PAUSE_IN_MILLISECONDS = 10000;
    private static final long TIME_WITHOUT_NEW_LOCATIONS = 30000;

    private static Float SPEED_LIMIT;
    private static Float ACCELERATION_LIMIT;

    // Idetificador para la notificacion del servicio cuando esta en primer plano
    private static final int NOTIFICATION_ID = 12345678;

    /**
     Se utiliza para verificar si la actividad vinculada realmente se ha ido y no se
     ha desvinculado como parte de un cambio de orientación. Creamos una notificación
     de servicio en primer plano solo si se lleva a cabo la primera.
     */
    private static boolean mChangingConfiguration = false;

    private static NotificationManager mNotificationManager;
    private static LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private static LocationCallback mLocationCallback;
    private static Handler mServiceHandler;

    private FirebaseManager firebaseManager = null;

    private static Location mLocation;
    private static Location antLocation;

    private static ArrayList<PolyNode> polyNodeArray;
    private static ArrayList<Integer> pauseNodes;
    private static BigDecimal realDistance = BigDecimal.valueOf(0);
    private static Double roundedDistance = 0D;
    private static String startDate;

    private static Timer timer;
    private static Long startTime = 0L;
    private static Long currentTime = 0L;
    private static Long accumulatedTime = 0L;

    private static boolean firstTime = false;

    private static Long startLeisurelyTime;
    private static Location locationPaused = null;
    private static boolean leisurelyTime = false;
    private static boolean beganLeisurelyTime = false;
    private static Queue<Location> lastValidsLocations = new LinkedList<>();
    private static final int numberOfLocationToSave = 5;

    private static Float lastValidSpeed = null;
    private static LinkedList<Long> timeOfLastTwoLocations = new LinkedList<>();

    public LocationUpdatesService() {}

    @Override
    public void onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firebaseManager = FirebaseManager.getInstance();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.runrunner);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Servicio comenzado ID: " + startId);
        if (intent != null){
            String action = intent.getAction();
            if (action != null && !action.isEmpty()){
                switch (action) {
                    case NOTIFICATION_STOP:
                        stopLocationUpdate(intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false));
                        break;
                    case NOTIFICATION_PAUSE:
                        pauseLocationUpdate();
                        break;
                    case NOTIFICATION_RESUME:
                        resumeLocationUpdate();
                        break;
                }
            }
        }

        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }


    public void pauseLocationUpdate(){
        Utils.setPausedState(this, true);
        leisurelyTime = false;
        beganLeisurelyTime = false;
        if (timer != null)
            timer.cancel();
        accumulatedTime = currentTime;
        if (!pauseNodes.contains(polyNodeArray.size() - 1))
            pauseNodes.add(polyNodeArray.size() - 1);
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    public void stopLocationUpdate(boolean startedFromNotification){
        Utils.setPausedState(this, false);
        leisurelyTime = false;
        beganLeisurelyTime = false;
        if (timer != null)
            timer.cancel();
        if (startedFromNotification) {
            Intent intentAct = new Intent(this, MainActivity.class);
            intentAct.putExtra("showHistoricTab", true);
            intentAct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intentAct);
            Intent intentCloseNoticationPanel = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            getApplicationContext().sendBroadcast(intentCloseNoticationPanel);
        }
        removeLocationUpdates();
        writeOnDatabase();
    }

    public void resumeLocationUpdate(){
        Utils.setPausedState(this, false);
        leisurelyTime = false;
        beganLeisurelyTime = false;
        startTime = System.currentTimeMillis();
        roundedDistance = realDistance.divide(BigDecimal.valueOf(1), 2, RoundingMode.HALF_EVEN).doubleValue();
        currentTime = accumulatedTime;
        lastValidsLocations = new LinkedList<>();
        timeOfLastTwoLocations = new LinkedList<>();
        antLocation = null;
        requestLocationUpdates();
    }

    public void startLocationUpdate(){
        Utils.setPausedState(this, false);
        firstTime = true;
        startTime = System.currentTimeMillis();
        accumulatedTime = 0L;
        currentTime = 0L;
        realDistance = BigDecimal.valueOf(0);
        roundedDistance = 0D;
        polyNodeArray = new ArrayList<>();
        pauseNodes = new ArrayList<>();
        DateFormat DFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        startDate = DFormat.format(new Date());
        lastValidsLocations = new LinkedList<>();
        timeOfLastTwoLocations = new LinkedList<>();
        antLocation = null;
        leisurelyTime = false;
        beganLeisurelyTime = false;

        requestLocationUpdates();
        startService(new Intent(getApplicationContext(), LocationUpdatesService.class));
        MINIMUN_DISTANCE_TO_REFRESH = Utils.getMtsRefresh();
        SPEED_LIMIT = Utils.getSpeedLimit();
        ACCELERATION_LIMIT = Utils.getAccelerationLimit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "La actividad se ha enlazado al servicio por primera vez");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "La actividad se ha enlazado nuevamente al servicio");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "La actividad se ha desenlazado del servicio");

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && Utils.getUpdateState(this)) {
            Log.i(TAG, "Ejecutando servicio en primer plano");
            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        if (timer != null)
            timer.cancel();
        timer = new Timer();
        timer.scheduleAtFixedRate(new timeUpdateTask(), 0, 1000);

        Log.i(TAG, "Requiriendo actualizaciones de ubicación");
        Utils.setUpdateState(this, true);
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            Utils.setUpdateState(this, false);
            Log.e(TAG, "Permiso de ubicación perdido. No se pudieron solicitar actualizaciones" + unlikely);
        }
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Finalizando actualizaciones de ubicación");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            Utils.setUpdateState(this, false);
        } catch (SecurityException unlikely) {
            Utils.setUpdateState(this, true);
            Log.e(TAG, "Permiso de ubicación perdido. No se pudieron solicitar actualizaciones " + unlikely);
        }
    }

    public void requestDataPack(){
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_DATAPACK, new DataPack(String.valueOf(roundedDistance), String.valueOf(currentTime),
                startDate, null, Utils.getMode(this), pauseNodes, polyNodeArray));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        if (!Utils.getUpdateState(this))
            stopSelf();
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, LocationUpdatesService.class);

        CharSequence text = Utils.getNotificationText(BigDecimal.valueOf(roundedDistance), currentTime);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent stopIntent = PendingIntent.getService(this, 0,
                intent.setAction(NOTIFICATION_STOP).putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true),
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pauseIntent = PendingIntent.getService(this, 0,
                intent.setAction(NOTIFICATION_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent resumeIntent = PendingIntent.getService(this, 0,
                intent.setAction(NOTIFICATION_RESUME),
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(activityPendingIntent)
                .addAction(R.drawable.ic_play, "Reanudar",
                        resumeIntent)
                .addAction(R.drawable.ic_pause, "Pausar",
                        pauseIntent)
                .addAction(R.drawable.ic_stop, "Detener",
                        stopIntent)
                .setContentText(text)
                .setContentTitle(Utils.getNotificationTitle(this))
                .setOnlyAlertOnce(true)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setSmallIcon(R.drawable.ic_notification)
                .setTicker(text)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis());

        return builder.build();
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Error al obtener ubicacion");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Permiso de ubicación perdido." + unlikely);
        }
    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "Nueva ubicación: " + Utils.getLocationText(location));

        // Notify anyone listening for broadcasts about the new location.
        Intent intent = new Intent(ACTION_BROADCAST);

        if (lastValidsLocations.size() > 0 && timeOfLastTwoLocations.size() > 0){
            Float lessLastValidSpeed = lastValidSpeed;
            long lastTimeDifference = (System.currentTimeMillis() - timeOfLastTwoLocations.getLast())/1000;
            lastValidSpeed = distanceBetweenLocations(location, lastValidsLocations.element())/ (lastTimeDifference);
            if (lastValidsLocations.size() > 1){
                long lessLastTimeDifference = (timeOfLastTwoLocations.getLast() - timeOfLastTwoLocations.getFirst())/1000;
                float acceleration = abs(lessLastValidSpeed - lastValidSpeed) / (lastTimeDifference - lessLastTimeDifference);
                if (acceleration < ACCELERATION_LIMIT && lastValidSpeed < SPEED_LIMIT){
                    if (lastValidsLocations.size() >= numberOfLocationToSave) {
                        lastValidsLocations.poll();
                    }
                    lastValidsLocations.add(location);
                    timeOfLastTwoLocations.set(0, timeOfLastTwoLocations.getLast());
                    timeOfLastTwoLocations.set(1, System.currentTimeMillis());
                    manageLeisurelyTime();
                }
            }
            else if (lastValidSpeed < SPEED_LIMIT){
                lastValidsLocations.add(location);
                timeOfLastTwoLocations.add(System.currentTimeMillis());
                manageLeisurelyTime();
            }
        } else {
            lastValidsLocations.add(location);
            timeOfLastTwoLocations.add(System.currentTimeMillis());
            manageLeisurelyTime();
        }

        refreshDistance(location);
        if (firstTime){
            polyNodeArray.add(new PolyNode(mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getAltitude(), roundedDistance));
            firstTime = false;
        }
        intent.putExtra(EXTRA_LOCATION, mLocation);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        // Update notification content if running as a foreground service.
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
        }
    }

    /**
     * Sets the location request parameters.
     */
    @SuppressLint("RestrictedApi")
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(MINIMUN_DISPLACEMENT_IN_METERS);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Utils.LEISURELY_TIME)) {
            if (!Utils.getLeisurelyTime(this)){
                if (leisurelyTime) {
                    beganLeisurelyTime = false;
                    resumeTime();
                }
            }
            else{
                beganLeisurelyTime = false;
                leisurelyTime = false;
            }
        }
        else if (key.equals(Utils.MODE)) {
            MINIMUN_DISTANCE_TO_REFRESH = Utils.getMtsRefresh();
            SPEED_LIMIT = Utils.getSpeedLimit();
            ACCELERATION_LIMIT = Utils.getAccelerationLimit();
        }
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        LocationUpdatesService getService() {
            return LocationUpdatesService.this;
        }
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    private void pauseTime(){
        Log.i(TAG, "Tiempo pausado");
        accumulatedTime = currentTime;
        leisurelyTime = true;
        if (timer != null)
            timer.cancel();
    }

    private void resumeTime(){
        Log.i(TAG, "Tiempo resumido");
        leisurelyTime = false;
        startTime = System.currentTimeMillis();
        roundedDistance = realDistance.divide(BigDecimal.valueOf(1), 2, RoundingMode.HALF_EVEN).doubleValue();
        currentTime = accumulatedTime;
        timer = new Timer();
        timer.scheduleAtFixedRate(new timeUpdateTask(), 0, 1000);
    }

    private void refreshDistance(Location loc){
        Float distanceRes = 0F;
        if (antLocation != null){
            distanceRes = distanceBetweenLocations(mLocation, antLocation);
        }
        else{
            antLocation = mLocation;
        }
        Location auxLocation = mLocation;
        mLocation = loc;
        if (distanceRes > MINIMUN_DISTANCE_TO_REFRESH && !leisurelyTime) {
            realDistance = BigDecimal.valueOf(distanceRes).divide(BigDecimal.valueOf(1000),16, RoundingMode.HALF_EVEN).add(realDistance);
            roundedDistance = realDistance.divide(BigDecimal.valueOf(1), 2, RoundingMode.HALF_EVEN).doubleValue();
            Log.i(TAG, "Distancia actualizada: " + realDistance);
            polyNodeArray.add(new PolyNode(mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getAltitude(), roundedDistance));
            antLocation = auxLocation;
        }
    }

    private Float distanceBetweenLocations(Location loc1, Location loc2){
        float[] distanceRes = new float[3];
        distanceRes[0] = 0;
        Location.distanceBetween(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude(), distanceRes);
        return distanceRes[0];
    }

    private void manageLeisurelyTime(){
        if (Utils.getLeisurelyTime(this) && !Utils.getPausedState(this)){
            Float distance = calculateCloseness((LinkedList<Location>) lastValidsLocations);
            if (distance != null){
                if ((distance < MINIMUN_DISTANCE_TO_PAUSE_TIME || locationPaused == mLocation) &&
                        (System.currentTimeMillis() - timeOfLastTwoLocations.getLast() < TIME_WITHOUT_NEW_LOCATIONS)){
                    if (!beganLeisurelyTime){
                        startLeisurelyTime = System.currentTimeMillis();
                        beganLeisurelyTime = true;
                    }
                    else if (System.currentTimeMillis() - startLeisurelyTime > TIME_TO_PAUSE_IN_MILLISECONDS && !leisurelyTime) {
                        pauseTime();
                    }
                } else{
                    locationPaused = mLocation;
                    beganLeisurelyTime = false;
                    if (leisurelyTime)
                        resumeTime();
                }
            }
        }
    }

    private Float calculateCloseness(LinkedList<Location> locations) {
        double accumLat = 0D;
        double accumLon = 0D;
        Iterator<Location> it = locations.iterator();
        if (locations.size() != 0) {
            while (it.hasNext()) {
                Location locAux = it.next();
                if (locAux != null){
                    accumLat += locAux.getLatitude();
                    accumLon += locAux.getLongitude();
                }
            }
            float centerLat = (float) (accumLat / locations.size());
            float centerLon = (float) (accumLon / locations.size());
            Location centerLoc = new Location("");
            centerLoc.setLatitude(centerLat);
            centerLoc.setLongitude(centerLon);

            Float accumDistance = 0F;
            it = locations.iterator();
            while (it.hasNext()) {
                Location locAux = it.next();
                if (locAux != null)
                    accumDistance += distanceBetweenLocations(centerLoc, locAux);
            }
            return accumDistance / (float) locations.size();
        }
        else return null;
    }

    private void writeOnDatabase() {
        if (startDate.equals(""))
            startDate = "Sin fecha";
        BigDecimal avg = BigDecimal.valueOf(0);
        try{
            if (currentTime != 0 && !realDistance.equals(BigDecimal.valueOf(0)))
                avg = realDistance.multiply(BigDecimal.valueOf(3600)).divide(BigDecimal.valueOf(currentTime), 1, RoundingMode.HALF_DOWN);
        }catch (NumberFormatException nfe) {
            System.out.println("NumberFormatException: " + nfe.getMessage());
        }catch (ArithmeticException ae) {
            System.out.println("ArithmeticException: " + ae.getMessage());
        }
        DataPack reg = new DataPack(roundedDistance.toString(), String.valueOf(currentTime), startDate,
                String.valueOf(avg), Utils.getMode(this), pauseNodes, polyNodeArray);
        firebaseManager.writeOnFirebase(reg);
    }

    private class timeUpdateTask extends TimerTask
    {
        public void run()
        {
            if (!Utils.checkGPSState(LocationUpdatesService.this)) {
                pauseLocationUpdate();
                Utils.setPausedState(LocationUpdatesService.this, true);
            }

            manageLeisurelyTime();
            if (leisurelyTime) {
                accumulatedTime -= TIME_TO_PAUSE_IN_MILLISECONDS / 1000;
                currentTime -= TIME_TO_PAUSE_IN_MILLISECONDS / 1000;
            }
            else {
                if (startTime != 0)
                    currentTime = accumulatedTime + (System.currentTimeMillis() - startTime) / 1000;
                else
                    currentTime = 0L;
            }


            // Notify anyone listening for broadcasts about the new location.
            Intent intent = new Intent(ACTION_BROADCAST);
            intent.putExtra(EXTRA_DATAPACK, new DataPack(String.valueOf(roundedDistance), String.valueOf(currentTime), startDate,
                    null, Utils.getMode(getApplicationContext()), pauseNodes, polyNodeArray));
            LocalBroadcastManager.getInstance(LocationUpdatesService.this).sendBroadcast(intent);

            // Update notification content if running as a foreground service.
            if (serviceIsRunningInForeground(LocationUpdatesService.this)) {
                mNotificationManager.notify(NOTIFICATION_ID, getNotification());
            }
        }
    }
}