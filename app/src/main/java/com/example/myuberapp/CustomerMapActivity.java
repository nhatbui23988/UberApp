package com.example.myuberapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback,
        View.OnClickListener,
        UberConstant{

    private static final int MAX_RADIUS = 5;
//
    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LatLng pickupLocation, destinationLatLng;
    private GeoFire.CompletionListener geoFireListener;
    private GeoQuery geoQuery, geoQueryDriversAround;
    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef, driverAroundInfoRef;
    private ValueEventListener driverValueEventListener, driverAroundValueEventListener;
    //    view
    private Button btnLogOut, btnRequest, btnCancelRequest, btnSetting, btnHideOrShow, btnCancelDriverInfo, btnChooseDriver, btnHistory;
    private SupportMapFragment mapFragment;
    private Marker pickUpMarker;
    private TextView tvDriverName, tvDriverPhone, tvDriverCar, tvService;
    private ImageView imageDriverProfile;
    private CardView layoutDriverInfo;
    private RadioGroup radioGroupDriverService;
    private RatingBar mRatingBar;
    private LinearLayout layoutSubButtons;
    //
    private String requestService;
    private boolean isRequest = false;
    private boolean isDriverFound = false;
    private boolean isGetDriversAround = false;
    private int radius = 1;
    private String destination;
    private String driverFoundID = "";
    private List<Marker> markersList = new ArrayList<>();
    private boolean isAutoFindDriver = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        connectView();
    }

    private void getInfoDriversAround() {
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                if (driverAroundValueEventListener != null){
//                    xoa listener driver cu
                    driverAroundInfoRef.removeEventListener(driverAroundValueEventListener);
                }
//                show driverInfo
                layoutSubButtons.setVisibility(View.VISIBLE);
                layoutDriverInfo.setVisibility(View.VISIBLE);
                driverAroundInfoRef = FirebaseDatabase.getInstance()
                        .getReference()
                        .child(NODE_USERS)
                        .child(NODE_DRIVERS)
                        .child(marker.getTag().toString());
                driverAroundValueEventListener = driverAroundInfoRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                            driverFoundID = marker.getTag().toString();
                            if (map.get(NODE_NAME) != null){
                                tvDriverName.setText(map.get(NODE_NAME).toString());
                            }
                            if (map.get(NODE_PHONE) != null){
                                tvDriverPhone.setText(map.get(NODE_PHONE).toString());
                            }
                            if (map.get(NODE_SERVICE) != null){
                                tvService.setText(map.get(NODE_SERVICE).toString());
                            }
                            if (map.get(NODE_CAR) != null){
                                tvDriverCar.setText(map.get(NODE_CAR).toString());
                            }
                            if (map.get(NODE_PROFILE_IMAGE_URL) != null){
                                String url = map.get("profileImageUrl").toString();
                                Glide.with(CustomerMapActivity.this)
                                        .load(url)
                                        .error(R.drawable.ic_user_default)
                                        .into(imageDriverProfile);
                            }

                            int ratingSum = 0, ratingsTotal = 0;
                            float ratingAvg = 0;
                            for (DataSnapshot child : dataSnapshot.child(NODE_RATING).getChildren()){
                                ratingSum += Integer.parseInt(child.getValue().toString());
                                ratingsTotal++;
                            }
                            if (ratingsTotal != 0){
                                ratingAvg = (float) ratingSum/ratingsTotal;
                                mRatingBar.setRating(ratingAvg);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                return true;
            }
        });
    }

    private void getDriversAround() {
        isGetDriversAround = true;
        final DatabaseReference driversAroundRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(NODE_DRIVER_AVAILABLE);
        GeoFire geoFire = new GeoFire(driversAroundRef);
        geoQueryDriversAround = geoFire.queryAtLocation(new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), 10000);
        geoQueryDriversAround.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
