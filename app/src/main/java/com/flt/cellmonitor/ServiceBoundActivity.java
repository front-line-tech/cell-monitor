package com.flt.cellmonitor;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import com.flt.cellmonitor.services.IMonitoringService;
import com.flt.cellmonitor.services.MonitoringService;

public abstract class ServiceBoundActivity extends AppCompatActivity {

  protected IMonitoringService service;
  protected boolean bound;

  protected abstract void onBoundChanged(boolean isBound);

  @Override
  protected void onStart() {
    super.onStart();
    Intent intent = new Intent(this, MonitoringService.class);
    bindService(intent, connection, Context.BIND_AUTO_CREATE);
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (bound) {
      unbindService(connection);
      bound = false;
      onBoundChanged(bound);
    }
  }

  private ServiceConnection connection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {
      // We've bound to LocalService, cast the IBinder and get LocalService instance
      service = ((MonitoringService.LocationServiceBinder) binder).getService();
      bound = true;
      onBoundChanged(bound);
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      bound = false;
      onBoundChanged(bound);
    }
  };



}
