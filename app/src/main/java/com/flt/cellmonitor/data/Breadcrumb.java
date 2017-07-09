package com.flt.cellmonitor.data;

import android.location.Location;

import java.util.Date;

public class Breadcrumb {
  public Breadcrumb(Location location, Date time) {
    this.location = location;
    this.time = time;
  }

  public Location location;
  public Date time;
}
