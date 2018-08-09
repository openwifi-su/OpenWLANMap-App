package su.openwifi.openwlanmap;

import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_CAP;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_CHANNEL;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_FREQ;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_ID;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_LAT;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_LON;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_RSSID;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_SSID;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_UPDATE;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import su.openwifi.openwlanmap.database.DatabaseConstant;

/**
 * This class describes a access point object.
 */
@Entity(tableName = DatabaseConstant.TABLE_NAME)
public class AccessPoint implements Parcelable {
  public static final Creator<AccessPoint> CREATOR = new Creator<AccessPoint>() {
    @Override
    public AccessPoint createFromParcel(Parcel in) {
      return new AccessPoint(in);
    }

    @Override
    public AccessPoint[] newArray(int size) {
      return new AccessPoint[size];
    }
  };
  @PrimaryKey
  @NonNull
  @ColumnInfo(name = COLUMN_ID)
  private String bssid;
  @ColumnInfo(name = COLUMN_SSID)
  private String ssid;
  @ColumnInfo(name = COLUMN_RSSID)
  private int rssid;
  @Ignore
  private long timestamp;
  @ColumnInfo(name = COLUMN_FREQ)
  private double frequency;
  @ColumnInfo(name = COLUMN_CHANNEL)
  private int channel;
  @ColumnInfo(name = COLUMN_CAP)
  private String capabilities;
  @ColumnInfo(name = COLUMN_LAT)
  private double lat;
  @ColumnInfo(name = COLUMN_LON)
  private double lon;
  @ColumnInfo(name = COLUMN_UPDATE)
  private boolean toUpdate;

  /**
   * Constructor of Access Point object.
   *
   * @param bssid        : baisc service set teamId (12 characters mac address)
   * @param ssid         : service set indentifier (name of the wifi access point)
   * @param rssid        : receive signal strength indicator
   * @param timestamp    : first seen timestamp
   * @param frequency    : wifi frequency
   * @param channel      : wifi channel
   * @param capabilities : encrypted methods etc
   * @param lat          : latitude of the access point
   * @param lon          : longitude of the access point
   */
  public AccessPoint(String bssid, String ssid, int rssid,
                     long timestamp, double frequency, int channel,
                     String capabilities, double lat, double lon, boolean toUpdate) {
    this.bssid = bssid;
    this.ssid = ssid;
    this.rssid = rssid;
    this.timestamp = timestamp;
    this.frequency = frequency;
    this.channel = channel;
    this.capabilities = capabilities;
    this.lat = lat;
    this.lon = lon;
    this.toUpdate = toUpdate;
  }

  /**
   * This constructor is for room db to use to recreate the object from db.
   * @param bssid        : baisc service set teamId (12 characters mac address)
   * @param ssid         : service set indentifier (name of the wifi access point)
   * @param rssid        : receive signal strength indicator
   * @param frequency    : wifi frequency
   * @param channel      : wifi channel
   * @param capabilities : encrypted methods etc
   * @param lat          : latitude of the access point
   * @param lon          : longitude of the access point
   * @param toUpdate     : if the ap should be updated or deleted from backend
   */
  public AccessPoint(String bssid, String ssid, int rssid, double frequency, int channel,
                     String capabilities, double lat, double lon, boolean toUpdate) {
    this.bssid = bssid;
    this.ssid = ssid;
    this.rssid = rssid;
    this.frequency = frequency;
    this.channel = channel;
    this.capabilities = capabilities;
    this.lat = lat;
    this.lon = lon;
    this.toUpdate = toUpdate;
  }

  protected AccessPoint(Parcel in) {
    bssid = in.readString();
    ssid = in.readString();
    rssid = in.readInt();
    timestamp = in.readLong();
    frequency = in.readDouble();
    channel = in.readInt();
    capabilities = in.readString();
    lat = in.readDouble();
    lon = in.readDouble();
  }

  public String getBssid() {
    return bssid;
  }

  public void setBssid(String bssid) {
    this.bssid = bssid;
  }

  public String getSsid() {
    return ssid;
  }

  public void setSsid(String ssid) {
    this.ssid = ssid;
  }

  public int getRssid() {
    return rssid;
  }

  public void setRssid(int rssid) {
    this.rssid = rssid;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public double getFrequency() {
    return frequency;
  }

  public void setFrequency(double frequency) {
    this.frequency = frequency;
  }

  public int getChannel() {
    return channel;
  }

  public void setChannel(int channel) {
    this.channel = channel;
  }

  public String getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(String capabilities) {
    this.capabilities = capabilities;
  }

  public double getLat() {
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLon() {
    return lon;
  }

  public void setLon(double lon) {
    this.lon = lon;
  }

  public boolean isToUpdate() {
    return toUpdate;
  }

  public void setToUpdate(boolean toUpdate) {
    this.toUpdate = toUpdate;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    AccessPoint otherAp = (AccessPoint) obj;
    return this.bssid.compareToIgnoreCase(otherAp.bssid) == 0;
  }

  @Override
  public int hashCode() {
    return bssid.hashCode();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeString(bssid);
    parcel.writeString(ssid);
    parcel.writeInt(rssid);
    parcel.writeLong(timestamp);
    parcel.writeDouble(frequency);
    parcel.writeInt(channel);
    parcel.writeString(capabilities);
    parcel.writeDouble(lat);
    parcel.writeDouble(lon);
  }

  @Override
  public String toString() {
    return "bssid=" + bssid
        + "\n ssid=" + ssid
        + "\n rssid=" + rssid
        + "\n lat= " + lat
        + "\n lon=" + lon
        + "\n others= " + "-" + channel + "-" + frequency + "-" + capabilities;
  }
}