//                query tim duoc driver o gan do
                for (Marker markerIt : markersList){
//                    nếu đã add vào list rồi thì không add nữa
                    if (markerIt.getTag().equals(key)){
                        return;
                    }
//                    nếu chưa add thì bắt đầu đánh dấu marker lên map và add vào list
                }
                LatLng driverLocation = new LatLng(location.latitude, location.longitude);

                Marker driverMarkerAround = mMap.addMarker(new MarkerOptions().position(driverLocation));
                driverMarkerAround.setTag(key);
                markersList.add(driverMarkerAround);
            }

            @Override
            public void onKeyExited(String key) {
//                khi driver do khong con nua
                for (Marker markerIt : markersList) {
//                  remove marker khỎi map, remove khỏi list
                    if (markerIt.getTag().equals(key)) {
                        markerIt.remove();
                        markersList.remove(markerIt);
                        return;
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
//                khi driver di chuyen sang vi tri moi
                for (Marker markerIt : markersList) {
//                  cap nhat vi tri marker
                    if (markerIt.getTag().equals(key)) {
                        markerIt.setPosition(new LatLng(location.latitude, location.longitude));
                        markersList.remove(markerIt);
                        return;
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
        getInfoDriversAround();
    }

    private void connectView() {
//
        btnHideOrShow = findViewById(R.id.btn_hide_or_show_driver_info);
        btnCancelDriverInfo = findViewById(R.id.btn_cancel_driver_info);
        btnCancelRequest = findViewById(R.id.btn_cancel_request);
        btnHistory = findViewById(R.id.btn_history);
        btnChooseDriver = findViewById(R.id.btn_choose_driver);
        btnHideOrShow = findViewById(R.id.btn_hide_or_show_driver_info);
        mRatingBar = findViewById(R.id.rating_bar);
        btnLogOut = findViewById(R.id.btn_log_out);
        btnRequest = findViewById(R.id.btn_request);
        btnSetting = findViewById(R.id.btn_setting);
        tvDriverCar = findViewById(R.id.tv_driver_car);
        tvService = findViewById(R.id.tv_driver_service);
        tvDriverName = findViewById(R.id.tv_driver_name);
        tvDriverPhone = findViewById(R.id.tv_driver_phone);
        layoutDriverInfo = findViewById(R.id.layout_driver_info);
        layoutSubButtons = findViewById(R.id.layout_sub_buttons);
        imageDriverProfile = findViewById(R.id.image_driver_profile);
        radioGroupDriverService = findViewById(R.id.radio_group_services);
//
        btnLogOut.setOnClickListener(this);
        btnRequest.setOnClickListener(this);
        btnCancelRequest.setOnClickListener(this);
        btnSetting.setOnClickListener(this);
        btnCancelDriverInfo.setOnClickListener(this);
        btnHideOrShow.setOnClickListener(this);
        btnChooseDriver.setOnClickListener(this);
        btnHistory.setOnClickListener(this);
//
        destinationLatLng = new LatLng(0,0);

        setUpEvent();
    }

    private void setUpEvent() {
        //        geoFireListener cần thêm vào setLocation để không bị error
        geoFireListener = new GeoFire.CompletionListener(){
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        };
//        callback khi gọi requestLocationUpdate
        mLocationCallback = new LocationCallback(){
            @Override
            //gọi event cập nhật vị trí
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    //lấy tọa độ nhận được
                    lastLocation = locationResult.getLastLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    //di chuyển camera map đến tọa độ định vị được
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    //animation zoom
                    mMap.animateCamera(CameraUpdateFactory.zoomBy(11));
                    if (!isGetDriversAround){
                        getDriversAround();
                    }
                }
            }
            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                Log.e("nhat",""+locationAvailability.toString());
            }
        };
// Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

// Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

// Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
                Log.e("nhat", "Place: " + place.getName() + ", " + place.getId());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.e("nhat", "An error occurred: " + status);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_request:{
//                nếu không đang request thì mới gửi request
                if (!isRequest){
                    setCustomerPickUp();
                }
                break;
            }
            case R.id.btn_cancel_request:{
//                set info to default and hide it, hide btn
                if (isRequest){
                    endRequestRide();
                }
                break;
            }
            case R.id.btn_log_out:{
                logOutCustomer();
                break;
            }
            case R.id.btn_history:{
                goToHistory();
                break;
            }
            case R.id.btn_setting:{
                goToSettingScreen();
                break;
            }
            case R.id.btn_choose_driver:{
                isAutoFindDriver = false;
                setCustomerPickUp();
//                sendRequest to this driver
                break;
            }
            case R.id.btn_cancel_driver_info:{
                setHideLayoutDriverInfo();
                break;
            }
            case R.id.btn_hide_or_show_driver_info:{
                if (layoutDriverInfo.getVisibility() == View.GONE){
                    layoutDriverInfo.setVisibility(View.VISIBLE);
                }
                else if (layoutDriverInfo.getVisibility() == View.VISIBLE){
                    layoutDriverInfo.setVisibility(View.GONE);
                }
                break;
            }
        }
    }

    private void goToHistory() {
    }

    //    chuyen qua man hinh setting thong tin user
    private void goToSettingScreen() {
        Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingActivity.class);
        startActivity(intent);
    }
