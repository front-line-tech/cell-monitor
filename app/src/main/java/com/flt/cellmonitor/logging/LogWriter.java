package com.flt.cellmonitor.logging;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.flt.cellmonitor.data.Descriptions;
import com.flt.cellmonitor.data.NetworkUpdate;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;

public class LogWriter {
  private static final String TAG = "LogWriter";

  Context context;
  Descriptions d;
  public File logfile;
  public File logfolder;

  public LogWriter(Context context) {
    this.context = context;
    this.d = new Descriptions(context);
    this.logfolder = getLogFolder();
    logfile = getLogFile();
  }

  public static File getLogFolder() {
    File logfolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    if (!logfolder.exists()) {
      boolean ok = logfolder.mkdirs();
      if (!ok) {
        Log.e(TAG, "Unable to create required directories.");
      }
    }
    return logfolder;
  }

  public static File getLogFile() {
    return new File(getLogFolder(), "celltracker.log");
  }

  public void writeLine(NetworkUpdate update) {
    String msg = d.describeEntryChange(update);
    String time = d.describeEntryTime(update);

    ArrayList<String> lines = new ArrayList<String>();

    String line =  time + " -- " + msg;

    Log.d(TAG, "Logging: " + line);
    lines.add(line);
    if (update.raw_detail != null) { lines.add("Raw detail -- " + update.raw_detail); }

    try {
      FileUtils.writeLines(logfile, lines, true);
    } catch (Exception e) {
      Log.e(TAG, "Unable to log", e);
    }
  }


}
