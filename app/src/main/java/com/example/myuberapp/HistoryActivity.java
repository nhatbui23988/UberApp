package com.example.myuberapp;

import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.LinearLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import afu.org.checkerframework.checker.oigj.qual.O;

public class HistoryActivity extends AppCompatActivity implements UberConstant{
    private RecyclerView rcvHistory;
    private HistoryAdapter adapter;
    private ArrayList<HistoryObject> listHistory;
    private String userID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
//
        getHistoryData();
        connectView();
    }
//  kiểm tra history mở từ hướng driver hay customer
    private void getHistoryData() {
        listHistory = new ArrayList<>();
        Intent intent = getIntent();
        String userHistory;
        if (intent!=null){
            userHistory = intent.getStringExtra("CustomerOrDriver");
            Log.e("nhat","user:"+userHistory);
        }
        else{
            return;
        }
//        get ID
        userID = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();
        Log.e("nhat","user ID:"+userID);
//        get node history theo user type và user id
        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child(userHistory)
                .child(userID)
                .child("history");
//        kiểm tra hitory
        historyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    for (DataSnapshot id:dataSnapshot.getChildren()){
                        getDetailHistory(id.getKey());
                        Log.e("nhat","id: "+id.toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
//lấy thông tin history nếu có
    private void getDetailHistory(final String key) {
        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("history")
                .child(key);
        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("nhat","dataSnapshot.exists(): "+dataSnapshot.exists());
                if (dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    HistoryObject historyObject = new HistoryObject();
                    historyObject.setHistoryID(key);
                    if (map.get("customer") != null){
                        historyObject.setCustomerName(map.get("customer").toString());
                        Log.e("nhat","customer: "+map.get("customer").toString());
                    }
                    if (map.get("driver") != null){
                        historyObject.setDriverName(map.get("driver").toString());
                    }
                    if (map.get(NODE_RATING) != null){
                        historyObject.setRating(map.get("rating").toString());
                    }
                    if (map.get(NODE_COMMENT) != null){
                        historyObject.setComment(map.get("comment").toString());
                    }
                    if (map.get(NODE_TIMESTAMP) != null){
                        long timestamp = Long.parseLong(map.get(NODE_TIMESTAMP).toString());
                        historyObject.setDate(getDate(timestamp));
                    }
                    listHistory.add(historyObject);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String getDate(long timestamp){
        Date date = new Date(timestamp);
        String dateResult = DateFormat.getDateTimeInstance().format(date);
        return dateResult;
    }

//    public static void main(String[] args){
//        Date date = new Date(System.currentTimeMillis()*1000);
//        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm");
//        String result = dateFormat.format(date);
//        System.out.println(result);
//    }

    private void connectView() {
        rcvHistory = findViewById(R.id.rcv_history);
        adapter = new HistoryAdapter(this, listHistory);
        rcvHistory.setAdapter(adapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rcvHistory.setLayoutManager(linearLayoutManager);
        adapter.setOnItemHistoryClick(new HistoryAdapter.OnItemHistoryClick() {
            @Override
            public void onItemClick(HistoryObject historyObject) {
                Intent intent = new Intent(HistoryActivity.this, HistorySingleActivity.class);
                intent.putExtra(KEY_RIDE_ID, historyObject.getHistoryID());
                startActivity(intent);
            }
        });
    }
}
