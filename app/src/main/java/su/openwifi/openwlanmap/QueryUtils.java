package su.openwifi.openwlanmap;

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
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tali on 06.06.18.
 */

public class QueryUtils {
  private static final String LOG_TAG = QueryUtils.class.getSimpleName();

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
    String response = "";
    if (!bssids.isEmpty()) {
      StringBuilder builder = new StringBuilder();
      for (String bssid : bssids) {
        builder.append(bssid);
        builder.append("\r\n");
      }
      String content = builder.toString();
      URL url = create(Utils.URL_GET_LOCATION);
      Log.i(LOG_TAG, "creating url successfully");
      response = makeHttpRequest(url, "POST", content);
    }
    Log.i(LOG_TAG, "Response from server " + response);
    response = response.trim();
    if (!TextUtils.isEmpty(response)
        && response.contains("result=")
        && response.contains("quality=")
        && response.contains("lat=")
        && response.contains("lon=")) {
      String[] splits = response.split("result=");
      splits = splits[1].split("quality=");
      int result = Integer.parseInt(splits[0]);
      if (result > 0) {
        locator = new LocationObject();
        splits = splits[1].split("lat=");
        locator.quality = (short) Integer.parseInt(splits[0]);
        splits = splits[1].split("lon=");
        locator.lat = Double.parseDouble(splits[0]);
        locator.lon = Double.parseDouble(splits[1]);
        Log.i(LOG_TAG, "Setting lat lon ..." + locator.lat + "-" + locator.lon);
      }
    }
    return locator;
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
      String urlString = Utils.URL_GET_LOCATION_NEW + content.substring(0, content.length());
      Log.i(LOG_TAG, urlString);
      URL url = create(urlString);
      Log.i(LOG_TAG, "creating url successfully");
      response = makeHttpRequest(url, "GET", null);
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

  private static String makeHttpRequest(URL url, String method, String content) {
    String response = "";
    if (url == null) {
      return response;
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
        response = streamToString(ins);
        Log.i(LOG_TAG, "Successfully getting " + response);
      }
    } catch (IOException e) {
      Log.e(LOG_TAG, "Error getting geolocation");
    } finally {
      //free resources
      if (httpUrlConnection != null) {
        httpUrlConnection.disconnect();
      }
      if (ins != null) {
        try {
          ins.close();
        } catch (IOException e) {
          Log.e(LOG_TAG, "Error closing inputstream");
        }
      }
    }
    return response;
  }

  private static String streamToString(InputStream ins) {
    StringBuilder builder = new StringBuilder();
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
    return builder.toString();
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
    }

    public LocationObject() {
    }
  }
}
