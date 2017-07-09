package com.flt.cellmonitor;

import android.app.Application;
import android.content.Intent;

import com.flt.cellmonitor.services.MonitoringService;

public class CellMonitorApp extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    // start the service here - so that binding doesn't kill it later!
    Intent i = new Intent(getApplicationContext(), MonitoringService.class);
    getApplicationContext().startService(i);
  }
}
