package su.openwifi.openwlanmap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;
import java.text.SimpleDateFormat;

/**
 * Created by tali on 18.05.18.
 */

public class Utils {
  public static final SimpleDateFormat SIMPLE_DATE_FORMAT
      = new SimpleDateFormat("dd.MM.yyyy 'at' hh:mm:ss");
  public static final String OPEN_WIFI_MAP_URL = "http://owm.vreeken.net/map/";
  public static final String MAILING_LIST = "caothivananh98@gmail.com";
  //request code
  public static final int REQUEST_GPS = 100;
  public static final int REQUEST_WRITE = 101;
  public static final int REQUEST_OVERLAY = 102;

  private Utils() {
  }

  /**
   * This methods checks the location permission.
   *
   * @param activity : the activity where the onPermissionResult should be implemented
   * @return true if the permission is already granted, otherwise false and ask for permission
   */
  public static boolean checkLocationPermission(Activity activity) {
    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          activity,
          new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
          REQUEST_GPS);
      return false;
    } else {
      return true;
    }
  }

  /**
   * This methods checks if the gps signal is available, if not pop up a message.
   *
   * @param context : context of the application
   */
  public static void checkGpsSignal(Context context) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      LocationManager manager = (LocationManager) context.getSystemService(
          Context.LOCATION_SERVICE);
      if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        Toast.makeText(context, context.getString(R.string.enable_gps), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
      }
    }
  }

  public static void checkDrawOverlayPermission(Activity activity) {
    /** check if we already  have permission to draw over other apps */
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (!Settings.canDrawOverlays(activity)) {
        /** if not construct intent to request permission */
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + activity.getPackageName()));
        /** request permission via start activity for result */
        activity.startActivityForResult(intent, REQUEST_OVERLAY);
      }
    }
  }

  public static boolean checkBssid(String teamBssid) {
    teamBssid = teamBssid.trim();
    return teamBssid.length() !=12 || !teamBssid.matches("^[0-9a-fA-F]+$");
  }
}
