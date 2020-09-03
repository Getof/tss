package ru.getof.taxispb.events;

import android.os.CountDownTimer;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import ru.getof.taxispb.utils.CommonUtils;

public class BaseRequestEvent {
    private BaseResultEvent resultEvent;
    CountDownTimer timer;
    public BaseRequestEvent(BaseResultEvent _resultEvent) {
        resultEvent = _resultEvent;
        timer = new CountDownTimer(15000,1000) {
            @Override
            public void onTick(long l) {
                Log.e("Time Passing","1");
            }

            @Override
            public void onFinish() {
                EventBus.getDefault().post(resultEvent);
            }
        }.start();
        CommonUtils.currentTimer = timer;
    }

}
