package com.naji_astier.lecrousclub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    private Alarm alarm = new Alarm();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            alarm.startAlarm(context);
        }
    }
}
