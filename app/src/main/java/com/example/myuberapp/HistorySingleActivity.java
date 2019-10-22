package com.example.myuberapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;



public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, UberConstant, RoutingListener, View.OnClickListener {
    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private TextView tvName, tvDistance, tvPhone, tvFromTo, tvDate, tvRating, tvInfoTitle, tvPrice;
    private ImageView imgProfile;
    private RatingBar mRatingBar;
    private Button btnBack, btnAccept;
    private LinearLayout layoutRatingForCustomer, layoutRatingForDriver;
//
    private DatabaseReference historyRideRef;
    private LatLng pickUpLatLng, destinationLatLng;
//
    private String rideID = "", userID = "", customerID = "", driverID = "", userHistory ="";
    private List<Polyline> polylines;
    private float ratingValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_map);
        mMapFragment.getMapAsync(this);
        connectView();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void connectView() {
        tvFromTo = findViewById(R.id.tv_history_from_to);
        tvDistance = findViewById(R.id.tv_history_distance);
        tvDate = findViewById(R.id.tv_history_date);
        tvName = findViewById(R.id.tv_history_name);
        tvPhone = findViewById(R.id.tv_history_phone);
        tvPrice = findViewById(R.id.tv_history_price);
        imgProfile = findViewById(R.id.img_history_user_profile);
        mRatingBar = findViewById(R.id.rating_bar);
        btnAccept = findViewById(R.id.btn_accept);
        btnBack = findViewById(R.id.btn_back);
        tvRating = findViewById(R.id.tv_rating);
        tvInfoTitle = findViewById(R.id.tv_info_title);
        layoutRatingForCustomer = findViewById(R.id.layout_rating_for_customer);
        layoutRatingForDriver = findViewById(R.id.layout_rating_for_driver);

        btnAccept.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        getBaseData();
    }

    private void getBaseData() {
        Intent intent = getIntent();
        if (intent != null){
            rideID = intent.getStringExtra(KEY_RIDE_ID);
            userHistory = intent.getStringExtra(KEY_DRIVER_OR_CUSTOMER);
            if (userHistory.equals(NODE_CUSTOMERS)){
                layoutRatingForDriver.setVisibility(View.GONE);
                layoutRatingForCustomer.setVisibility(View.VISIBLE);
                tvInfoTitle.setText("Your Driver Info");
            }else if (userHistory.equals(NODE_DRIVERS)){
                mRatingBar.setIsIndicator(true);
                layoutRatingForDriver.setVisibility(View.VISIBLE);
                layoutRatingForCustomer.setVisibility(View.GONE);
                tvInfoTitle.setText("Your Customer Info");
            }
        }
        Log.e("nhat","ride ID"+rideID);
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getRideInfo();
    }

    private void getRideInfo() {
        historyRideRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(NODE_HISTORY)
                .child(rideID);
        historyRideRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get(NODE_CUSTOMER) != null){
                        customerID = map.get(NODE_CUSTOMER).toString();
                    }
                    if (map.get(NODE_DRIVER) != null){
                        driverID = map.get(NODE_DRIVER).toString();
                    }
                    if (map.get(NODE_RATING) != null){
                        tvRating.setText(map.get(NODE_RATING).toString());
                    }
//                    nếu current user là Customer thì hiển thị thông tin driver
                    if (userID.equals(customerID)){
                        getInfoOtherUser(NODE_DRIVERS, driverID);
                        displayCustomerRelatedObject();
                    }
