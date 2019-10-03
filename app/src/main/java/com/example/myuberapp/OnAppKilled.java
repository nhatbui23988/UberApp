package com.example.myuberapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.firebase.geofire.GeoFire;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class OnAppKilled extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        String userID = FirebaseAuth.getInstance().getUid();
        if (userID != null){
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("driverAvailable");
            GeoFire geoFire = new GeoFire(reference);
//            sẽ gây lỗi vì không truyền geoListener
            geoFire.removeLocation(userID);
        }
    }
}
