package ru.getof.taxispb.utils;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.tapadoo.alerter.Alerter;

import ru.getof.taxispb.R;

public class AlerterHelper {
    public static void showError(Context context, String message){
        Alerter.create((AppCompatActivity)context)
                .setTitle(R.string.error)
                .setText(message)
                .setIcon(R.drawable.ic_error)
                .setBackgroundColorRes(R.color.accent_red)
                .show();
    }
    public static void showWarning(Context context,String message){
        Alerter.create((AppCompatActivity)context)
                .setTitle(R.string.warning)
                .setText(message)
                .setIcon(R.drawable.ic_warning)
                .setBackgroundColorRes(R.color.accent_orange)
                .show();
    }
    public static void showInfo(Context context,String message){
        Alerter.create((AppCompatActivity)context)
                .setTitle(R.string.info)
                .setText(message)
                .setIcon(R.drawable.ic_info)
                .setBackgroundColorRes(R.color.accent_cyan)
                .show();
    }
}
