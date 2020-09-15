package ru.getof.driver.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.List;

import ru.getof.driver.R;
import ru.getof.driver.databinding.ActivitySplashBinding;
import ru.getof.driver.events.LoginResultEvent;
import ru.getof.driver.services.DriverService;
import ru.getof.taxispb.BuildConfig;
import ru.getof.taxispb.components.BaseActivity;
import ru.getof.taxispb.events.BackgroundServiceStartedEvent;
import ru.getof.taxispb.events.ConnectEvent;
import ru.getof.taxispb.events.ConnectResultEvent;
import ru.getof.taxispb.events.LoginEvent;
import ru.getof.taxispb.interfaces.AlertDialogEvent;
import ru.getof.taxispb.models.Driver;
import ru.getof.taxispb.utils.AlertDialogBuilder;
import ru.getof.taxispb.utils.AlerterHelper;
import ru.getof.taxispb.utils.CommonUtils;
import ru.getof.taxispb.utils.MyPreferenceManager;

public class SplashActivity extends BaseActivity {

    MyPreferenceManager SP;
    int RC_SIGN_IN = 123;
    ActivitySplashBinding binding;


    private PermissionListener permissionListener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            try {
                if (!isMyServiceRunning(DriverService.class))
                    startService(new Intent(SplashActivity.this, DriverService.class));
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {

        }
    };


    private View.OnClickListener onLoginClicked = v -> {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(
                                Collections.singletonList(new AuthUI.IdpConfig.PhoneBuilder().build())
                        )
                        .build(), RC_SIGN_IN);
    };


    private boolean isMyServiceRunning(Class<DriverService> driverServiceClass) {
        try {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
                if (driverServiceClass.getName().equals(service.service.getClassName()))
                    return true;
            }
        } catch (Exception e){
            AlertDialogBuilder.show(this,e.getMessage());
        }
        return false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setImmersive(true);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash);
        binding.loginButton.setOnClickListener(onLoginClicked);
        SP = MyPreferenceManager.getInstance(getApplicationContext());

        checkPermissions();

    }

    private void checkPermissions() {
        if (!CommonUtils.isGPSEnabled(this)){
            AlertDialogBuilder.show(this, "Включите GPS", AlertDialogBuilder.DialogButton.CANCEL_RETRY, result -> {
                if (result == AlertDialogBuilder.DialogResult.RETRY) {
                    checkPermissions();
                } else {
                    finishAffinity();
                }
            });
            return;
        }
        if (!CommonUtils.isInternetEnabled(this)) {
            AlertDialogBuilder.show(this, "Подключите интернет", AlertDialogBuilder.DialogButton.CANCEL_RETRY, result -> {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectedResult(ConnectResultEvent event) {
        if (event.hasError()) {
            binding.progressBar.setVisibility(View.INVISIBLE);
            event.showError(SplashActivity.this, new AlertDialogEvent() {
                @Override
                public void onAnswerDialog(AlertDialogBuilder.DialogResult result) {
                    if (result == AlertDialogBuilder.DialogResult.RETRY) {
                        eventBus.post(new ConnectEvent(SP.getString("driver_token", null)));
                        binding.progressBar.setVisibility(View.VISIBLE);
                    } else {
                        binding.loginButton.setVisibility(View.VISIBLE);
                    }
                }
            });
            return;
        }

        CommonUtils.driver = new Gson().fromJson(SP.getString("driver_user", "{}"), Driver.class);
        startMainActivity();
    }

    private void startMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
    }

    @Subscribe
    public void onServiceStarted(BackgroundServiceStartedEvent event){
        tryConnect();
    }

    @Subscribe
    public void onLoginResultEvent(LoginResultEvent event) {
        if (event.hasError()) {
            event.showError(SplashActivity.this, new AlertDialogEvent() {
                @Override
                public void onAnswerDialog(AlertDialogBuilder.DialogResult result) {
                    if (result == AlertDialogBuilder.DialogResult.RETRY)
                        binding.loginButton.callOnClick();
                    else
                        finish();
                }
            });
            return;
        }
        CommonUtils.driver = event.driver;
        SP.putString("driver_user", event.driverJson);
        SP.putString("driver_token", event.jwtToken);
        tryConnect();
    }

    private void tryConnect() {
        try {
            String token = SP.getString("driver_token", null);
            if (token != null && !token.isEmpty()) {
                eventBus.post(new ConnectEvent(token));
            } else {
                binding.loginButton.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.INVISIBLE);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse idpResponse = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null){
                    String phone = user.getPhoneNumber();
                    tryLogin(phone);
                } else {
                    Toast.makeText(this, idpResponse.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
        AlerterHelper.showError(SplashActivity.this, getString(R.string.login_failed));
    }

    private void tryLogin(String phone) {
        binding.progressBar.setVisibility(View.VISIBLE);
        if (phone.substring(0, 1).equals("+"))
            phone = phone.substring(1);
        eventBus.post(new LoginEvent(Long.parseLong(phone), BuildConfig.VERSION_CODE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryConnect();
    }
}
