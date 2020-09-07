package ru.getof.rider.activities.splash;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.maps.model.LatLng;
import com.gun0912.tedpermission.BuildConfig;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.List;

import ru.getof.rider.R;
import ru.getof.rider.activities.main.MainActivity;
import ru.getof.rider.databinding.ActivitySplashBinding;
import ru.getof.rider.events.LoginResultEvent;
import ru.getof.rider.services.RiderService;
import ru.getof.taxispb.components.BaseActivity;
import ru.getof.taxispb.events.BackgroundServiceStartedEvent;
import ru.getof.taxispb.events.ConnectEvent;
import ru.getof.taxispb.events.ConnectResultEvent;
import ru.getof.taxispb.events.LoginEvent;
import ru.getof.taxispb.models.Rider;
import ru.getof.taxispb.utils.AlertDialogBuilder;
import ru.getof.taxispb.utils.AlerterHelper;
import ru.getof.taxispb.utils.CommonUtils;
import ru.getof.taxispb.utils.LocationHelper;
import ru.getof.taxispb.utils.MyPreferenceManager;

public class SplashActivity extends BaseActivity implements LocationListener {

    int RC_SIGN_IN = 123;
    LocationManager locationManager;
    LatLng currentLocation;
    MyPreferenceManager SP;
    ActivitySplashBinding binding;
    Handler locationTimeoutHandler;

    private PermissionListener permissionListener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            boolean isServiceRunning = isMyServiceRunning(RiderService.class);
            if (!isServiceRunning)
                startService(new Intent(SplashActivity.this, RiderService.class));
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            boolean isServiceRunning = isMyServiceRunning(RiderService.class);
            if (!isServiceRunning)
                startService(new Intent(SplashActivity.this, RiderService.class));
        }
    };

    private boolean isMyServiceRunning(Class<RiderService> riderServiceClass) {
        try {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (riderServiceClass.getName().equals(service.service.getClassName()))
                        return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private View.OnClickListener onLoginButtonClicked = view -> {
        startActivityForResult(
                AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(
                        Collections.singletonList(new AuthUI.IdpConfig.PhoneBuilder().build())
                )
                .build(),
                RC_SIGN_IN);

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(SplashActivity.this, R.layout.activity_splash);
        binding.loginButton.setOnClickListener(onLoginButtonClicked);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        SP = MyPreferenceManager.getInstance(getApplicationContext());
        checkPermissions();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginResultEvent(LoginResultEvent event) {
        if (event.hasError()) {
            event.showError(SplashActivity.this, result -> {
                if (result == AlertDialogBuilder.DialogResult.RETRY)
                    binding.loginButton.callOnClick();
                else
                    finish();
            });
            return;
        }
        CommonUtils.rider = event.rider;
        SP.putString("rider_user", event.riderJson);
        SP.putString("rider_token", event.jwtToken);
        tryConnect();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectedResult(ConnectResultEvent event) {
        if (event.hasError()) {
            binding.progressBar.setVisibility(View.INVISIBLE);
            event.showError(SplashActivity.this, result -> {
                if (result == AlertDialogBuilder.DialogResult.RETRY) {
                    eventBus.post(new ConnectEvent(SP.getString("rider_token", null)));
                    binding.progressBar.setVisibility(View.VISIBLE);
                } else {
                    binding.loginButton.setVisibility(View.VISIBLE);
                }
            });
            return;
        }
        locationTimeoutHandler = new Handler();
        locationTimeoutHandler.postDelayed(() -> {
            locationManager.removeUpdates(SplashActivity.this);
            if (currentLocation == null) {
                String[] location = getString(R.string.defaultLocation).split(",");
                double lat = Double.parseDouble(location[0]);
                double lng = Double.parseDouble(location[1]);
                currentLocation = new LatLng(lat, lng);
            }
            startMainActivity(currentLocation);

        }, 5000);
        searchCurrentLocation();
        CommonUtils.rider = Rider.fromJson(SP.getString("rider_user", "{}"));
    }

    private void startMainActivity(LatLng currentLocation) {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        double[] array = LocationHelper.LatLngToDoubleArray(currentLocation);
        intent.putExtra("currentLocation", array);
        startActivity(intent);
    }

    @Subscribe
    public void onServiceStart (BackgroundServiceStartedEvent event){
        tryConnect();
    }

    public void tryConnect() {
        String token = SP.getString("rider_token", null);
        if (token != null && !token.isEmpty()) {
            eventBus.post(new ConnectEvent(token));
        } else {
            binding.loginButton.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void checkPermissions() {
        if (!CommonUtils.isInternetEnabled(this)) {
            AlertDialogBuilder.show(this, getString(R.string.message_enable_wifi), AlertDialogBuilder.DialogButton.CANCEL_RETRY, result -> {
                if (result == AlertDialogBuilder.DialogResult.RETRY) {
                    checkPermissions();
                } else {
                    finishAffinity();
                }
            });
            return;
        }
        TedPermission.with(this)
                .setPermissionListener(permissionListener)
                .setDeniedMessage(getString(R.string.message_permission_denied))
                .setPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                    IdpResponse idpResponse = IdpResponse.fromResultIntent(data);
                    String phone;
                    if (idpResponse != null) {
                        phone = idpResponse.getPhoneNumber();
                        if (phone != null) {
                            tryLogin(phone);
                        }
                        return;
                    }


            }
            AlerterHelper.showError(SplashActivity.this, getString(R.string.login_failed));
        }
    }

    private void tryLogin(String phone) {
        binding.progressBar.setVisibility(View.VISIBLE);
        if (phone.substring(0,1).equals("+"))
            phone = phone.substring(1);
        eventBus.post(new LoginEvent(Long.parseLong(phone), BuildConfig.VERSION_CODE));
    }

    @SuppressLint("MissingPermission")
    private void searchCurrentLocation() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        tryConnect();
    }
}
