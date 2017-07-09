package com.flt.cellmonitor.helpers;

import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import java.util.List;

public class NetworkHelper {
  private static final String TAG = "NetworkHelper";

  public static String learnCellId(CellLocation cellLocation) {
    if (cellLocation instanceof GsmCellLocation) {
      GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
      return String.valueOf("GSM CID: " + gsmCellLocation.getCid());
    }

    Log.w(TAG, "Cell location unreadable.");
    return null; // unreadable?!
  }

  public static String learnRegisteredCellId(List<CellInfo> cellInfos) {
    String tower = null;

    if (cellInfos == null) { return null; }

    for(CellInfo cellInfo : cellInfos) {
      if (cellInfo.isRegistered()) {

        if (cellInfo instanceof CellInfoGsm) {
          tower = "GSM CID: " + String.valueOf(((CellInfoGsm) cellInfo).getCellIdentity().getCid());
        }

        if (cellInfo instanceof CellInfoWcdma) {
          tower = "WCDMA CID: " + String.valueOf(((CellInfoWcdma) cellInfo).getCellIdentity().getCid());
        }

        if (cellInfo instanceof CellInfoLte) {
          tower = "LTE CI:" + String.valueOf(((CellInfoLte) cellInfo).getCellIdentity().getCi());
        }

        if (cellInfo instanceof CellInfoCdma) {
          tower = "CDMA BID: " + String.valueOf(((CellInfoCdma) cellInfo).getCellIdentity().getBasestationId());
        }

      }
    }

    return tower;
  }

}
