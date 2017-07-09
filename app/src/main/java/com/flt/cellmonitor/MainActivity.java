package com.flt.cellmonitor;

import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Adapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.flt.cellmonitor.data.Breadcrumb;
import com.flt.cellmonitor.data.Descriptions;
import com.flt.cellmonitor.data.NetworkUpdate;
import com.flt.cellmonitor.services.MonitoringService;
import com.flt.cellmonitor.ui.NetworkUpdateListAdapter;
import com.kopfgeldjaeger.ratememaybe.RateMeMaybe;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.List;

public class MainActivity extends ServiceBoundActivity {

  TextView label_state;
  TextView label_tower_id;
  TextView label_network;
  TextView label_data_connection;
  TextView label_location;
  ToggleButton toggle_btn_enable;
  ListView log_list;

  NetworkUpdateListAdapter log_adapter;

  MonitoringService.MonitoringServiceListener listener;

  private final int REQUEST_PERMISSION_AND_ENABLE_MONITORING = 1001;
  private final String[] PERMISSIONS_REQUIRED = new String[] {
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION,
      Manifest.permission.ACCESS_NETWORK_STATE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.READ_PHONE_STATE
  };

  private Descriptions d;

  private RateMeMaybe rmm;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    d = new Descriptions(this);

    label_state = (TextView) findViewById(R.id.label_current_state_value);
    label_tower_id = (TextView) findViewById(R.id.label_current_tower_value);
    label_network = (TextView) findViewById(R.id.label_current_network_value);
    label_data_connection = (TextView) findViewById(R.id.label_current_data_connection_value);
    label_location = (TextView) findViewById(R.id.label_current_location_value);
    toggle_btn_enable = (ToggleButton) findViewById(R.id.button_enable_monitor);
    log_list = (ListView) findViewById(R.id.list_log_view);

    listener = createListener();

    toggle_btn_enable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(PERMISSIONS_REQUIRED, REQUEST_PERMISSION_AND_ENABLE_MONITORING);
          } else {
            enableMonitoring();
          }
        } else {
          disableMonitoring();
        }
      }
    });

    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      String version = pInfo.versionName;
      setTitle(getString(R.string.app_name) + " " + version);

    } catch (Exception e) {
      Log.e("CellMonitor", "Unable to read version.", e);
    }

    initRatingRequestCode();
  }

  private void initRatingRequestCode() {
    rmm = new RateMeMaybe(this);
    rmm.setPromptMinimums(3, 2, 2, 10);
    rmm.setDialogMessage(getString(R.string.feedback_request_message));
    rmm.setDialogTitle(getString(R.string.feedback_request_title));
    rmm.run();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    int granted = 0;
    for (int i = 0; i < permissions.length; i++) {
      if (grantResults[i] == PackageManager.PERMISSION_GRANTED) { granted++; }
    }
    if (granted == permissions.length) {
      switch (requestCode) {
        case REQUEST_PERMISSION_AND_ENABLE_MONITORING:
          enableMonitoring();
          break;
      }
    }
  }

  private void enableMonitoring() { service.activate(); }

  private void disableMonitoring() { service.deactivate(); }

  @Override
  protected void onBoundChanged(boolean isBound) {
    if (!isBound) {
      clearAllFields();
      service.removeListener(listener);
      toggle_btn_enable.setEnabled(false);
    } else {
      label_state.setText(d.describeActivation(service.isActivated()));
      toggle_btn_enable.setChecked(service.isActivated());
      label_tower_id.setText(either(d.describeTower(service.getLastTower()), getString(R.string.tower_unknown)));
      label_network.setText(either(d.describeNetwork(service.getLastNetwork()), getString(R.string.network_unknown)));
      label_data_connection.setText(either(d.describeDataConnection(service.getLastData()), getString(R.string.data_connection_unknown)));
      label_location.setText(d.describeBreadcrumb(service.getLastBreadcrumb()));

      if (log_adapter != null) {
        log_adapter.clear();
      }

      log_adapter = new NetworkUpdateListAdapter(this, R.layout.entry_update, service.getAllNetworkUpdates());
      log_list.setAdapter(log_adapter);
      service.addListener(listener);
      toggle_btn_enable.setEnabled(true);
    }

    invalidateOptionsMenu();
  }

  private String either(String a, String b) {
    return a != null ? a : b;
  }

  private void clearAllFields() {
    label_state.setText(getText(R.string.monitoring_disabled));
    label_tower_id.setText(getText(R.string.tower_unknown));
    label_network.setText(getText(R.string.network_unknown));
    label_data_connection.setText(getText(R.string.data_connection_unknown));
    label_location.setText(getString(R.string.location_unknown));
    log_list.setAdapter(null);
    log_adapter = null;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    if (bound) {
      menu.add(0, 1001, 1, getString(R.string.menu_clear_view));

      if (service.isActivated()) {
        menu.add(0, 1002, 2, getString(R.string.menu_disable));
      } else {
        menu.add(0, 1003, 2, getString(R.string.menu_enable));
      }

      menu.add(0, 1004, 3, getString(R.string.menu_export_logfile));
      menu.add(0, 1005, 4, getString(R.string.menu_locate_logfile));
      menu.add(0, 1006, 4, getString(R.string.menu_erase_logfile));

      menu.add(0, 1100, 100, getString(R.string.menu_give_feedback));
    }

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case 1001:
        service.clearDisplay();
        return true;

      case 1002:
        service.deactivate();
        return true;

      case 1003:
        service.activate();
        return true;

      case 1004:
        service.exportLogFile();
        return true;

      case 1005:
        service.informLogFile();
        return true;

      case 1006:
        service.eraseLogFile();
        return true;

      case 1100:
        rmm.forceShow();
        return true;
    }

    return false;
  }

  private MonitoringService.MonitoringServiceListener createListener() {
    return new MonitoringService.MonitoringServiceListener() {

      @Override
      public void onClear() {
        log_adapter.notifyDataSetChanged();
      }

      @Override
      public void onActivationChange(boolean isActiveNow) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            label_state.setText(service.isActivated() ? getString(R.string.monitoring_enabled) : getString(R.string.monitoring_disabled));
            toggle_btn_enable.setChecked(service.isActivated());
            invalidateOptionsMenu();
          }
        });
      }

      @Override
      public void onLocationUpdate(Breadcrumb last, CircularFifoQueue<Breadcrumb> all) {
        final Breadcrumb lastFinal = last;
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            label_location.setText(d.describeBreadcrumb(lastFinal));
          }
        });
      }

      @Override
      public void onNetworkUpdate(NetworkUpdate update, List<NetworkUpdate> all) {
        final NetworkUpdate updateFinal = update;
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            String tower = d.describeTower(updateFinal);
            String network = d.describeNetwork(updateFinal);
            String data = d.describeDataConnection(updateFinal);
            if (tower != null) { label_tower_id.setText(tower); }
            if (network != null) { label_network.setText(network); }
            if (data != null) { label_data_connection.setText(data); }
            log_adapter.notifyDataSetChanged();
          }
        });
      }
    };
  }
}
