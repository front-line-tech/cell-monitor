package com.flt.cellmonitor.services;

import com.flt.cellmonitor.data.Breadcrumb;
import com.flt.cellmonitor.data.NetworkUpdate;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.List;

public interface IMonitoringService {

  Breadcrumb getLastBreadcrumb();
  CircularFifoQueue<Breadcrumb> getAllBreadcrumbs();

  NetworkUpdate getLastNetworkUpdate();
  NetworkUpdate getLastTower();
  NetworkUpdate getLastNetwork();
  NetworkUpdate getLastData();

  List<NetworkUpdate> getAllNetworkUpdates();

  boolean isActivated();
  void activate();
  void deactivate();

  void clearDisplay();
  void informLogFile();
  void exportLogFile();
  void eraseLogFile();

  void addListener(MonitoringService.MonitoringServiceListener listener);
  void removeListener(MonitoringService.MonitoringServiceListener listener);

}