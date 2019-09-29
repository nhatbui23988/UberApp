package com.example.myuberapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnCustomer, btnDriver, btnTestFunc1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectView();
    }
    private void connectView(){
        btnCustomer = findViewById(R.id.btn_customer);
        btnDriver= findViewById(R.id.btn_driver);
        btnTestFunc1= findViewById(R.id.btn_test_func1);
        btnDriver.setOnClickListener(this);
        btnCustomer.setOnClickListener(this);
        btnTestFunc1.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_customer:{
                Intent intent = new Intent(MainActivity.this, LoginCustomerActivity.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btn_driver:{
                Intent intent = new Intent(MainActivity.this, LoginDriverActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.btn_test_func1:{
                Intent intent = new Intent(MainActivity.this, LoginDriverActivity.class);
                startActivity(intent);
                break;
            }
        }
    }
}
