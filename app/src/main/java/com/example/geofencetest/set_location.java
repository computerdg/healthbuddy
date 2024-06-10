package com.example.geofencetest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class set_location extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng targetLocation;
    private Marker currentLocationMarker;
    private final float RADIUS_IN_METERS = 300;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean checkInSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_location);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button setLocationButton = findViewById(R.id.setLocation);
        setLocationButton.setOnClickListener(v -> setLocation());

        Button checkInButton = findViewById(R.id.checkInButton);
        checkInButton.setOnClickListener(v -> checkIn());

        Button completeLocationButton = findViewById(R.id.completeLocationButton);
        completeLocationButton.setOnClickListener(v -> completeLocation());

        // 사용자의 현재 위치 가져오기
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnSuccessListener(this, location -> {
            if (location != null) {
                targetLocation = new LatLng(location.getLatitude(), location.getLongitude());
                updateMap();
            } else {
                Toast.makeText(set_location.this, "현재 위치를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
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

        // 현재 위치 표시 활성화
        mMap.setMyLocationEnabled(true);

        // 초기 타겟 위치에 마커와 원 추가
        updateMap();

        mMap.getUiSettings().setZoomControlsEnabled(true);

        // 지도 클릭 리스너 설정
        mMap.setOnMapClickListener(latLng -> {
            targetLocation = latLng;
            updateMap();
            updateCurrentLocationMarker();
        });
    }

    private void updateMap() {
        if (mMap == null || targetLocation == null) return;

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(targetLocation).title("타겟 위치"));
        mMap.addCircle(new CircleOptions()
                .center(targetLocation)
                .radius(RADIUS_IN_METERS)
                .strokeColor(0x220000FF)
                .fillColor(0x220000FF));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 14));
    }

    private void updateCurrentLocationMarker() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    if (currentLocationMarker != null) {
                        currentLocationMarker.remove();
                    }
                    currentLocationMarker = mMap.addMarker(new MarkerOptions()
                            .position(currentLatLng)
                            .title("내 위치"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14));
                } else {
                    Toast.makeText(set_location.this, "현재 위치 확인 불가", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setLocation() {
        if (targetLocation != null) {
            Toast.makeText(set_location.this, "헬스장 위치가 설정되었습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(set_location.this, "지도를 클릭하여 위치를 설정하세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private void completeLocation() {
        if (targetLocation != null) {
            Intent intent = new Intent();
            intent.putExtra("latitude", targetLocation.latitude);
            intent.putExtra("longitude", targetLocation.longitude);
            intent.putExtra("checkInSuccess", checkInSuccess);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Toast.makeText(set_location.this, "헬스장 위치를 지정해주세요", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIn() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(this, location -> {
                if (location != null && targetLocation != null) {
                    float[] distance = new float[2];
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                            targetLocation.latitude, targetLocation.longitude, distance);
                    if (distance[0] <= RADIUS_IN_METERS) {
                        Toast.makeText(set_location.this, "출석 인증 성공", Toast.LENGTH_SHORT).show();
                        checkInSuccess = true;
                    } else {
                        Toast.makeText(set_location.this, "출석 인증 실패: 반경 내에 있지 않습니다.", Toast.LENGTH_SHORT).show();
                        checkInSuccess = false;
                    }
                } else if (targetLocation == null) {
                    Toast.makeText(set_location.this, "먼저 위치를 설정하세요.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(set_location.this, "현재 위치를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
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
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    updateCurrentLocationMarker();
                }
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
