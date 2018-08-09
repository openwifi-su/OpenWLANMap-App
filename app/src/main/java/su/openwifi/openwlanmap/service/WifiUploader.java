package su.openwifi.openwlanmap.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import java.util.List;
import su.openwifi.openwlanmap.AccessPoint;
import su.openwifi.openwlanmap.R;
import su.openwifi.openwlanmap.database.MyDatabase;
import su.openwifi.openwlanmap.utils.QueryUtils;
import su.openwifi.openwlanmap.utils.RankingObject;
import su.openwifi.openwlanmap.utils.UploadQueryUtils;

/**
 * Created by tali on 31.05.18.
 */

public class WifiUploader {
  private static final int MAX_TRIAL = 5;
  private static final String LOG_TAG = WifiUploader.class.getName();
  private Context context;
  private TotalApWrapper totalAps;
  private RankingObject ranking;
  private String error;

  public WifiUploader(Context context, TotalApWrapper totalAps) {
    this.context = context;
    this.totalAps = totalAps;
  }

  /**
   * This method upload all data in database to backend.
   *
   * @return true if successful otherwise false
   */
  public boolean upload() {
    //String testOwnBssid = "8911CDEE5A14";
    //String testTeam = "Team42";
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
        ranking = UploadQueryUtils.uploadOpenWifi(uploadEntries,
            ServiceController.ownId.toUpperCase(),
            ServiceController.teamId.toUpperCase(),
            ServiceController.tag,
            ServiceController.mode);
        if (ranking != null) {
          //Delete
          Log.e(LOG_TAG, "Getting back from upload response");
          MyDatabase.getInstance(context)
              .getAccessPointDao()
              .delete(uploadEntries);
        } else {
          error = context.getString(R.string.upload_error) + "\n" + QueryUtils.requestDetail + "\n" + UploadQueryUtils.parseMsg;
          return false;
        }
      } else {
        counter++;
        if (counter > MAX_TRIAL) {
          totalAps.setTotalAps(count);
          error = context.getString(R.string.connect_error);
          return false;
        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
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

  public RankingObject getRanking() {
    return ranking;
  }

  public String getError() {
    return error;
  }
}
