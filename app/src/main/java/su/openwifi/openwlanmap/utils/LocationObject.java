package su.openwifi.openwlanmap.utils;

/**
 * This class describes a location with latitude, longitude and optional quality from the backend.
 */
public class LocationObject {
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
