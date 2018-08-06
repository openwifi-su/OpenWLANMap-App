package su.openwifi.openwlanmap.utils;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import su.openwifi.openwlanmap.AccessPoint;

public class UploadQueryUtils {
  private static final String LOG_TAG = UploadQueryUtils.class.getName();
  private static final String URL_UPLOAD_OPEN_WIFI = "http://www.openwifi.su/android/upload.php";
  private static final String URL_UPLOAD_VIRTUAL_WORLD = "http://tracker.virtualworlds.de/android/upload.php";

  private UploadQueryUtils() {
  }

  public static RankingObject uploadOpenWifi(List<AccessPoint> uploadEntries,
                                             String mac,
                                             String teamTag,
                                             int mode) {
    return uploadData(uploadEntries, mac, teamTag, mode, URL_UPLOAD_OPEN_WIFI);
  }

  /*
  public static RankingObject uploadVirtualWorld(List<AccessPoint> uploadEntries,
                                                 String mac,
                                                 String teamTag,
                                                 int mode) {
    return uploadData(uploadEntries, mac, teamTag, mode, URL_UPLOAD_VIRTUAL_WORLD);
  }
  */

  /**
   * This method uploads a list of access point to backend.
   *
   * @param uploadEntries : a list of access point
   * @param mac           : own generated bssid
   * @param teamTag       : team tag as user defines
   * @param mode          : mode for public data, as well as map
   * @return a RankingObject
   */
  private static RankingObject uploadData(
      List<AccessPoint> uploadEntries,
      String mac,
      String teamTag,
      int mode,
      String urlString) {
    Log.e(LOG_TAG, String.format("Value to upload=%d mac=%s tag=%s mode =%d",uploadEntries.size(),mac,teamTag,mode));
    InputStream ins = QueryUtils
        .makeHttpRequest(
            urlString,
            "POST",
            prepareUploading(uploadEntries, mac, teamTag, mode));
    RankingObject rankingObject = streamToRankingObject(ins);
    Log.i(LOG_TAG, "Finish uploaded");
    if (ins != null) {
      try {
        ins.close();
      } catch (IOException e) {
        Log.e(LOG_TAG, "Error closing inputstream");
      }
    }
    return rankingObject;
  }

  private static String prepareUploading(
      List<AccessPoint> uploadEntries,
      String mac,
      String tag,
      int mode) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(mac + "\n");
    stringBuilder.append("T\t");
    stringBuilder.append(tag + "\n");
    stringBuilder.append("E\t");
    stringBuilder.append(mac + "\n");
    stringBuilder.append("F\t");
    stringBuilder.append(mode + "\n");
    for (AccessPoint ap : uploadEntries) {
      if (ap.isToUpdate()) {
        stringBuilder.append(1 + "\t");
        stringBuilder.append(ap.getBssid() + "\t");
        stringBuilder.append(ap.getLat() + "\t");
        stringBuilder.append(ap.getLon() + "\t");
        stringBuilder.append("\n");
      } else {
        stringBuilder.append("U\t");
        stringBuilder.append(ap.getBssid());
        stringBuilder.append("\n");
      }
    }
    return stringBuilder.toString();
  }

  private static RankingObject streamToRankingObject(InputStream ins) {
    if (ins != null) {
      RankingObject rankingObject = new RankingObject();
      InputStreamReader inputStreamReader = new InputStreamReader(ins, Charset.forName("UTF-8"));
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      try {
        rankingObject.remoteVersion = Integer.parseInt(bufferedReader.readLine());
        rankingObject.uploadedCount = Integer.parseInt(bufferedReader.readLine());
        rankingObject.uploadedRank = Integer.parseInt(bufferedReader.readLine());
        rankingObject.newAps = Integer.parseInt(bufferedReader.readLine());
        rankingObject.updAps = Integer.parseInt(bufferedReader.readLine());
        rankingObject.delAps = Integer.parseInt(bufferedReader.readLine());
        rankingObject.newPoints = Integer.parseInt(bufferedReader.readLine());
        return rankingObject;
      } catch (Exception e) {
        Log.e(LOG_TAG, "Error reading inputstream");
      }
    }
    return null;
  }
}
