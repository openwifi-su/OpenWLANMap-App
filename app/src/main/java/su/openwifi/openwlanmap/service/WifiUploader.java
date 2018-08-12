package su.openwifi.openwlanmap.service;

import android.content.Context;
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
  private static final String LOG_TAG = WifiUploader.class.getName();
  private Context context;
  private TotalApWrapper totalAps;
  private RankingObject ranking;
  private String msg;

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
    ranking = null;
    long total = totalAps.getTotalAps();
    msg = context.getString(R.string.upload_sum) + "\n";
    long count = MyDatabase.getInstance(context)
        .getAccessPointDao()
        .countEntries();
    while (count > 0) {
      Log.i(LOG_TAG, "READING DATABASE......................db=" + count);
      List<AccessPoint> uploadEntries = MyDatabase.getInstance(context)
          .getAccessPointDao()
          .getAllDataEntriesToUpload();
      Log.i(LOG_TAG, "Uploading now = " + uploadEntries.size());
      //Upload
      RankingObject response = UploadQueryUtils.uploadOpenWifi(uploadEntries,
          ServiceController.ownId.toUpperCase(),
          ServiceController.teamId.toUpperCase(),
          ServiceController.tag,
          ServiceController.mode);
      msg += String.format("%s %d / %d : ", context.getString(R.string.upload_m),
          uploadEntries.size(),
          total);
      if (response != null) {
        //Delete
        Log.e(LOG_TAG, "Getting back from upload response");
        msg += context.getString(R.string.ok) + "\n";
        if (ranking == null) {
          ranking = response;
        } else {
          ranking.uploadedRank = response.uploadedRank;
          ranking.uploadedCount = response.uploadedCount;
          ranking.delAps += response.delAps;
          ranking.newAps += response.newAps;
          ranking.updAps += response.updAps;
          ranking.newPoints += response.newPoints;
        }
        MyDatabase.getInstance(context)
            .getAccessPointDao()
            .delete(uploadEntries);
        count = MyDatabase.getInstance(context)
            .getAccessPointDao()
            .countEntries();
      } else {
        msg += context.getString(R.string.upload_error)
            + " Http code = " + QueryUtils.requestDetail
            + "," + UploadQueryUtils.parseMsg
            + "\n";
        count = MyDatabase.getInstance(context)
            .getAccessPointDao()
            .countEntries();
        break;
      }
    }
    if (count < total) {
      totalAps.setTotalAps(count);
    }
    Log.i(LOG_TAG, "Done uploading......");
    return ranking != null;
  }

  public RankingObject getRanking() {
    return ranking;
  }

  public String getMsg() {
    return msg;
  }
}
