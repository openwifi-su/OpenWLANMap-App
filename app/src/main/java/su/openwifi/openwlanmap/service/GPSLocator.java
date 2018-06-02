package su.openwifi.openwlanmap.service;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import java.util.logging.Logger;

import su.openwifi.openwlanmap.Utils;

/**
 * Created by tali on 01.06.18.
 */

public class GPSLocator {
    private Logger LOG = Logger.getLogger(GPSLocator.class.getName());
    private Context context;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private GeoData geo;

    public GPSLocator(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.locationListener = new GPSLocationListener();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0, locationListener
            );
        } else {
            Utils.checkPermission((Activity) context);
        }
    }

    private class GPSLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            String out = "Current location is " +
                    location.getLatitude() + "-" + location.getLongitude();
            Toast.makeText(context, out, Toast.LENGTH_SHORT).show();
            getGeo(location.getLatitude(),location.getLongitude());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    }

    protected void getGeo(double lat, double lon){

    }
}