//                    nếu current user là driver thì hiển thị thông tin customer
                    if (userID.equals(driverID)){
                        getInfoOtherUser(NODE_CUSTOMERS, customerID);
                    }
                    if (map.get(NODE_TIMESTAMP) != null){
                        tvDate.setText(getDate(map.get(NODE_TIMESTAMP).toString()));
                    }
                    if (map.get(NODE_DESTINATION) != null){
                        tvFromTo.setText(map.get(NODE_DESTINATION).toString());
                    }
                    if (map.get(NODE_DISTANCE) != null){
                        tvDistance.setText(map.get(NODE_DISTANCE).toString()+" km");
                    }
                    if (map.get(NODE_PRICE) != null){
                        tvPrice.setText(map.get(NODE_PRICE).toString()+" VND");
                    }
                    if (map.get(NODE_RATING) != null){
                        mRatingBar.setRating(Integer.parseInt(map.get(NODE_RATING).toString()));
                    }
                    if (map.get(NODE_LOCATION) != null){
//                        pickUpLatLng = new LatLng(
//                                Double.parseDouble(dataSnapshot.child(NODE_FROM).child(NODE_LAT).toString())
//                                ,Double.parseDouble(dataSnapshot.child(NODE_FROM).child(NODE_LNG).toString()));
//                        if (dataSnapshot.child(NODE_TO).child(NODE_LAT).exists()){
//                            destinationLatLng = new LatLng(
//                                    Double.parseDouble(dataSnapshot.child(NODE_TO).child(NODE_LAT).toString())
//                                    ,Double.parseDouble(dataSnapshot.child(NODE_TO).child(NODE_LNG).toString()) );
////                            getRouteToMaker();
//                            if (mMap != null){
//                                int height = 100;
//                                int width = 100;
//                                BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.drawable.location_pin,null);
//                                Bitmap b = bitmapdraw.getBitmap();
//                                Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
////                    đặt marker lên map để biết vị trí Driver
//                                Marker pickupMarker = mMap.addMarker(new MarkerOptions()
//                                        .position(pickUpLatLng)
//                                        .title("Your Pick Up Location")
//                                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
//
//                                BitmapDrawable bitmapdraw2 = (BitmapDrawable)getResources().getDrawable(R.drawable.location_pin,null);
//                                Bitmap b2 = bitmapdraw2.getBitmap();
//                                Bitmap smallMarker2 = Bitmap.createScaledBitmap(b2, width, height, false);
////                    đặt marker lên map để biết vị trí Driver
//                                Marker destinationMarker = mMap.addMarker(new MarkerOptions()
//                                        .position(destinationLatLng)
//                                        .title("Your Destination Location")
//                                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker2)));
//                            }
//                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
// save and display rating
    private void displayCustomerRelatedObject() {
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                ratingValue = rating;
            }
        });
    }

    private void getInfoOtherUser(String otherUser, String otherUserID) {
        DatabaseReference otherUserRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(NODE_USERS)
                .child(otherUser)
                .child(otherUserID);
        otherUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Map<String, Object> map= (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get(NODE_NAME) != null){
                        tvName.setText(map.get(NODE_NAME).toString());
                    }
                    if (map.get(NODE_PHONE) != null){
                        tvPhone.setText(map.get(NODE_PHONE).toString());
                    }
                    if (map.get(NODE_PROFILE_IMAGE_URL) != null){
                        Glide.with(getApplicationContext())
                                .load(map.get(NODE_PROFILE_IMAGE_URL).toString())
                                .error(R.drawable.ic_user_default)
                                .into(imgProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String getDate(String timestamp){
        Date date = new Date(Long.parseLong(timestamp));
        String dateResult = DateFormat.getDateTimeInstance().format(date);
        return dateResult;
    }

    private void getRouteToMaker() {
        Routing routing = new Routing.Builder()
                .key("AIzaSyBARj5JyjCV7hBN-9yC3dfzVbccEOSxdOM")
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(pickUpLatLng, destinationLatLng)
                .build();
        routing.execute();
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null) {
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Something went wrong, Please try again: ", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    //    Tim` duong
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
//        if (polylines.size() > 0) {
//            for (Polyline poly : polylines) {
//                poly.remove();
//            }
//        }
//
//        polylines = new ArrayList<>();
//        //add route(s) to the map.
//        for (int i = 0; i < route.size(); i++) {
//
//            //In case of more than 5 alternative routes
//            int colorIndex = i % COLORS.length;
//
//            PolylineOptions polyOptions = new PolylineOptions();
//            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
//            polyOptions.width(10 + i * 3);
//            polyOptions.addAll(route.get(i).getPoints());
//            Polyline polyline = mMap.addPolyline(polyOptions);
//            polylines.add(polyline);
//
//            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
//        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void removePolylines(){
        for (Polyline lines: polylines) {
            lines.remove();
        }
        polylines.clear();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_accept:{
                updateRatingForDriver(ratingValue);
                break;
            }
            case R.id.btn_back:{
                finish();
                break;
            }
        }
    }

    private void updateRatingForDriver(float ratingValue) {
        historyRideRef.child(NODE_RATING).setValue(ratingValue);
        DatabaseReference driverRatingRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(NODE_USERS)
                .child(NODE_DRIVERS)
                .child(driverID)
                .child(NODE_RATING);
        driverRatingRef.child(rideID).setValue(ratingValue);
    }
}
