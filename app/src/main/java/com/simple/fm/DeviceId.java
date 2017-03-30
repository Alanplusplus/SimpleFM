package com.simple.fm;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * Created by Alan on 16/7/20.
 */
public class DeviceId {
    private static String sDeviceId;

    public static String getDeviceId(Context context){
        if (sDeviceId == null){
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            sDeviceId = manager.getDeviceId();
        }
        return sDeviceId;
    }
}
