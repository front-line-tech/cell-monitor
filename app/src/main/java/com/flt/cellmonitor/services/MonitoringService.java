package com.flt.cellmonitor.services;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.Toast;

import com.flt.cellmonitor.Constants;
import com.flt.cellmonitor.MainActivity;
import com.flt.cellmonitor.R;
import com.flt.cellmonitor.data.Breadcrumb;
import com.flt.cellmonitor.data.Descriptions;
import com.flt.cellmonitor.data.NetworkUpdate;
import com.flt.cellmonitor.helpers.NetworkHelper;
import com.flt.cellmonitor.helpers.PhoneHelper;
import com.flt.cellmonitor.logging.LogWriter;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static android.telephony.ServiceState.STATE_EMERGENCY_ONLY;
import static android.telephony.ServiceState.STATE_IN_SERVICE;
import static android.telephony.ServiceState.STATE_OUT_OF_SERVICE;
import static android.telephony.ServiceState.STATE_POWER_OFF;

public class MonitoringService extends Service implements IMonitoringService {
  private static final String TAG = "MonitoringService";

  private static final int FOREGROUND_ID = 1001;
  private Notification foreground_notification;

  private LocationManager locationManager;
  private LocationListener locationListener;
  private Breadcrumb last_breadcrumb;
  private CircularFifoQueue<Breadcrumb> breadcrumbs;

  private ConnectivityManager connectivityManager;
  private TelephonyManager telephonyManager;
  private NetworkStateReceiver networkReceiver;
  private IntentFilter networkReceiverFilter;
  private PhoneStateListener phoneStateListener;

  private NetworkUpdate last_network_update;
  private NetworkUpdate lastTower;
  private NetworkUpdate lastNetwork;
  private NetworkUpdate lastData;

  private List<NetworkUpdate> network_updates;

  private IBinder binder;

  private List<SoftReference<MonitoringServiceListener>> listeners;

  private boolean active;

  private Descriptions d;
  private LogWriter logger;

  @Override
  public void onCreate() {
    super.onCreate();

    listeners = new ArrayList<SoftReference<MonitoringServiceListener>>();
    d = new Descriptions(this);

    binder = new LocationServiceBinder();

    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

    breadcrumbs = new CircularFifoQueue<Breadcrumb>(Constants.MAX_BREADCRUMBS);
    network_updates = new ArrayList<NetworkUpdate>();

    locationListener = createLocationListener();

    networkReceiver = new NetworkStateReceiver();
    networkReceiver.addListener(createNetworkStateReceiverListener());
    networkReceiverFilter = new IntentFilter();
    networkReceiverFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

    phoneStateListener = createPhoneStateListener();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    deactivate();
    super.onDestroy();
  }

  @Override
  public void activate() {
    logger = new LogWriter(this);
    boolean location_OK = initLocationUpdates();
    boolean network_OK = initNetworkUpdates();
    active = location_OK && network_OK;
    if (active) {
      recordStartTracking();
      goToForeground();
    }
    sendNotificationActive(active);
  }

  @Override
  public void deactivate() {
    haltLocationUpdates();
    haltNetworkUpdates();
    active = false;
    recordStopTracking();
    sendNotificationActive(active);
    quitForeground();
  }

  @Override
  public void clearDisplay() {
    network_updates.clear();
    sendNotificationClear();
  }

