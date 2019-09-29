package com.example.myuberapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomerSettingActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_CHOOSE_IMAGE = 1;
    //    View
    private EditText edtName, edtPhoneNumber;
    private Button btnCancel, btnConfirm;
    private TextView tvScreenTitle;
    private ImageView imageProfile;
//
    private FirebaseAuth auth;
    private DatabaseReference customerRef;
//
    private String userName, userPhone, userID, userProfileImageURL;
    private Uri resultImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_setting);
        connectView();
    }


    private void connectView() {
        tvScreenTitle = findViewById(R.id.tv_title);
        btnCancel = findViewById(R.id.btn_cancel);
        btnConfirm = findViewById(R.id.btn_confirm);
        edtPhoneNumber = findViewById(R.id.edt_user_phone);
        edtName= findViewById(R.id.edt_user_name);
        imageProfile = findViewById(R.id.image_profile);
//
        imageProfile.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnConfirm.setOnClickListener(this);
//
        getUserInfo();
    }
//  Lấy info user (nếu có) hiển thị lên UI
    private void getUserInfo(){
//        get user ID
        auth = FirebaseAuth.getInstance();
        userID = auth.getCurrentUser().getUid();
//        get ref customers+currentID
        customerRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Customers").child(userID);
//        Event nếu data trên Firebase đc cập nhật thì cập nhật lên UI
        customerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//              nếu user tồn tại, và đã có thông tin user thì get thông tin đưa lên EditText
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null){
                        userName = map.get("name").toString();
                        Log.e("nhat","userName: "+userName);
                        edtName.setText(userName);
                    }
                    if (map.get("phone") != null){
                        userPhone = map.get("phone").toString();
                        Log.e("nhat","userPhone: "+userPhone);
                        edtPhoneNumber.setText(userPhone);
                    }
                    if (map.get("profileImageUrl") != null){
                        userProfileImageURL = map.get("profileImageUrl").toString();
                        Log.e("nhat","url: "+userProfileImageURL);
                        Glide.with(getApplicationContext())
                                .load(userProfileImageURL)
                                .error(R.drawable.ic_user_default)
                                .into(imageProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_cancel:{
                finish();
                break;
            }
            case R.id.btn_confirm:{
                confirmUpdateUserInfo();
                break;
            }
            case R.id.image_profile:{
                openScreenChooseImage();
                break;
            }
        }
    }

    private void openScreenChooseImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_CHOOSE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        sau khi chọn ảnh sẽ gửi data result về
        if (requestCode == REQUEST_CODE_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK){
//            trong data sẽ có uri của ảnh vừa chọn
            if (data != null){
//                lấy uri ảnh
                Uri imageUri = data.getData();
//                lưu vào biến resultImageUri để có thể update lên Firebase khi cần
                resultImageUri = imageUri;
//                set ảnh vừa chọn lên UI
                imageProfile.setImageURI(imageUri);
            }

        }
    }


    //  xác nhận cập nhật info lên Firebase
    private void confirmUpdateUserInfo() {
        if (checkEditTextNotNull()){
//            get data
            userName = edtName.getText().toString();
            userPhone= edtPhoneNumber.getText().toString();
//            update name, phone to Firebase
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("name",userName);
            userInfo.put("phone",userPhone);
            customerRef.updateChildren(userInfo);

            if (resultImageUri != null){
//                Tạo 1 node lưu profile image trên Firebase Storage
                final StorageReference filePath = FirebaseStorage
                        .getInstance()
                        .getReference()
                        .child("profile_images")
                        .child(userID);
                Bitmap bitmap = null;
//                dùng content resolver để truy xuất thông tin của image
//                đc lưu trong devices, theo uri
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), resultImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                format ảnh: type, quality(0-100)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
//               parse bitmap sang byte
                byte[] data = baos.toByteArray();
//              upload image lên Firebase Storage
                UploadTask uploadTask = filePath.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e("nhat","uploadTask onFailure");
                        finish();
                    }
                });
                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                        add url vào node customers+userID
                        Log.e("nhat","uploadTask onSuccess");
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Uri downloadUri = uri;
                                Map newImage = new HashMap();
                                newImage.put("profileImageUrl", downloadUri.toString());
                                customerRef.updateChildren(newImage);
                                Log.e("nhat","getDownloadUrl onSuccess");
                                finish();
                            }
                        });

                    }
                });

            }else {
                finish();
            }
        }
    }
//
    private boolean checkEditTextNotNull() {
        if (TextUtils.isEmpty(edtName.getText()) || TextUtils.isEmpty(edtPhoneNumber.getText())){
            return false;
        } else {
            return true;
        }

    }
}
