package com.example.myuberapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.appcompat.app.AppCompatActivity;

public class LoginDriverActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText edtEmail, edtPassword;
    private Button btnSignin, btnSignup;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_driver);
        connectView();
    }
    private void connectView() {
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        btnSignin = findViewById(R.id.btn_sign_in);
        btnSignup = findViewById(R.id.btn_sign_up);

        btnSignin.setOnClickListener(this);
        btnSignup.setOnClickListener(this);
        mAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
                FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
                if (mUser!=null){
                    Log.e("nhat","user:"
                            +"\nemail: "+mUser.getEmail()
                            +"\nUID: "+mUser.getUid()
                            +"\nProviderID: "+mUser.getProviderId());
                    Intent intent = new Intent(LoginDriverActivity.this, DriverMapActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_sign_in:{
                checkSignIn();
                break;
            }
            case R.id.btn_sign_up:{
                checkSignUp();
                break;
            }
        }
    }
    private void checkSignIn(){
        User mUser = getUserInfo();
        if (mUser == null){
            Toast.makeText(this,"Không để trống", Toast.LENGTH_SHORT).show();
        }else {
            mAuth.signInWithEmailAndPassword(mUser.getEmail(),
                    mUser.getPassword()).addOnCompleteListener(LoginDriverActivity.this,
                    new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(Task<AuthResult> task) {
                            if (!task.isSuccessful()){
                                Toast.makeText(LoginDriverActivity.this, "error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

        }
    }
    private void checkSignUp(){
        User mUser = getUserInfo();
        if (mUser == null){
            Toast.makeText(this,"Không để trống", Toast.LENGTH_SHORT).show();
        }
        else {
            mAuth.createUserWithEmailAndPassword(mUser.getEmail(),
                    mUser.getPassword()).addOnCompleteListener(LoginDriverActivity.this,
                    new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(Task<AuthResult> task) {
                            if (!task.isSuccessful()){
                                Toast.makeText(LoginDriverActivity.this, "error", Toast.LENGTH_SHORT).show();
                            }else {
                                String user_id = mAuth.getCurrentUser().getUid();
                                DatabaseReference current_user_db = FirebaseDatabase
                                        .getInstance()
                                        .getReference()
                                        .child("Users")
                                        .child("Drivers")
                                        .child(user_id);
                                current_user_db.setValue(true);

                            }
                        }
                    });
        }
    }

    private User getUserInfo() {
        String email, pass;
        email = edtEmail.getText().toString();
        pass = edtPassword.getText().toString();
        if (TextUtils.isEmpty(email)||TextUtils.isEmpty(pass)){
            return null;
        }
        else return new User(email, pass);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(authStateListener);
    }
}
