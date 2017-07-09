package com.flt.cellmonitor.data;

import android.content.Context;
import android.location.Location;
import android.text.format.DateFormat;

import com.flt.cellmonitor.R;

public class Descriptions {

  private Context context;

  public Descriptions(Context context) {
    this.context = context;
  }

  public String describeEntryChange(NetworkUpdate update) {
    switch (update.change) {
      case StartTracking:
      case StopTracking:
      case DisconnectData:
      case DisconnectNetwork:
      case DisconnectTower:
        if (update.modifier != null) {
          return describeChange(update) + ": " + update.modifier;
        } else {
          return describeChange(update);
        }

      case ConnectData:
      case ChangeData:
        return describeChange(update) + ": " + describeDataConnection(update);

      case ConnectNetwork:
      case ChangeNetwork:
        return describeChange(update) + ": " + describeNetwork(update);

      case ConnectTower:
      case ChangeTower:
        return describeChange(update) + ": " + describeTower(update);

      default:
        return describeChange(update) + ": " + update.modifier;
    }
  }

  public String describeEntryTime(NetworkUpdate update) {
    if (update.last_breadcrumb == null) {
      return describeTime(update);
    } else {
      return describeTime(update) + "; at " + describeBreadcrumb(update.last_breadcrumb);
    }
  }

  public String describeChange(NetworkUpdate update) {
    return update.change.name();
  }

  public String describeTime(NetworkUpdate update) {
    return DateFormat.format("yyyy-MM-dd HH:mm:ss", update.time).toString();
  }

  public String describeActivation(boolean active) {
    return active ?
        context.getString(R.string.monitoring_enabled) :
        context.getString(R.string.monitoring_disabled);
  }

  public String describeLocation(Location location) {
    if (location == null) { return context.getString(R.string.location_unknown); }
    return
        "LAT: " + String.format("%.6f", location.getLatitude()) + ", " +
        "LNG: " + String.format("%.6f", location.getLongitude());
  }

  public String describeBreadcrumb(Breadcrumb morsel) {
    if (morsel == null) { return context.getString(R.string.location_unknown); }
    return describeLocation(morsel.location);
  }

  public String describeTower(NetworkUpdate morsel) {
    if (morsel != null && (morsel.change == NetworkUpdate.Change.ConnectTower || morsel.change == NetworkUpdate.Change.ChangeTower)) {
      return morsel.modifier;
    } else {
      return null;
    }
  }

  public String describeNetwork(NetworkUpdate morsel) {
    if (morsel != null && (morsel.change == NetworkUpdate.Change.ConnectNetwork || morsel.change == NetworkUpdate.Change.ChangeNetwork)) {
      return morsel.modifier;
    } else {
      return null;
    }
  }

  public String describeDataConnection(NetworkUpdate morsel) {
    if (morsel != null && (morsel.change == NetworkUpdate.Change.ConnectData || morsel.change == NetworkUpdate.Change.ChangeData)) {
      return morsel.modifier;
    } else {
      return null;
    }
  }


}
