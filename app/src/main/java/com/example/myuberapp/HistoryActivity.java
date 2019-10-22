package com.example.myuberapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class HistoryActivity extends AppCompatActivity implements UberConstant{
    private RecyclerView rcvHistory;
    private HistoryAdapter adapter;
    private ArrayList<HistoryObject> listHistory;
    private String userID;
    private String userHistory;
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
        if (intent!=null){
            userHistory = intent.getStringExtra(KEY_DRIVER_OR_CUSTOMER);
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
                .child(NODE_USERS)
                .child(userHistory)
                .child(userID)
                .child(NODE_HISTORY);
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
                .child(NODE_HISTORY)
                .child(key);
        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("nhat","dataSnapshot.exists(): "+dataSnapshot.exists());
                if (dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    HistoryObject historyObject = new HistoryObject();
                    historyObject.setHistoryID(key);
                    if (map.get(NODE_CUSTOMER) != null){
                        historyObject.setCustomerID(map.get(NODE_CUSTOMER).toString());
                        Log.e("nhat","customer: "+map.get(NODE_CUSTOMER).toString());
                    }
                    if (map.get(NODE_DRIVER) != null){
                        historyObject.setDriverID(map.get(NODE_DRIVER).toString());
                    }
                    if (map.get(NODE_RATING) != null){
                        historyObject.setRating(map.get(NODE_RATING).toString());
                    }
                    if (map.get(NODE_COMMENT) != null){
                        historyObject.setComment(map.get(NODE_COMMENT).toString());
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

    private void connectView() {
        rcvHistory = findViewById(R.id.rcv_history);
        adapter = new HistoryAdapter(this, listHistory);
        rcvHistory.setAdapter(adapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        rcvHistory.setLayoutManager(linearLayoutManager);
        adapter.setOnItemHistoryClick(new HistoryAdapter.OnItemHistoryClick() {
            @Override
            public void onItemClick(HistoryObject historyObject) {
                Intent intent = new Intent(HistoryActivity.this, HistorySingleActivity.class);
                intent.putExtra(KEY_RIDE_ID, historyObject.getHistoryID());
                intent.putExtra(KEY_DRIVER_OR_CUSTOMER, userHistory);
                startActivity(intent);
            }
        });
    }
}
