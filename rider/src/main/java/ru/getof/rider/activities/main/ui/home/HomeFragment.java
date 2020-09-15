package ru.getof.rider.activities.main.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;
import com.pollux.widget.DualProgressView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.getof.rider.R;
import ru.getof.rider.activities.RequestDriverActivity;
import ru.getof.rider.activities.splash.SplashActivity;
import ru.getof.rider.events.SelectPlaceEvent;
import ru.getof.taxispb.events.ProfileInfoChangedEvent;
import ru.getof.taxispb.interfaces.ISputnikAPI;
import ru.getof.taxispb.utils.LocationHelper;
import ru.getof.taxispb.utils.RetrofitClient;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private ISputnikAPI iSputnikAPI;

    private double distance = 1.0; //default in km
    private static final double LIMIT_RANGE = 10.0; //km
    private Location previousLocation, currentLocation; //Use to calculate distance
    private boolean firstTime, firstStart = true;
    private String cityName;
    private LatLng curLocation;

    //Bind
    DualProgressView dualProgress_origin;
    ImageView img_origin;
    private CardView cardBtn_destination;


    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private TextView txt_origin, txt_city;


    @Override
    public void onStop() {
        compositeDisposable.clear();
        firstStart = false;
        super.onStop();
    }

    @Override
    public void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
//        curLocation = LocationHelper.DoubleArrayToLatLng(getArguments().getDoubleArray("curLocation"));
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        initView(root);

        init();

        return root;
    }

    private void initView(View root) {
        dualProgress_origin = root.findViewById(R.id.dualProgress_origin);
        img_origin = root.findViewById(R.id.img_origin);
        txt_origin = root.findViewById(R.id.txt_origin);
        txt_city = root.findViewById(R.id.txt_city);
        cardBtn_destination = root.findViewById(R.id.cardBtn_destination);
    }


    private void init() {

        iSputnikAPI = RetrofitClient.getInstance().create(ISputnikAPI.class);



        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());



//                Call<String> address = iSputnikAPI.geoToAddress(newPosition.latitude, newPosition.longitude);
//                address.enqueue(new Callback<String>() {
//                    @Override
//                    public void onResponse(Call<String> call, Response<String> response) {
//                        cityName = response.body();
//                    }
//
//                    @Override
//                    public void onFailure(Call<String> call, Throwable t) {
//
//                    }
//                });


                if (firstTime) {
                    previousLocation = currentLocation = locationResult.getLastLocation();
                    firstTime = false;
                } else {
                    previousLocation = currentLocation;
                    currentLocation = locationResult.getLastLocation();
                }
            }
        };

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        curLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        getAddress(curLocation);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        cardBtn_destination.setOnClickListener(view -> {
            startActivity(new Intent(getContext(), RequestDriverActivity.class));
            EventBus.getDefault().postSticky(new SelectPlaceEvent(curLocation));
        });



    }

    private void getAddress(LatLng newPosition) {
        Call<String> address = iSputnikAPI.geoToAddress(newPosition.latitude, newPosition.longitude, 1);
                address.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response.body());
                            JSONArray jsonArray = jsonObject.getJSONArray("suggestions");
                            JSONObject object = jsonArray.getJSONObject(0);
                            JSONObject data = object.getJSONObject("data");
                            cityName = data.getString("city");
                            txt_origin.setText(new StringBuilder(data.getString("street_with_type"))
                                    .append(", ")
                                    .append(data.getString("house_type"))
                                    .append(".")
                                    .append(data.getString("house")).toString());
                            dualProgress_origin.setVisibility(View.GONE);
                            img_origin.setVisibility(View.VISIBLE);
                            txt_city.setText(cityName);

                        } catch (Exception e){
                            Snackbar.make(getView(),e.getMessage(),Snackbar.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {

                    }
                });
    }

}