//    dang xuat, xoa node Customer+ID
    private void logOutCustomer() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
//     bat dau setUpSendRequest
    private void setCustomerPickUp() {
//        check radio button service
//        chon loại xe
        if (!checkRequestService()){
            return;
        }
//        Hiển thị button cancel request
//        ẩn button lựa chọn driver trên map
        btnCancelRequest.setVisibility(View.VISIBLE);
        layoutSubButtons.setVisibility(View.GONE);
//
//        ngừng chức năng dò tìm driver xung quanh
        if (isGetDriversAround){
            stopGetDriversAround();
        }
//        set trạng thái đang gửi request = true
        isRequest = true;
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //tạo node customer request
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(reference);
        //set tọa độ của khách lên FireBase
        pickupLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        if (userID != null) {
            Log.e("nhat", "userID:" + userID);
            geoFire.setLocation(userID
                    , new GeoLocation(pickupLocation.latitude, pickupLocation.longitude)
                    , geoFireListener);
        }
//        Marker resize
        int height = 100;
        int width = 100;
        BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.drawable.location_pin  ,null);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
//        dat marker pickup len map
        pickUpMarker = mMap.addMarker(new MarkerOptions()
                .position(pickupLocation)
                .title("Pickup here")
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
        );
        btnRequest.setText("Getting your driver");
// nếu đang trong chế độ tự tìm driver, thì tìm driver gần đây
        if (isAutoFindDriver){
            getClosestDriver();
        }
//        nếu gửi request cho driver chọn trên bản đồ thì get ID Driver
        else {
            startSendCustomerRequest(driverFoundID);
        }
    }
//      dung tim kiem cac driver xung quanh
    private void stopGetDriversAround() {
        if (geoQueryDriversAround != null){
            geoQueryDriversAround.removeAllListeners();
        }
//        if (markersList.size()>0){
//            for (Marker markerIt : markersList){
//                markersList.remove(markerIt);
//                markerIt.remove();
//            }
//        }
    }
//      kiem tra da chon service chua
    private boolean checkRequestService() {
        int selectedId = radioGroupDriverService.getCheckedRadioButtonId();

        final RadioButton radioButton = findViewById(selectedId);

        if (TextUtils.isEmpty(radioButton.getText())){
            return false;
        }else {
            requestService = radioButton.getText().toString();
            return true;
        }
    }
