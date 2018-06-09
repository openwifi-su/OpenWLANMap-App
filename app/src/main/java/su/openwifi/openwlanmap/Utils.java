package su.openwifi.openwlanmap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import java.text.SimpleDateFormat;

/**
 * Created by tali on 18.05.18.
 */

public class Utils {
    private Utils() {
    }

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy 'at' hh:mm:ss");
    public static final String OPEN_WIFI_MAP_URL = "http://owm.vreeken.net/map/";
    public static final String MAILING_LIST = "caothivananh98@gmail.com";
    public static final String URL_GET_LOCATION = "http://www.openwlanmap.org/getpos.php";
    public static final String URL_GET_LOCATION_NEW = "http://openwifi.su/api/v1/bssids/";
    //request code
    public static final int REQUEST_GPS = 100;
    public static final int REQUEST_WRITE = 102;

    public static void checkPermission(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_GPS);
        }
    }

    public static void checkGPS(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(context, context.getString(R.string.enable_gps), Toast.LENGTH_SHORT).show();
                //Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                //context.startActivity(intent);
            }
        }
    }
}
