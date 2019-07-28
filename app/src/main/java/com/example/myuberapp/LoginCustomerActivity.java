package com.example.myuberapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginCustomerActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText edtEmail, edtPassword;
    private Button btnSignin, btnSignup;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_customer);
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
                    Intent intent = new Intent(LoginCustomerActivity.this, MapActivity.class);
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

    private void checkSignUp(){
        User mUser = getUserInfo();
        if (mUser == null){
            Toast.makeText(this,"Không để trống", Toast.LENGTH_SHORT).show();
        }
        else {
            mAuth.createUserWithEmailAndPassword(mUser.getEmail(),
                    mUser.getPassword())
                    .addOnCompleteListener(LoginCustomerActivity.this,
                    new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(Task<AuthResult> task) {
                            if (!task.isSuccessful()){
                                Toast.makeText(LoginCustomerActivity.this, "error", Toast.LENGTH_SHORT).show();
                            }else {
                                String user_id = mAuth.getCurrentUser().getUid();
                                DatabaseReference current_user_db = FirebaseDatabase
                                        .getInstance()
                                        .getReference()
                                        .child("Users")
                                        .child("Customer")
                                        .child(user_id);
                                current_user_db.setValue(true);

                            }
                        }
                    });
        }
    }

    private void checkSignIn(){
        User mUser = getUserInfo();
        if (mUser == null){
            Toast.makeText(this,"Không để trống", Toast.LENGTH_SHORT).show();
        }else {
            mAuth.signInWithEmailAndPassword(mUser.getEmail(),
                    mUser.getPassword()).addOnCompleteListener(LoginCustomerActivity.this,
                    new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(Task<AuthResult> task) {
                            if (!task.isSuccessful()){
                                Toast.makeText(LoginCustomerActivity.this, "error", Toast.LENGTH_SHORT).show();
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
        if (mAuth != null)
            mAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuth != null)
            mAuth.removeAuthStateListener(authStateListener);
    }
}
