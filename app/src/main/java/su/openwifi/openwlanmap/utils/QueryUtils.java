package su.openwifi.openwlanmap.utils;

import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by tali on 06.06.18.
 */

public class QueryUtils {
  private static final String LOG_TAG = QueryUtils.class.getSimpleName();
  public static String requestDetail;

  private QueryUtils() {
  }

  /**
   * This method does a http unsecure connection.
   *
   * @param urlString : url to connect
   * @param method    : http method
   * @param content   : payload
   * @return an inputstream
   */
  public static InputStream makeHttpRequest(String urlString, String method, String content) {
    URL url = QueryUtils.create(urlString);
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
      requestDetail = code + " (" + httpUrlConnection.getResponseMessage() + ")";
      Log.e(LOG_TAG, "response code= " + code);
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

  /**
   * This method helps create an URL object out of an url String.
   *
   * @param urlString : url in String
   * @return URL object
   */
  private static URL create(String urlString) {
    URL url = null;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      Log.e(LOG_TAG, "Error creating url");
    }
    return url;
  }

}
