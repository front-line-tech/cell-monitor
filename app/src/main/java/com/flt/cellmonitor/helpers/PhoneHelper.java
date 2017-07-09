package com.flt.cellmonitor.helpers;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;

public class PhoneHelper {
  private static final String TAG = "PhoneHelper";

  public static String getPhoneDescription(Context context) {
    String description = "SERIAL: " +
        getPhoneSerial(context) + ", " +
        "IMEI: " + getPhoneIMEI(context) + ", " +
        "SIM: " + getSIM(context);

    return description;
  }

  public static String getSIM(Context context) {
    TelephonyManager mgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    return mgr.getSimSerialNumber();
  }

  public static String getPhoneSerial(Context context) {
    try {
      // sneaky - get it by reflection... sigh!
      Class<?> c = Class.forName("android.os.SystemProperties");
      Method get = c.getMethod("get", String.class, String.class);
      return (String) get.invoke(c, "ril.serialnumber", "unknown");
    } catch (Exception e) {
      Log.w(TAG, "Unable to extract SERIAL using ril.serialnumber from SystemProperties", e);
      return null;
    }
  }

  public static String getPhoneIMEI(Context context) {
    TelephonyManager mgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    return mgr.getDeviceId();
  }

}
