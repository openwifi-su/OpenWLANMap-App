package su.openwifi.openwlanmap.service;

/**
 * Created by tali on 01.06.18.
 */

public class GeoData {
    private double latitude;
    private double longtitude;
    private double radius;
    private GeoData(){}
    private static GeoData geoData;
    public static GeoData getGeoData(){return geoData;}

    public boolean setGeoData(double latitude, double longtitude, double radius){
        return false;
    }
    public boolean setGeoData(double latitude, double longtitude){
        return false;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongtitude() {
        return longtitude;
    }
}
