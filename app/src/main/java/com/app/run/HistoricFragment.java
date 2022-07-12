package com.app.run;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import java.util.LinkedHashMap;
import java.util.Vector;

public class HistoricFragment extends Fragment {

    private MainActivity mainActivity;
    private FirebaseManager firebaseManager;

    private LinkedHashMap<String, DataPack> registers = new LinkedHashMap<>();
    private AlertDialog.Builder deleteMessage;
    private AlertDialog.Builder showMapMessage;
    private int posItemToDelete;
    private int posItemToShow;
    private RegisterAdapter adapter = null;
    private ListView listView;
    private boolean firstTime = true;

    public void setMainActivity(MainActivity mainActivity){this.mainActivity = mainActivity;}

    public void setFirebaseManager(FirebaseManager firebaseManager){this.firebaseManager = firebaseManager;}

    public void setRegisters(LinkedHashMap<String, DataPack> linkedHashMap){
        LinkedHashMap<String, DataPack> newRegister = invertMap(linkedHashMap);
        if (registers.keySet() != newRegister.keySet()){
            if (this.registers.size() < newRegister.size() && listView != null)
                scroll(0);

            this.registers = invertMap(linkedHashMap);
            if (registers.size() > 0){
                if (adapter != null) {
                    if (listView.getAdapter() instanceof RegisterAdapter) {
                        adapter.setDataMap(this.registers);
                    }
                    adapter.notifyDataSetChanged();
                }
                else {
                    if (getContext() != null){
                        adapter = new RegisterAdapter(getContext(), registers);
                        listView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
            else
                noRegister();
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Inflo mi listView
        final View rootView = inflater.inflate(R.layout.historic, container, false);
        listView = rootView.findViewById(R.id.list_view_historic);
        createAlertsMessages();
        setListerners();
        
        if(registers.size() > 0) {
            adapter = new RegisterAdapter(getContext(), registers);
            listView.setAdapter(adapter);
        }
        //Si todavia no se han hecho busquedas
        else{
            noRegister();
        }

        return rootView;
    }

    private void scroll(final int position){
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                listView.smoothScrollToPosition(position);
            }
        };
        Handler handler = new Handler();
        handler.postDelayed(r, 100);
    }

    private void deleteRegister(){
        Pair<String, LinkedHashMap<String, DataPack>> pairAux = adapter.remove(posItemToDelete);
        firebaseManager.removeFromFirebase(pairAux.first);
        registers = pairAux.second;
        adapter.notifyDataSetChanged();
        if (posItemToDelete != 0) {
            scroll(posItemToDelete - 1);
        }
        else if(adapter.getCount() != 0)
            scroll(posItemToDelete);

    }

    private void noRegister(){
        if (getContext() != null){
            Vector<String> auxArray = new Vector<>();
            if (firstTime){
                auxArray.add("Cargando registros");
                firstTime = false;
                final Runnable r = new Runnable() {
                    public void run() {
                        if (!(listView.getAdapter() instanceof RegisterAdapter))
                            noRegister();
                    }
                };
                Handler handler = new Handler();
                handler.postDelayed(r, 10000);
            }
            else
                auxArray.add("No se tienen registros");
            ListAdapter listAdapter = new ArrayAdapter<>(getContext(), R.layout.list_empty_item, R.id.textview, auxArray);
            listView.setAdapter(listAdapter);
        }
    }

    private void setListerners(){
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                posItemToShow = position;
                //Algo que verifique si hay un seguimiento en progreso y luego muestre el mensaje
                if (mainActivity.getTracing())
                    showMapMessage.show();
                else{
                    mainActivity.showRegister(adapter.getItem(posItemToShow));
                }
            }
        });

        //En caso de click largo que defina la posicion del item presionado y muestre el mensaje
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                posItemToDelete = position;
                deleteMessage.show();
                return true;
            }
        });
    }

    private LinkedHashMap<String, DataPack> invertMap(LinkedHashMap<String, DataPack> linkedHashMap){
        LinkedHashMap<String, DataPack> auxMap = new LinkedHashMap<>();
        String[] arrayKeys = linkedHashMap.keySet().toArray(new String[0]);
        for (int i = linkedHashMap.size() - 1; i >= 0; i--) {
            auxMap.put(arrayKeys[i], linkedHashMap.get(arrayKeys[i]));
        }
        return auxMap;
    }

    private void createAlertsMessages(){
        deleteMessage = new AlertDialog.Builder(new ContextThemeWrapper(getContext(),
                R.style.AlertDialogCustom));
        deleteMessage.setMessage("¿ Desea borrar el registro seleccionado ?");
        deleteMessage.setCancelable(false);
        deleteMessage.setPositiveButton("Si", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface avisoBorrar, int id) {
                deleteRegister();
                //Si era el ultimo elemento agrego la frase de que no hay registros
//                if (adapter.getCount() == 0){
//                    noRegister();
//                }

            }
        });
        deleteMessage.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface avisoBorrar, int id) {
                avisoBorrar.cancel();
            }
        });


        showMapMessage = new AlertDialog.Builder(new ContextThemeWrapper(getContext(),
                R.style.AlertDialogCustom));
        showMapMessage.setMessage(" Hay un seguimiento en progreso. ¿ Quiere detenerlo ?");
        showMapMessage.setCancelable(false);
        showMapMessage.setPositiveButton("Si", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface avisoBorrar, int id) {
                mainActivity.showRegister(adapter.getItem(posItemToShow));
            }
        });
        showMapMessage.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface avisoBorrar, int id) {
                avisoBorrar.cancel();
            }
        });
    }
}
