package com.flt.cellmonitor.data;

import java.util.Date;

public class NetworkUpdate {

  public enum Change {
    StartTracking,
    StopTracking,

    Snapshot,

    ConnectNetwork,
    DisconnectNetwork,

    ConnectTower,
    DisconnectTower,

    ConnectData,
    DisconnectData,

    ChangeNetwork,
    ChangeTower,
    ChangeData
  }

  public NetworkUpdate(Date time, Change change, String modifier, Breadcrumb last_breadcrumb) {
    this.time = time;
    this.change = change;
    this.modifier = modifier;
    this.last_breadcrumb = last_breadcrumb;
  }

  public Date time;
  public Change change;
  public String modifier;
  public Breadcrumb last_breadcrumb;
  public String raw_detail;

  public boolean matches(NetworkUpdate other) {
    if (other == null) { return false; }

    boolean modifiers_match =
        (modifier == null && other.modifier == null) ||
            (modifier != null && modifier.equals(other.modifier)) ||
            (other.modifier != null && other.modifier.equals(modifier));

    return other.change == change && modifiers_match;
  }

  public boolean isNegative() {
    return change == Change.DisconnectTower ||
        change == Change.DisconnectNetwork ||
        change == Change.DisconnectData;
  }

  public boolean isApp() {
    return change == Change.StartTracking || change == Change.StopTracking;
  }

}
