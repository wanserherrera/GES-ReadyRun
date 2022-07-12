package com.app.run;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.LinkedHashMap;
import java.util.Vector;

public final class FirebaseManager {

    private static FirebaseManager firebaseManager = null;

    private HistoricFragment historicFragment;
    private CalendarFragment calendarFragment;
    private DatabaseReference mDatabase;
    private LinkedHashMap<String, DataPack> registers = new LinkedHashMap<>();
    private Vector<DataPack> rawDataPack = new Vector<>();
    private String TAG = FirebaseManager.class.getSimpleName();
    private long nroReg = 0;
    private int contRegRead = 0;
    private String userID = null;

    public void setHistoricFragment(HistoricFragment historicFragment){
        this.historicFragment = historicFragment;
    }

    public void setCalendarFragment(CalendarFragment calendarFragment){
        this.calendarFragment = calendarFragment;
    }

    public void setUserID(String userID){
        this.userID = userID;
        readFromFirebase();
    }

    private FirebaseManager(){
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public static FirebaseManager getInstance(){
        if (firebaseManager == null)
            firebaseManager = new FirebaseManager();
        return firebaseManager;
    }

    public  void writeOnFirebase(DataPack reg){
        if (!checkUserId()){
            mDatabase.child(userID).child("Recorridos").push().setValue(reg, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error != null)
                        Log.e(TAG, "Error al guardar en la base de datos", error.toException());
                    else
                        Log.i(TAG, "Registro guardado satisfactoriamente");
                }
            });
        }
        else
            Log.e(TAG, "Problema con el id de usuario al escribir en la base de datos");
    }

    public void readFromFirebase() {
        if (!checkUserId()) {
            mDatabase.child(userID).child("Recorridos").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    registers = new LinkedHashMap<>();
                    rawDataPack = new Vector<>();
                    contRegRead = 0;
                    nroReg = snapshot.getChildrenCount();
                    if (nroReg == 0)
                        historicFragment.setRegisters(registers);
                    Log.i(TAG, nroReg + " registros leídos");

                    for (final DataSnapshot auxSnapshot : snapshot.getChildren()) {
                        mDatabase.child(userID).child("Recorridos").child(auxSnapshot.getKey()).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                DataPack data = auxSnapshot.getValue(DataPack.class);
                                String key = snapshot.getKey();
                                contRegRead += 1;
                                int auxSize = registers.size();
                                registers.put(key, data);
                                // En caso de que se lea concurrentemente registers no agrega repetidos por las key, por lo que si este no agrega
                                // tampoco debe hacerlos rawDataPack que solo guarda datos
                                if (auxSize != registers.size())
                                    rawDataPack.add(data);
                                if (contRegRead == nroReg) {
                                    historicFragment.setRegisters(registers);
                                    calendarFragment.setRegisters(rawDataPack);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } else
            Log.e(TAG, "Problema con el id de usuario al leer en la base de datos");
    }

    public void removeFromFirebase(String key){
        mDatabase.child(userID).child("Recorridos").child(key).removeValue();
        Log.i(TAG, "Registro eliminado");
    }

    private void removeAllFromFirebase(){
        String[] arrayKeys = registers.keySet().toArray(new String[0]);
        for (int i = 0; i < registers.size(); i++) {
            mDatabase.child(userID).child("Recorridos").child(arrayKeys[i]).removeValue();
        }
        Log.i(TAG, "Registros eliminados completamente");
    }

    private boolean checkUserId(){
        return userID == null || userID.equals("");
    }

}
