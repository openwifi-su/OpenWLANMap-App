package su.openwifi.openwlanmap;

import java.io.Serializable;
import org.osmdroid.api.IGeoPoint;

public class Coordinate implements IGeoPoint {
  private double lat;
  private double lon;
  public Coordinate(double lat, double lon){
    this.lat = lat;
    this.lon = lon;
  }

  @Override
  public int getLatitudeE6() {
    return 0;
  }

  @Override
  public int getLongitudeE6() {
    return 0;
  }

  @Override
  public double getLatitude() {
    return this.lat;
  }

  @Override
  public double getLongitude() {
    return this.lon;
  }
}