  private boolean initLocationUpdates() {
    if (Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0.0f, locationListener);
      return true;
    } else {
      Log.w(TAG, "Permission not granted for location.");
      return false;
    }
  }

  private void haltLocationUpdates() {
    locationManager.removeUpdates(locationListener);
  }

  private boolean initNetworkUpdates() {
    registerReceiver(networkReceiver, networkReceiverFilter);

    telephonyManager.listen(phoneStateListener,
        PhoneStateListener.LISTEN_CELL_INFO |
            PhoneStateListener.LISTEN_CELL_LOCATION |
            PhoneStateListener.LISTEN_SERVICE_STATE );

    return true;
  }

  private void haltNetworkUpdates() {
    try {
      unregisterReceiver(networkReceiver);
    } catch (Exception e) {
      Log.w(TAG, "Exception when unregistering receiver", e);
    }
    //telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
  }

  private void addLocation(Location location) {
    last_breadcrumb = new Breadcrumb(location, Calendar.getInstance().getTime());
    breadcrumbs.add(last_breadcrumb);
    sendNotificationLocationUpdate();
  }

  private void goToForeground() {
    foreground_notification = buildStandardNotification();
    startForeground(FOREGROUND_ID, foreground_notification);
  }

  private Notification buildStandardNotification() {
    Intent launchIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);

    return new Notification.Builder(this)
        .setContentTitle("Active")
        .setContentText("Cell and location tracking is active.")
        .setSmallIcon(R.mipmap.ic_launcher_cellmonitor)
        .setPriority(Notification.PRIORITY_MAX)
        .setContentIntent(pendingIntent)
        .build();
  }

  private void quitForeground() {
    stopForeground(true);
    foreground_notification = null;
  }

  @Override
  public Breadcrumb getLastBreadcrumb() {
    return last_breadcrumb;
  }

  @Override
  public CircularFifoQueue<Breadcrumb> getAllBreadcrumbs() {
    return breadcrumbs;
  }

  @Override
  public NetworkUpdate getLastNetworkUpdate() {
    return last_network_update;
  }

  @Override
  public List<NetworkUpdate> getAllNetworkUpdates() {
    return network_updates;
  }

  @Override
  public boolean isActivated() {
    return active;
  }

  private void sendNotificationClear() {
    for (SoftReference<MonitoringServiceListener> sr : listeners) {
      MonitoringServiceListener listener = sr.get();
      if (listener != null) {
        listener.onClear();
      }
    }
  }

  private void sendNotificationActive(boolean isActive) {
    for (SoftReference<MonitoringServiceListener> sr : listeners) {
      MonitoringServiceListener listener = sr.get();
      if (listener != null) {
        listener.onActivationChange(isActive);
      }
    }
  }

  private void sendNotificationLocationUpdate() {
    for (SoftReference<MonitoringServiceListener> sr : listeners) {
      MonitoringServiceListener listener = sr.get();
      if (listener != null) {
        listener.onLocationUpdate(last_breadcrumb, breadcrumbs);
      }
    }
  }

  private void sendNotificationNetworkUpdate() {
    for (SoftReference<MonitoringServiceListener> sr : listeners) {
      MonitoringServiceListener listener = sr.get();
      if (listener != null) {
        listener.onNetworkUpdate(last_network_update, network_updates);
      }
    }
  }

  private LocationListener createLocationListener() {
    return new LocationListener() {
      public void onLocationChanged(Location location) {
        Log.d(TAG, "Location change received: " + d.describeLocation(location));
        addLocation(location);
      }

      public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Status changed for provider: " + provider + ", status: " + status);
      }

      public void onProviderEnabled(String provider) {
        Log.i(TAG, "Provider enabled: " + provider);

      }

      public void onProviderDisabled(String provider) {
        Log.w(TAG, "Provider disabled: " + provider);
      }
    };
  }

  private NetworkStateReceiver.NetworkStateReceiverListener createNetworkStateReceiverListener() {
    return new NetworkStateReceiver.NetworkStateReceiverListener() {

      @Override
      public void networkAvailable() {
        Log.d(TAG, "networkAvailable");

        List<CellInfo> cellInfos = (List<CellInfo>) telephonyManager.getAllCellInfo();

        String tower = null;
        String network = null;
        String data_connection = null;

        tower = NetworkHelper.learnRegisteredCellId(cellInfos);
        network = telephonyManager.getNetworkOperatorName();

        if (network != null) { recordNetworkConnection(network, null); }
        if (tower != null) { recordTower(tower); }
      }

      @Override
      public void networkUnavailable() {
        Log.d(TAG, "networkUnavailable");
        recordDisconnection();
      }
    };
  }

  private void recordStartTracking() {
    NetworkUpdate nuStart = new NetworkUpdate(
        new Date(),
        NetworkUpdate.Change.StartTracking,
        null,
        getLastBreadcrumb());

    nuStart.raw_detail = PhoneHelper.getPhoneDescription(this);

    record(nuStart);
    lastNetwork = nuStart;
  }

  private void recordStopTracking() {
    NetworkUpdate nuStop = new NetworkUpdate(
        new Date(),
        NetworkUpdate.Change.StopTracking,
        null,
        getLastBreadcrumb());

    record(nuStop);
    lastNetwork = nuStop;
  }

  private void recordNetworkConnection(String network, String raw) {
    NetworkUpdate nu = new NetworkUpdate(
        new Date(),
        NetworkUpdate.Change.ConnectNetwork,
        network,
        getLastBreadcrumb());

    nu.raw_detail = raw;

    if (!nu.matches(lastNetwork)) { record(nu); }
    lastNetwork = nu;
  }

  private void recordTower(String tower) {
    NetworkUpdate nu = new NetworkUpdate(
        new Date(),
        NetworkUpdate.Change.ConnectTower,
        tower,
        getLastBreadcrumb());

    if (!nu.matches(lastTower)) { record(nu); }
    lastTower = nu;
  }

  private void recordDisconnection() {
    NetworkUpdate nu = new NetworkUpdate(
        new Date(),
        NetworkUpdate.Change.DisconnectNetwork,
        null,
        getLastBreadcrumb());

    record(nu);
    lastNetwork = null;
    lastTower = null;
  }

  private void recordNoTower() {
    NetworkUpdate nu = new NetworkUpdate(
        new Date(),
        NetworkUpdate.Change.DisconnectTower,
        null,
        getLastBreadcrumb());

    record(nu);
    lastTower = null;
  }

  private void recordNoTower(String reason) {
    NetworkUpdate nu = new NetworkUpdate(
        new Date(),
        NetworkUpdate.Change.DisconnectTower,
        reason,
        getLastBreadcrumb());

    record(nu);
    lastTower = null;
  }

  private void recordDisconnection(String reason) {
    NetworkUpdate nu = new NetworkUpdate(
        new Date(),
        NetworkUpdate.Change.DisconnectNetwork,
        reason,
        getLastBreadcrumb());

    record(nu);
    lastNetwork = null;
    lastTower = null;
  }

  @Override public NetworkUpdate getLastNetwork() { return lastNetwork; }
  @Override public NetworkUpdate getLastTower() { return lastTower; }
  @Override public NetworkUpdate getLastData() { return lastData; }

  private void record(NetworkUpdate update) {
    last_network_update = update;
    network_updates.add(update);
    if (logger != null) {
      logger.writeLine(update);
    } else {
      Log.w(TAG, "Logger was null when MonitoringService tried to record()");
    }
    sendNotificationNetworkUpdate();
  }

  @Override
  public void informLogFile() {
    Toast.makeText(this, LogWriter.getLogFile().getAbsolutePath(), Toast.LENGTH_LONG).show();
  }

  @Override
  public void exportLogFile() {
    try {
      Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
      emailIntent.setType("*/*");
      // emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { getString(R.string.email_logfile_to_support) });
      emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.email_logfile_subject));
      emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.email_logfile_text));
      emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(LogWriter.getLogFile()));
      startActivity(Intent.createChooser(emailIntent, getString(R.string.email_logfile_chooser_text)));
    } catch (Exception e) {
      Log.e(TAG, "Unexpected exception during export log file.", e);
      informUser(e.getClass().getName() + " occurred while exporting the log file: " + e.getMessage());
    }
  }

  @Override
  public void eraseLogFile() {
    File log = LogWriter.getLogFile();
    if (log.exists()) {
      boolean success = log.delete();
      if (!success) {
        Log.w(TAG, "Unable to delete log file: " + log.getAbsolutePath());
        informUser("Unable to erase log file.");
      } else {
        Log.i(TAG, "Log file deleted: " + log.getAbsolutePath());
        clearDisplay();

        if (active) {
          recordStartTracking();
        }
      }
    }
  }

  private void informUser(String msg) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
  }

  private PhoneStateListener createPhoneStateListener() {
    return new PhoneStateListener() {

      @Override
      public void onCellLocationChanged(CellLocation location) {
        Log.d(TAG, "onCellLocationChanged");

        String tower = NetworkHelper.learnCellId(location);
        if (tower != null) { recordTower(tower); } else { recordNoTower("PhoneStateListener.onCellLocationChanged"); }
      }

      @Override
      public void onCellInfoChanged(List<CellInfo> cellInfo) {
        Log.d(TAG, "onCellInfoChanged");

        if (cellInfo == null) { Log.w(TAG, "onCellInfoChanged with null"); return; }

        String tower = NetworkHelper.learnRegisteredCellId(cellInfo);
        if (tower != null) { recordTower(String.valueOf(tower)); } else { recordNoTower("PhoneStateListener.onCellInfoChanged"); }
      }

      @Override
      public void onServiceStateChanged(ServiceState serviceState) {
        Log.d(TAG, "ServiceState = " + serviceState.toString());

        switch (serviceState.getState()) {
          case ServiceState.STATE_IN_SERVICE:
            recordNetworkConnection(serviceState.getOperatorAlphaLong(), serviceState.toString());
            break;
          case STATE_OUT_OF_SERVICE:
            recordDisconnection("No service");
            break;
          case STATE_EMERGENCY_ONLY:
            recordNetworkConnection(serviceState.getOperatorAlphaLong() + " (emergency only)", serviceState.toString());
            break;
          case STATE_POWER_OFF:
            recordDisconnection("Power off");
            break;
        }

      }
    };
  }

  public class LocationServiceBinder extends Binder {
    public IMonitoringService getService() { return MonitoringService.this; }
  }

  @Override
  public IBinder onBind(Intent intent) { return binder; }

  public interface MonitoringServiceListener {
    void onClear();
    void onActivationChange(boolean isActiveNow);
    void onLocationUpdate(Breadcrumb last, CircularFifoQueue<Breadcrumb> all);
    void onNetworkUpdate(NetworkUpdate update, List<NetworkUpdate> all);
  }

  @Override
  public void addListener(MonitoringServiceListener l) {
    boolean alreadyPresent = false;
    ArrayList<SoftReference<MonitoringServiceListener>> toRemove = new ArrayList<>();

    for (SoftReference<MonitoringServiceListener> sr : listeners) {
      if (sr.get() == null) {
        toRemove.add(sr);
      }
      if (sr.get() == l) {
        alreadyPresent = true;
      }
    }
    for (SoftReference<MonitoringServiceListener> sr : toRemove) {
      listeners.remove(sr);
    }
    if (!alreadyPresent) { listeners.add(new SoftReference<MonitoringServiceListener>(l)); }
  }

  @Override
  public void removeListener(MonitoringServiceListener l) {
    ArrayList<SoftReference<MonitoringServiceListener>> toRemove = new ArrayList<>();
    for (SoftReference<MonitoringServiceListener> sr : listeners) {
      if (sr.get() == null || sr.get() == l) {
        toRemove.add(sr);
      }
    }
    for (SoftReference<MonitoringServiceListener> sr : toRemove) {
      listeners.remove(sr);
    }
  }

}