package com.example.geofencetest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class check_currentLocation extends AppCompatActivity implements OnMapReadyCallback {

    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mMap;
    private LatLng targetLatLng;
    private final float RADIUS_IN_METERS = 300;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_current_location);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // SharedPreferences 쓰면 되네 -> 내부 저장소 느낌으로 하는거 Intent로 안되노
        SharedPreferences sharedPreferences = getSharedPreferences("LocationData", Context.MODE_PRIVATE);
        double latitude = sharedPreferences.getFloat("latitude", 0);
        double longitude = sharedPreferences.getFloat("longitude", 0);
        targetLatLng = new LatLng(latitude, longitude);

        // 맵 프래그먼트
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.currentLocation);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 버튼 설정
        Button completeLocationButton = findViewById(R.id.btn_currentLocation_complete);
        completeLocationButton.setOnClickListener(v -> checkLocation());

        getCurrentLocation();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 위치 권한 요청
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // 맵튜닝
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // 설정된 타겟 위치에 마커와 반경 추가
        if (targetLatLng != null) {
            mMap.addMarker(new MarkerOptions().position(targetLatLng).title("설정된 위치"));
            mMap.addCircle(new CircleOptions()
                    .center(targetLatLng)
                    .radius(RADIUS_IN_METERS)
                    .strokeColor(0x220000FF)
                    .fillColor(0x220000FF));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 14));
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14));
                } else {
                    Toast.makeText(check_currentLocation.this, "현재 위치를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(this, location -> {
                if (location != null) {
                    float[] distance = new float[2];
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                            targetLatLng.latitude, targetLatLng.longitude, distance);
                    if (distance[0] <= RADIUS_IN_METERS) {
                        Toast.makeText(check_currentLocation.this, "위치인증 성공!! 운동을 시작합니다.", Toast.LENGTH_SHORT).show();
                        // 운동 정상 시작되면 오늘 루틴 페이지로 이동
                        Intent intent = new Intent(getApplicationContext(), complete_todayWorkout.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(check_currentLocation.this, "위치인증 실패: 지정한 헬스장으로 이동해주세요.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(check_currentLocation.this, "현재 위치를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocation();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
