package su.openwifi.openwlanmap.utils;

import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

public class LocationQueryUtils {
  private static final String LOG_TAG = LocationQueryUtils.class.getName();
  private static final String URL_GET_LOCATION = "http://www.openwlanmap.org/getpos.php";
  private static final String URL_GET_LOCATION_NEW = "http://openwifi.su/api/v1/bssids/";

  private LocationQueryUtils() {
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
      URL url = QueryUtils.create(URL_GET_LOCATION);
      Log.i(LOG_TAG, "creating url successfully");
      InputStream ins = QueryUtils.makeHttpRequest(url, "POST", content);
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
      URL url = QueryUtils.create(urlString);
      Log.i(LOG_TAG, "creating url successfully");
      InputStream ins = QueryUtils.makeHttpRequest(url, "POST", null);
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

  private static LocationObject streamToLocationObject(InputStream ins) {
    LocationObject locationObject = new LocationObject();
    if (ins != null) {
      InputStreamReader inputStreamReader = new InputStreamReader(ins, Charset.forName("UTF-8"));
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      try {
        locationObject.result = Integer.parseInt(bufferedReader.readLine().substring(7));
        if (locationObject.result < 1) {
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
}