//      kết thúc request ride
    private void endRequestRide() {
        //Nếu đang gửi request tìm Driver, bấm lần nữa để hủy
        //dừng query tìm kiếm driver, cũng như tìm location của driver đã nhận cuốc
        isRequest = false;
        isGetDriversAround = true;
//      remove dò driver gần nhất
        if (geoQuery != null){
            geoQuery.removeAllListeners();
        }
//      remove  xác định vị trí driver
        if (driverValueEventListener != null){
            driverLocationRef.removeEventListener(driverValueEventListener);
        }
//      remove  lắng nghe sự kiện từ chối của driver
        if (driverHasEndedRefListener != null){
            driverHasEndedRef.removeEventListener(driverHasEndedRefListener);
        }
        btnRequest.setText("Request");

        //set các giá trị về default
        if (driverFoundID != null){
            DatabaseReference driverRef = FirebaseDatabase
                    .getInstance()
                    .getReference()
                    .child("Users")
                    .child("Drivers")
                    .child(driverFoundID)
                    .child(NODE_CUSTOMER_REQUEST)
                    ;
//            xóa bên trong node Driver, set lại là true, bỏ customerRequest
            driverRef.removeValue();
//            ID driver tìm đc về null
            driverFoundID = null;

//            hide View Driver Info
           setHideLayoutDriverInfo();
        }
        isDriverFound = false;
        radius = 1;

        //remove location đã gửi request đi
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(reference);
        geoFire.removeLocation(userID, geoFireListener);
        //        Remove Marker
        if (pickUpMarker != null){
            pickUpMarker.remove();
        }

    }

    private void setHideLayoutDriverInfo() {
        layoutDriverInfo.setVisibility(View.GONE);
        tvDriverPhone.setText("");
        tvDriverName.setText("");
        tvDriverCar.setText("");
        layoutSubButtons.setVisibility(View.GONE);
        btnCancelRequest.setVisibility(View.GONE);
        imageDriverProfile.setImageResource(R.drawable.ic_user_default);
    }

    //
    private DatabaseReference driverHasEndedRef;
    private ValueEventListener driverHasEndedRefListener;
