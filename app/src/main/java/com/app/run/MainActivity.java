package com.app.run;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Stack;


/*
Actividad principal encargada del layout principal, manejo de los fragments y de interactuar con
el servicio encargado de los recorridos
 */

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private Receiver receiver;
    private LocationUpdatesService mService = null;

    // Tracks the bound state of the service.
    private boolean mBound = false;

    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (!toShow)
                mService.requestDataPack();
            if ((Utils.getUpdateState(getApplicationContext()) && !Utils.getPausedState(getApplicationContext()))) {
                tracing.setValue(true);
            } else {
                tracing.setValue(false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    private MapsFragment mapsFragment = null;
    private HistoricFragment historicFragment = null;
    private ConfigurationFragment configurationFragment = null;
    private CalendarFragment calendarFragment = null;
    private FragmentManager fm = getSupportFragmentManager();

    private TextView txtDistance;
    private TextView txtTime;
    private TextView txtAvg;
    private FloatingActionButton bPlay;
    private Button bStop;
    private BottomNavigationView navView;

    private ActiveVariable tracing;
    private boolean mapTracking = false;
    private boolean toShow = false;
    private String distance = "0";
    private long time = 0;
    private BigDecimal avg = BigDecimal.valueOf(0);
    private String userId = "";
    private String provider = "";
    private boolean showMarkers = false;

    private ArrayList<PolyNode> polyNodeArray = null;
    private Stack<Integer> stackMenu = new Stack<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Iniciliazaciones del usuario, variables, listeners, visibilidad y gps
        setUserProvider();
        inicialization();
        setListeners();
        setStopButtonVisibility();
        Utils.checkGPSState(this);

        // Define el fragment con que inicia
        if (getIntent().getBooleanExtra("showHistoricTab", false))
            // Caso que se abre desde el servicio al detener un recorrido
            navView.setSelectedItemId(R.id.menu_historic);
        else if (!checkPermissions()) {
            // Caso que no se cuenten con los permisos necesarios
            requestPermissions();
            navView.setSelectedItemId(R.id.menu_setting);
        }
        else {
            // Por defecto se abre el fragment del mapa
            navView.setSelectedItemId(R.id.menu_map);
        }
    }

    private void setUserProvider() {
        // Obtiene el usuario y el metodo de autenticacion desde la actividad de autenticacion
        // o desde default preferences
        if (getIntent().hasExtra(Utils.AUTH_PROVIDER) && getIntent().hasExtra(Utils.USER_ID)){
            provider = getIntent().getExtras().getString(Utils.AUTH_PROVIDER);
            userId = getIntent().getExtras().getString(Utils.USER_ID);
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(Utils.AUTH_PROVIDER, provider)
                    .putString(Utils.USER_ID, userId)
                    .apply();
        }else{
            provider = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(Utils.AUTH_PROVIDER, "");
            userId = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(Utils.USER_ID, "");
        }
    }

    private void inicialization() {
        // Inicializaciones de todos los fragments
        mapsFragment = new MapsFragment();
        mapsFragment.setActivity(this);
        mapsFragment.setContext(this);

        configurationFragment = new ConfigurationFragment();
        configurationFragment.setMainActivity(this);

        receiver = new Receiver();
        receiver.setMainActivity(this);

        historicFragment = new HistoricFragment();
        historicFragment.setMainActivity(this);

        calendarFragment = new CalendarFragment();

        FirebaseManager firebaseManager = FirebaseManager.getInstance();
        firebaseManager.setUserID(userId);
        firebaseManager.setHistoricFragment(historicFragment);
        firebaseManager.setCalendarFragment(calendarFragment);

        historicFragment.setFirebaseManager(firebaseManager);

        showMarkers = Utils.getMarkers(this);

        // Inicializaciones de los elementos del layout
        tracing = new ActiveVariable();
        txtDistance = findViewById(R.id.txt_dist);
        txtTime = findViewById(R.id.txt_time);
        txtAvg = findViewById(R.id.txt_avg);
        bPlay = findViewById(R.id.b_play_pause);
        bStop = findViewById(R.id.btn_stop);
        navView = findViewById(R.id.bottom_navigation);
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(getApplicationContext(), LocationUpdatesService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
        // Cada vez que resumo actualizo los cuadros de textos y me fijo si el gps sigue activo
        updateTextViews();
        Utils.checkGPSState(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        // Cada vez que presiono atras vuelvo al ultimo fragment abierto, guardado en la pila
        boolean frag = fm.popBackStackImmediate();
        if (frag) {
            stackMenu.pop();
            if (stackMenu.size() == 0)
                // En caso que no se tengan mas items en la pila cierro la app
                shutdown(false);
            else
                navView.setSelectedItemId(stackMenu.peek());
        }
    }

    private void setListeners(){
        // Define los listeners del menu de navegacion, de la variable que define si hay un
        // recorrido activo y de los botones

        navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                // Checkeo el item que eliga del menu y reemplazo el fragment segun el elegido
                int id = menuItem.getItemId();
                menuItem.setChecked(true);
                switch (id){
                    case R.id.menu_map: {
                        if (!(fm.findFragmentById(R.id.fragment_container) instanceof MapsFragment)) {
                            // Actualizo la pila de fragments pero sin que se acumule el mismo
                            // fragment muchas veces, por eso primero lo saco de la cola sin
                            // importar su posicion y luego lo agrego al final
                            stackMenu.removeElement(id);
                            stackMenu.push(id);
                            fm.beginTransaction()
                                    .replace(R.id.fragment_container, mapsFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                        break;
                    }
                    case R.id.menu_historic: {
                        if (!(fm.findFragmentById(R.id.fragment_container) instanceof HistoricFragment)) {
                            stackMenu.removeElement(id);
                            stackMenu.push(id);
                            fm.beginTransaction()
                                    .replace(R.id.fragment_container, historicFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                        break;
                    }
                    case R.id.menu_calendar: {
                        if (!(fm.findFragmentById(R.id.fragment_container) instanceof CalendarFragment)) {
                            stackMenu.removeElement(id);
                            stackMenu.push(id);
                            fm.beginTransaction()
                                    .replace(R.id.fragment_container, calendarFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                        break;
                    }
                    case R.id.menu_setting: {
                        if (!(fm.findFragmentById(R.id.fragment_container) instanceof ConfigurationFragment)) {
                            stackMenu.removeElement(id);
                            stackMenu.push(id);
                            fm.beginTransaction()
                                    .replace(R.id.fragment_container, configurationFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                        break;
                    }
                }
                return false;
            }
        });

        tracing.setListener(new ActiveVariable.ChangeListener() {
            @Override
            public void onChange() {
                if (tracing.getValue()){
                    // Si pasa a verdadera la actividad debe ponerse el boton con el logo de stop
                    setButtonImage(false);
                    if (!checkPermissions()) {
                        requestPermissions();
                    } else if(mBound){
                        // Si estaba pausado simplemente resumo el servicio
                        if (Utils.getPausedState(getApplicationContext()))
                            mService.resumeLocationUpdate();
                        // Si no estaba en pausa debo reiniciar las variables, detener el servicio
                        // y empezar uno nuevo
                        else if (!Utils.getUpdateState(getApplicationContext())) {
                            toShow = false;
                            distance = "0";
                            time = 0;
                            avg = BigDecimal.valueOf(0);
//                            elevationFragment.resetPoints();
                            stopService(new Intent(MainActivity.this, LocationUpdatesService.class));
                            mService.startLocationUpdate();
                        }
                    }
                }
                else if(mBound){
                    // Si pauso simplemente defino la imagen del boton a play y le aviso al servicio
                    setButtonImage(true);
                    if (!Utils.getPausedState(getApplicationContext()) && Utils.getUpdateState(getApplicationContext())){
                        mService.pauseLocationUpdate();
                    }
                }
            }
        });

        bPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cada vez que apreto el play/pause cambio el valor de la variable activa
                tracing.setValue(!tracing.getValue());
            }
        });
        bStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // El boton detener avisa al servicio sobre esto y carga el fragment de los registros
                if (mBound && Utils.getUpdateState(getApplicationContext())) {
                    mService.stopLocationUpdate(false);
                    navView.setSelectedItemId(R.id.menu_historic);
                }
            }
        });
    }

    private boolean checkPermissions() {
        return  PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Mostrar justificación de permisos para proporcionar contexto adicional.");
            Snackbar.make(
                    findViewById(R.id.main_activity),
                    "Permisos",
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("Ok", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Pidiendo permisos");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "El usuario ha cancelado la interacción");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService.requestLocationUpdates();
            } else {
                // Permission denied.
                tracing.setValue(Boolean.FALSE);
                Snackbar.make(
                        findViewById(R.id.main_activity),
                        "Permisos denegados",
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction("Opciones", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Actualizo los estados
        switch (key) {
            case Utils.UPDATE_STATE:
                tracing.setValue(sharedPreferences.getBoolean(Utils.UPDATE_STATE, false));
                break;
            case Utils.TRACKING:
                setMapTracking(sharedPreferences.getBoolean(Utils.TRACKING, false));
                break;
            case Utils.MARKERS:
                showMarkers = (sharedPreferences.getBoolean(Utils.MARKERS, false));
                if (toShow) {
                    // Si muestro los marcadores solamente los agrego, sino dibujo la polilinea
                    // anteriormente guardada (al pasar null no sobre-escribe las que estaban)
                    if (showMarkers)
                        mapsFragment.addMarkers();
                    else {
                        mapsFragment.updatePolyline(null, null);
                    }
                }
                break;
        }

        if (key.equals(Utils.UPDATE_STATE) || key.equals(Utils.PAUSED_UPDATE))
            setStopButtonVisibility();
    }

    private void setButtonImage(boolean state){
        // Cambio la imagen del boton segun el estado de actividad
        if (state)
            bPlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, getTheme()));
        else
            bPlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, getTheme()));
    }

    private void setStopButtonVisibility(){
        // Seteo la visibilidad del boton para detener
        if (!Utils.getPausedState(this) && !Utils.getUpdateState(this))
            bStop.setVisibility(View.INVISIBLE);
        else
            bStop.setVisibility(View.VISIBLE);
    }

    public void setMapTracking(boolean mapTracking){
        this.mapTracking = mapTracking;
    }

    public boolean getTracing(){
        return tracing.getValue();
    }

    public void showRegister(DataPack dataPack){
        // Cuando termino un recorrido o lo cargo desde el registro a uno ya hecho debo cambiar
        // al fragment del mapa, actualizar las variables pertinentes y dibujar la polilinea
        toShow = true;
        mapsFragment.setStartLocation(false);
        navView.setSelectedItemId(R.id.menu_map);
        tracing.setValue(false);
        updateDataPack(dataPack);
        if (showMarkers)
            mapsFragment.addMarkers();
        if (dataPack.getPolyline() != null) {
            // Tomo el primer punto del recorrido y hago que el mapa se mueva a ese punto
            Location auxLocation = new Location("");
            auxLocation.setLatitude(polyNodeArray.get(0).getLatitude());
            auxLocation.setLongitude(polyNodeArray.get(0).getLongitude());
            if (mapTracking)
                updateLocation(auxLocation);
            else{
                // En caso de no haber seguimiento lo activo por esa actualizacion y vuelvo a
                // desactivarlo
                mapTracking = true;
                updateLocation(auxLocation);
                mapTracking = false;
            }
        }
    }

    public void updateLocation(Location loc){
        // Actualizo la localizacion y muevo la camara en caso de estar el seguimiento activo
        mapsFragment.setLat(loc.getLatitude());
        mapsFragment.setLon(loc.getLongitude());
        if (mapTracking)
            mapsFragment.moveCamera();
    }

    public void updateTime(long time){
        this.time = time;
    }

    public void updateDistance(String distance){
        this.distance = distance;
    }

    public void updatePolyline(ArrayList<PolyNode> polyNodeArray, ArrayList<Integer> pauseNodes){
        // Actualizo la polilinea y los nodos que pertenencen al inicio o fin de un tramo en caso de
        // utilizarse la pausa
        this.polyNodeArray = polyNodeArray;
        mapsFragment.updatePolyline(this.polyNodeArray, pauseNodes);
    }

    public void updateDataPack(DataPack dataPack){
        // Actualizo todas las variables y los cuadros de texto
        updateTime(Long.parseLong(dataPack.getTime()));
        updateDistance(dataPack.getDistance());
        if (dataPack.getPolyline() != null){
            if (dataPack.getPolyline().size() > 0)
                updatePolyline(dataPack.getPolyline(), dataPack.getPause());
        }
        updateAverage();
        updateTextViews();
    }

    private void updateAverage(){
        // Recalculo el promedio, tengo en cuentas las posibles excepciones numericas al dividir
        try{
            if (time != 0 && !BigDecimal.valueOf(Float.parseFloat(distance)).equals(BigDecimal.valueOf(0)))
                avg = BigDecimal.valueOf(Float.parseFloat(distance)).multiply(BigDecimal.valueOf(3600)).divide(BigDecimal.valueOf(time), 1, RoundingMode.HALF_DOWN );
        }catch (NumberFormatException nfe) {
            System.out.println("NumberFormatException: " + nfe.getMessage());
        }catch (ArithmeticException ae) {
            System.out.println("ArithmeticException: " + ae.getMessage());
        }
    }

    private void updateTextViews(){
        // Actualizo todos los cuadros de textos
        txtTime.setText(DateUtils.formatElapsedTime(time));
        txtDistance.setText(distance);
        txtAvg.setText(String.valueOf(avg));
    }

    public void shutdown(boolean logout){
        // En caso de deslogueo reseteo el usuario y metodo de autenticacion. Despues vuelvo a la
        // actividad de autenticacion
        if (logout){
            FirebaseAuth.getInstance().signOut();
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(Utils.AUTH_PROVIDER, "")
                    .putString(Utils.USER_ID, "")
                    .apply();
            this.finishAffinity();
            startActivity(new Intent(this, AuthenticationActivity.class));
        // Si no deslogueo directamente cierro la app
        }else
            this.finishAffinity();
    }
}
