package su.openwifi.openwlanmap;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tali on 06.06.18.
 */

public class QueryUtils {
  private static final String LOG_TAG = QueryUtils.class.getSimpleName();
  private static final String URL_GET_LOCATION = "http://www.openwlanmap.org/getpos.php";
  private static final String URL_GET_LOCATION_NEW = "http://openwifi.su/api/v1/bssids/";
  private static final String URL_UPLOAD = "http://www.openwifi.su/android/upload.php";

  private QueryUtils() {
  }

  /**
   * This methods fetchs the location data in case of no gps signal from the old api.
   *
   * @param bssids : a list of surrounding wifi access points
   * @return a LocationObject contains latitude, longitude and result's quality
   */
  public static LocationObject fetchLocationOld(Set<String> bssids) {
    LocationObject locator = null;
    if (!bssids.isEmpty()) {
      StringBuilder builder = new StringBuilder();
      for (String bssid : bssids) {
        builder.append(bssid);
        builder.append("\r\n");
      }
      String content = builder.toString();
      URL url = create(URL_GET_LOCATION);
      Log.i(LOG_TAG, "creating url successfully");
      InputStream ins = makeHttpRequest(url, "POST", content);
      locator = streamToLocationObject(ins);
      Log.i(LOG_TAG, "Successfully getting location");
      if (ins != null) {
        try {
          ins.close();
        } catch (IOException e) {
          Log.e(LOG_TAG, "Error closing inputstream");
        }
      }
    }
    return locator != null && locator.result > 0 ? locator : null;
  }

  /**
   * This methods fetchs the location data in case of no gps signal from the new api.
   *
   * @param bssids : a list of surrounding wifi access points
   * @return a LocationObject contains latitude, longitude
   */
  public static LocationObject fetchLocationNew(Set<String> bssids) {
    LocationObject locator = null;
    String response = "";
    if (!bssids.isEmpty()) {
      StringBuilder builder = new StringBuilder();
      for (String bssid : bssids) {
        builder.append(bssid);
        builder.append(",");
      }
      String content = builder.toString();
      String urlString = URL_GET_LOCATION_NEW + content.substring(0, content.length());
      Log.i(LOG_TAG, urlString);
      URL url = create(urlString);
      Log.i(LOG_TAG, "creating url successfully");
      InputStream ins = makeHttpRequest(url, "POST", null);
      response = streamToString(ins);
      Log.i(LOG_TAG, "Successfully getting " + response);
      if (ins != null) {
        try {
          ins.close();
        } catch (IOException e) {
          Log.e(LOG_TAG, "Error closing inputstream");
        }
      }
    }
    Log.i(LOG_TAG, "Response from server " + response);
    response = response.trim();
    if (!TextUtils.isEmpty(response)) {
      try {
        JSONObject jo = new JSONObject(response);
        if (jo.has("lat") && jo.has("lon")) {
          locator = new LocationObject(jo.getDouble("lat"), jo.getDouble("lon"));
        }
      } catch (JSONException e) {
        return locator;
      }
    }
    return locator;
  }

  /**
   * This method uploads a list of access point to backend.
   * @param uploadEntries : a list of access point
   * @param mac : own generated bssid
   * @param teamTag : team tag as user defines
   * @param mode : mode for public data, as well as map
   * @return a RankingObject
   */
  public static RankingObject uploadData(
      List<AccessPoint> uploadEntries,
      String mac,
      String teamTag,
      int mode) {
    URL url = create(URL_UPLOAD);
    InputStream ins = makeHttpRequest(url, "POST", prepareUploading(uploadEntries, mac, teamTag, mode));
    RankingObject rankingObject = streamToRankingObject(ins);
    Log.i(LOG_TAG, "Successfully getting ranking back");
    if (ins != null) {
      try {
        ins.close();
      } catch (IOException e) {
        Log.e(LOG_TAG, "Error closing inputstream");
      }
    }
    return rankingObject;
  }

  private static String prepareUploading(List<AccessPoint> uploadEntries, String mac, String tag, int mode) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(mac + "\n");
    stringBuilder.append("T\t");
    stringBuilder.append(tag +"\n");
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

