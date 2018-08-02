package su.openwifi.openwlanmap.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import java.util.List;
import java.util.Set;
import su.openwifi.openwlanmap.AccessPoint;
import su.openwifi.openwlanmap.QueryUtils;
import su.openwifi.openwlanmap.database.MyDatabase;

/**
 * Created by tali on 31.05.18.
 */

public class WifiUploader {
  private static final int MAX_TRIAL = 5;
  private static final String LOG_TAG = WifiUploader.class.getName();
  private Context context;
  private TotalApWrapper totalAps;
  private QueryUtils.RankingObject ranking;
  private String error;

  public WifiUploader(Context context, TotalApWrapper totalAps) {
    this.context = context;
    this.totalAps = totalAps;
  }

  /**
   * This method upload all data in database to backend.
   * @return true if successful otherwise false
   * @param id
   * @param tag
   * @param mode
   * @param pref_support_project
   */
  public boolean upload(String id, String tag, int mode, Set<String> pref_support_project) {
    //TODO read from preference
    //String testOwnBssid = "8911CDEE5A14";
    //String testTeam = "Team42";
    Log.e(LOG_TAG,id+"-"+tag+pref_support_project.toString());
    long count = MyDatabase.getInstance(context)
        .getAccessPointDao()
        .countEntries();
    int counter = 0;
    while (count > 0) {
      Log.i(LOG_TAG, "READING DATABASE......................db=" + count);
      List<AccessPoint> uploadEntries = MyDatabase.getInstance(context)
          .getAccessPointDao()
          .getAllDataEntriesToUpload();
      Log.i(LOG_TAG, "Uploading now = " + uploadEntries.size());
      if (checkConnection()) {
        //Upload
        ranking = QueryUtils.uploadData(uploadEntries, id, tag, mode);
        if (ranking != null) {
          //Delete
          Log.e(LOG_TAG, "Getting back from upload response");
          MyDatabase.getInstance(context)
              .getAccessPointDao()
              .delete(uploadEntries);
        } else {
          error = "Uploading failed!";
          return false;
        }
      } else {
        counter++;
        if (counter > MAX_TRIAL) {
          totalAps.setTotalAps(count);
          error = "Connection might be broken!";
          return false;
        }
      }
      count = MyDatabase.getInstance(context)
          .getAccessPointDao()
          .countEntries();
    }
    totalAps.setTotalAps(count);
    Log.i(LOG_TAG, "Done uploading......");
    return true;
  }

  private boolean checkConnection() {
    ConnectivityManager manager = (ConnectivityManager)
        context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo info = manager.getActiveNetworkInfo();
    return (info != null && info.isConnected());
  }

  public QueryUtils.RankingObject getRanking() {
    return ranking;
  }

  public String getError() {
    return error;
  }
}
