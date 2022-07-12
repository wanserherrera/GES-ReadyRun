package com.app.run;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthenticationActivity extends AppCompatActivity {

    private static final String TAG = AuthenticationActivity.class.getSimpleName();
    Button btnGoogle;
    Button btnRegister;
    Button btnAccess;
    Button btnForgot;
    EditText mail;
    EditText pass;
    TextView txtInfo;
    private int GOOGLE_SIGN_IN = 17;
    private boolean firstEntry = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authentication_activity);
        btnAccess = findViewById(R.id.btn_auth_acc);
        btnForgot = findViewById(R.id.btn_auth_forgot_pass);
        txtInfo = findViewById(R.id.txt_info_auth);
        mail = findViewById(R.id.txt_mail);
        pass = findViewById(R.id.txt_pass);
        Utils.setPausedState(this, false);
        Utils.setUpdateState(this, false);
        hideForgotButton();
        setListeners();
    }

    private void setListeners(){



        btnAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                if(!mail.getText().toString().equals("") && !pass.getText().toString().equals("")){
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(mail.getText().toString(), pass.getText().toString())
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    signComplete(task, "MAIL", FirebaseAuth.getInstance().getUid());
                                }
                            });
                }
            }
        });


        btnForgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String auxPass = " ";
                if (!pass.getText().toString().equals(""))
                    auxPass = pass.getText().toString();
                FirebaseAuth.getInstance().signInWithEmailAndPassword(mail.getText().toString(), auxPass)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                try{
                                    FirebaseAuth.getInstance().sendPasswordResetEmail(mail.getText().toString())
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        txtInfo.setText(R.string.auth_info_mail_send);
                                                        Log.i(TAG, "Correo enviado");
                                                    }
                                                    else{
                                                        txtInfo.setText(R.string.auth_wrong_mail_in_forgot);
                                                    }
                                                }
                                            });
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                    txtInfo.setText(R.string.auth_wrong_mail_in_forgot);
                                    Log.e(TAG, "Excepción: problema al enviar el correo");
                                }
                            }
                        });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mail.setText("");
        pass.setText("");
        String provider = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Utils.AUTH_PROVIDER, "");
        String userId = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Utils.USER_ID, "");
        if (!provider.equals("") && !userId.equals("")){
            findViewById(R.id.auth_layout).setVisibility(View.INVISIBLE);
            if (firstEntry){
                showMain(provider, userId);
                firstEntry = false;
            }else
                this.finishAffinity();
        }
        else{
            findViewById(R.id.auth_layout).setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GOOGLE_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                final GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null){
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            signComplete(task, "GOOGLE", FirebaseAuth.getInstance().getUid());
                        }
                    });
                }
            }
            catch (ApiException e){
                txtInfo.setText(R.string.auth_error);
                e.printStackTrace();
                Log.i(TAG, "Excepción: error al autenticar en cuenta de google");
            }
        }
    }

    private void signComplete(Task<AuthResult> task, String provider, String userId){
        if (task.isSuccessful()){
            showMain(provider, userId);
            Log.i(TAG, "Autenticación completada");
        }
        else{
            Exception e = task.getException();
            if (e instanceof FirebaseAuthUserCollisionException){
                txtInfo.setText(R.string.auth_reg_mail);
                Log.i(TAG, "Excepción: correo ya registrado");
            }
            else if(e instanceof FirebaseAuthInvalidUserException){
                txtInfo.setText(R.string.auth_no_reg_mail);
                Log.i(TAG, "Excepción: correo no registrado");
            }
            else if (e instanceof FirebaseAuthInvalidCredentialsException){
                txtInfo.setText(R.string.auth_wrong_pass_mail);
                ViewGroup.LayoutParams lp = btnForgot.getLayoutParams();
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                Log.i(TAG, "Excepción: contraseña incorrecta o correo mal ingresado");
            }
            else{
                txtInfo.setText(R.string.auth_error);
                Log.i(TAG, "Excepción: error al autenticar con cuenta de correo");
            }
        }
    }

    private void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mail.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(pass.getWindowToken(), 0);
    }

    private void hideForgotButton(){
        btnForgot.setHeight(0);
    }

    private void showMain(String provider, String userId){
        Intent intent = new Intent(this, MainActivity.class)
                .putExtra(Utils.AUTH_PROVIDER, provider)
                .putExtra(Utils.USER_ID, userId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