  private static InputStream makeHttpRequest(URL url, String method, String content) {
    if (url == null) {
      return null;
    }
    HttpURLConnection httpUrlConnection = null;
    InputStream ins = null;
    try {
      httpUrlConnection = (HttpURLConnection) url.openConnection();
      httpUrlConnection.setRequestMethod(method);
      httpUrlConnection.setReadTimeout(10000);
      httpUrlConnection.setConnectTimeout(15000);
      if (!TextUtils.isEmpty(content)) {
        httpUrlConnection.addRequestProperty(
            "Content-Type",
            "application/x-www-form-urlencoded, "
                + "*.*");
        httpUrlConnection.addRequestProperty("Content-Length", "" + content.length());
        BufferedOutputStream os = new BufferedOutputStream(httpUrlConnection.getOutputStream());
        os.write(content.getBytes(), 0, content.length());
        os.flush();
        os.close();
      }
      httpUrlConnection.connect();
      int code = httpUrlConnection.getResponseCode();
      if (code == HttpURLConnection.HTTP_OK) {
        ins = httpUrlConnection.getInputStream();
      }
    } catch (IOException e) {
      Log.e(LOG_TAG, "Error getting geolocation");
    } finally {
      //free resources
      if (httpUrlConnection != null) {
        httpUrlConnection.disconnect();
      }
    }
    return ins;
  }

  private static String streamToString(InputStream ins) {
    StringBuilder builder = new StringBuilder();
    if (ins != null) {
      InputStreamReader inputStreamReader = new InputStreamReader(ins, Charset.forName("UTF-8"));
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      try {
        String line = bufferedReader.readLine();
        while (line != null) {
          builder.append(line);
          line = bufferedReader.readLine();
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, "Error reading inputstream");
      }
    }
    return builder.toString();
  }

  private static LocationObject streamToLocationObject(InputStream ins) {
    LocationObject locationObject = new LocationObject();
    if (ins != null) {
      InputStreamReader inputStreamReader = new InputStreamReader(ins, Charset.forName("UTF-8"));
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      try {
        locationObject.result = Integer.parseInt(bufferedReader.readLine().substring(7));
        if(locationObject.result <1){
          return null;
        }
        locationObject.quality = (short) Integer.parseInt(bufferedReader.readLine().substring(8));
        locationObject.lat = Double.parseDouble(bufferedReader.readLine().substring(4));
        locationObject.lon = Double.parseDouble(bufferedReader.readLine().substring(4));
      } catch (IOException e) {
        Log.e(LOG_TAG, "Error reading inputstream");
      }
      return locationObject;
    }
    return null;
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
      } catch (IOException e) {
        Log.e(LOG_TAG, "Error reading inputstream");
      }
      return rankingObject;
    }
    return null;
  }

  private static URL create(String urlString) {
    URL url = null;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      Log.e(LOG_TAG, "Error creating url");
    }
    return url;
  }

  /**
   * This class describes a location with latitude, longitude and optional quality from the backend.
   */
  public static class LocationObject {
    public double lat;
    public double lon;
    public short quality;
    public int result;

    /**
     * Constructor of LocationObject.
     *
     * @param lat : latitude
     * @param lon : longitude
     */
    public LocationObject(double lat, double lon) {
      this.lat = lat;
      this.lon = lon;
      this.quality = (short) 0;
      this.result = 0;
    }

    public LocationObject() {
    }
  }

  /**
   * This class describes a ranking result from backend.
   */
  public static class RankingObject implements Parcelable {
    public int remoteVersion;
    public int uploadedCount;
    public int uploadedRank;
    public int newAps;
    public int updAps;
    public int delAps;
    public int newPoints;

    public RankingObject() {
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeInt(uploadedRank);
      dest.writeInt(uploadedCount);
      dest.writeInt(newAps);
      dest.writeInt(updAps);
      dest.writeInt(delAps);
      dest.writeInt(newPoints);
    }

    protected RankingObject(Parcel in) {
      uploadedRank = in.readInt();
      uploadedCount = in.readInt();
      newAps = in.readInt();
      updAps = in.readInt();
      delAps = in.readInt();
      newPoints = in.readInt();
    }

    public static final Creator<RankingObject> CREATOR = new Creator<RankingObject>() {

      @Override
      public RankingObject createFromParcel(Parcel source) {
        return new RankingObject(source);
      }

      @Override
      public RankingObject[] newArray(int size) {
        return new RankingObject[0];
      }
    };
  }
}
