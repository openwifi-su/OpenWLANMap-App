package su.openwifi.openwlanmap;

import android.content.AsyncTaskLoader;
import android.content.Context;
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
import java.util.ArrayList;
import java.util.List;
import org.osmdroid.api.IGeoPoint;
import su.openwifi.openwlanmap.service.ServiceController;
import su.openwifi.openwlanmap.utils.QueryUtils;

public class CoordinateLoader extends AsyncTaskLoader<List<IGeoPoint>> {
  private static final String URL_OWN_MAP = "http://www.openwlanmap.org/android/map.php";
  private static final String LOG_TAG = CoordinateLoader.class.getName();

  public CoordinateLoader(Context context) {
    super(context);
  }

  @Override
  protected void onStartLoading() {
    forceLoad();
  }

  @Override
  public List<IGeoPoint> loadInBackground() {
    URL url = null;
    try {
      url = new URL(URL_OWN_MAP);
    } catch (MalformedURLException e) {
      Log.e(LOG_TAG, "Error creating url");
    }
    if (url == null) {
      return null;
    }
    HttpURLConnection httpUrlConnection = null;
    InputStream ins = null;
    String content = ServiceController.ownId.toUpperCase() + "\n";
    try {
      httpUrlConnection = (HttpURLConnection) url.openConnection();
      httpUrlConnection.setRequestMethod("POST");
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
        httpUrlConnection.connect();
        int code = httpUrlConnection.getResponseCode();
        Log.e(LOG_TAG, "response code= " + code);
        if (code == HttpURLConnection.HTTP_OK) {
          ins = httpUrlConnection.getInputStream();
          List<IGeoPoint> list = new ArrayList<>();
          if (ins != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(ins, Charset.forName("UTF-8"));
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            try {
              while (true) {
                String latS = bufferedReader.readLine();
                if (latS == null) {
                  break;
                }
                String lonS = bufferedReader.readLine();
                if (lonS == null) {
                  break;
                }
                Double lat = Long.parseLong(latS) / 1000000.0;
                Double lon = Long.parseLong(lonS) / 1000000.0;
                if (lat >= -180
                    && lat <= 180
                    && lon >= -90
                    && lon <= 90
                    && lat != 0.0
                    && lon != 0.0) {
                  list.add(new Coordinate(lat, lon));
                }
              }
              return list;
            } catch (Exception e) {
              //Do nothing
            }
          }
        }
      }
    } catch (Exception e) {
      //Do nothing
    } finally {
      if (ins != null) {
        try {
          ins.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (httpUrlConnection != null) {
        httpUrlConnection.disconnect();
      }
    }

    return null;
  }
}
