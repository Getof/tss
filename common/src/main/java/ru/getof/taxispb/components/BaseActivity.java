package ru.getof.taxispb.components;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;

public class BaseActivity extends AppCompatActivity {
    public EventBus eventBus;
    public boolean registerEventBus = true;
    public boolean isInForeground = false;
    private boolean isImmersive = false;
    public float screenDensity;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(registerEventBus)
            eventBus = EventBus.getDefault();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus & isImmersive) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onStart() {
        if(registerEventBus)
            eventBus.register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        if(registerEventBus)
            eventBus.unregister(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isInForeground = true;
    }

    public int convertDPToPixel(int dp) {
        return (int) (dp * (screenDensity));
    }

    @Override
    public void setImmersive(boolean i) {
        isImmersive = i;
    }
}