//    lắng nghe sự kiện khi driver complete chuyến đi
    private void getHasEndedRequest(){
//        end request tu phia driver: driver complete ride
        driverHasEndedRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(driverFoundID)
                .child("customerRequest");
        driverHasEndedRefListener = driverHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
//      nếu customerRequest còn thì chưa kết thúc
                } else {
//      đã kết thúc chuyến
                    endRequestRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
// dò driver ở gần
    private void getClosestDriver() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference()
                .child("driverAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
//        remove listener cũ
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
//            driver has been found
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
//                đã tìm đc driver gần bạn
//                nếu trước đó chưa tìm đc driver nào thì chọn driver này
                if (!isDriverFound && isRequest){
                    startSendCustomerRequest(key);
                    Log.e("nhat","onKeyEntered driver ID: "+key);
//
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!isDriverFound){
//                    if (radius < MAX_RADIUS){
//                        radius++;
//                        getClosestDriver();
//                    }
                    radius++;
                    Log.e("nhat",""+radius);
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
// gửi customer request cho driver
    private void startSendCustomerRequest(String key) {
        DatabaseReference customerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
        customerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    if (isDriverFound) {
                        return;
                    }
                    if (driverMap.get("service").equals(requestService)) {
//                                    SUCCESS
//                    chọn driver này-> chuyển isDriverFound = true
                        isDriverFound = true;
//                    lấy ID driver
                        driverFoundID = dataSnapshot.getKey();
//          Tìm đến node driver có ID đc truyền vào
                        DatabaseReference driverRef = FirebaseDatabase
                                .getInstance()
                                .getReference()
                                .child("Users")
                                .child("Drivers")
                                .child(driverFoundID)
                                .child("customerRequest");
//                    thêm ID của mình (khách hàng) vào bên trong node của driver đó
//                        thêm thông tin vị trí đích
                        String customerID = FirebaseAuth
                                .getInstance()
                                .getCurrentUser()
                                .getUid();
                        HashMap map = new HashMap();
                        map.put("customerRideID", customerID);
                        map.put("destination", destination);
                        map.put("destinationLat", destinationLatLng.latitude);
                        map.put("destinationLatLng", destinationLatLng.longitude);
                        Log.e("nhat", "onKeyEntered customerID ID: " + customerID);
                        Log.e("nhat", "onKeyEntered destination: " + destination);
//                    làm mới node
                        driverRef.updateChildren(map);
//                  hiển thị vị trí driver vừa tìm đc
                        getDriverLocation();
//                  Hiển thị thông tin driver
                        getDriverInfo();
//                  Kiểm tra nếu Driver từ chối request thì sẽ xóa request
                        getHasEndedRequest();
                        btnRequest.setText("Found your Driver");
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    // get thông tin driver
    private void getDriverInfo() {
        layoutDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference driverInfoRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(driverFoundID);

        driverInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null){
                        tvDriverName.setText(map.get("name").toString());
                        Log.e("nhat","driver name: "+map.get("name").toString());
                    }
                    if (map.get("phone") != null){
                        tvDriverPhone.setText(map.get("phone").toString());
                        Log.e("nhat","driver phone: "+map.get("phone").toString());
                    }
                    if (map.get("car") != null){
                        tvDriverCar.setText(map.get("car").toString());
                        Log.e("nhat","driver car: "+map.get("car").toString());
                    }
                    if (map.get("profileImageUrl") != null){
                        String url = map.get("profileImageUrl").toString();
                        Glide.with(CustomerMapActivity.this)
                                .load(url)
                                .error(R.drawable.ic_user_default)
                                .into(imageDriverProfile);
                        Log.e("nhat","driver car: "+map.get("car").toString());
                    }

                    int ratingSum = 0, ratingsTotal = 0;
                    float ratingAvg = 0;
                    for (DataSnapshot child : dataSnapshot.child(NODE_RATING).getChildren()){
                        ratingSum += Integer.parseInt(child.getValue().toString());
                        ratingsTotal++;
                    }
                    if (ratingsTotal != 0){
                        ratingAvg = (float) ratingSum/ratingsTotal;
                        mRatingBar.setRating(ratingAvg);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void getInfoRide(){

    }
    private void getDriverLocation() {
//        tìm đến thông tin vị trí của Driver
        driverLocationRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("driverWorking")
                .child(driverFoundID)
                .child("l");

        driverValueEventListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0, locationLng = 0;
                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if (mDriverMarker != null){
                        mDriverMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(driverLatLng.latitude);
                    loc1.setLongitude(driverLatLng.longitude);
//
                    Location loc2 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    float distance = loc1.distanceTo(loc2);
                    if (distance<100){
//                        thong bao tai xe sap toi roi
                        btnHideOrShow.setText("Your Driver arrived");
                    }
                    else{
                        btnHideOrShow.setText("Distance your Driver: "+String.valueOf(distance));
                    }
//                    resize
                    int height = 100;
                    int width = 100;
                    BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.drawable.car,null);
                    Bitmap b = bitmapdraw.getBitmap();
                    Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
//                    đặt marker lên map để biết vị trí Driver
                    mDriverMarker = mMap.addMarker(new MarkerOptions()
                            .position(driverLatLng)
                            .title("Your Driver Location")
                            .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
        //từ phiên bản Android Version 6.0 trở lên sẽ cần người dùng ban quyền
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            checkLocationPermission();
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            new AlertDialog.Builder(this)
                    .setTitle("Grant permission")
                    .setMessage("Please provide permission")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create()
                    .show();
        }
        else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }
        }
    }


    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_REQUEST_CODE);
    }

    @Override
    protected void onStart() {
        super.onStart();
//        if(!checkPermissions()){
//            startLocationPermissionRequest();
//        }
//        checkLocationPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        removeLocation();
    }
//  func remove cần đặt lại chỗ khác
    private void removeLocation() {
//        String userID = FirebaseAuth.getInstance().getUid();
//        if (userID != null){
//            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("customerRequest");
//            GeoFire geoFire = new GeoFire(reference);
//            geoFire.removeLocation(userID, geoFireListener);
//        }

    }

    final int LOCATION_REQUEST_CODE = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_REQUEST_CODE:{
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                }else {
                    Toast.makeText(getApplicationContext(), "Please grant permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


}
