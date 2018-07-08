package su.openwifi.openwlanmap.database;

/**
 * This class contains the defined table col name and
 * all constant needed for the database.
 */
public class DatabaseConstant {
  //database
  public static final String TABLE_NAME = "access_point_table";
  public static final String UPLOAD_MAX = "3";
  //col
  public static final String COLUMN_ID = "id";
  public static final String COLUMN_SSID = "ssid";
  public static final String COLUMN_RSSID = "rssid";
  public static final String COLUMN_FREQ = "frequency";
  public static final String COLUMN_CHANNEL = "channel";
  public static final String COLUMN_CAP = "capacities";
  public static final String COLUMN_LAT = "latitude";
  public static final String COLUMN_LON = "longitude";
  public static final String COLUMN_UPDATE = "to_update";

  private DatabaseConstant() {}
}
