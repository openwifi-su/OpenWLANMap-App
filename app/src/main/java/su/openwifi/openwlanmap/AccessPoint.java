package su.openwifi.openwlanmap;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class describes a access point object.
 */

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
  private String bssid;
  private String ssid;
  private int rssid;
  private long timestamp;
  private double frequency;
  private int channel;
  private String capabilities;
  private double lat;
  private double lon;
  private boolean seeing;

  /**
   * Constructor of Access Point object.
   * @param bssid : baisc service set id (12 characters mac address)
   * @param ssid : service set indentifier (name of the wifi access point)
   * @param rssid : receive signal strength indicator
   * @param timestamp : first seen timestamp
   * @param frequency : wifi frequency
   * @param channel : wifi channel
   * @param capabilities : encrypted methods etc
   * @param lat : latitude of the access point
   * @param lon : longitude of the access point
   */
  public AccessPoint(String bssid, String ssid, int rssid,
                     long timestamp, double frequency, int channel,
                     String capabilities, double lat, double lon) {
    this.bssid = bssid;
    this.ssid = ssid;
    this.rssid = rssid;
    this.timestamp = timestamp;
    this.frequency = frequency;
    this.channel = channel;
    this.capabilities = capabilities;
    this.lat = lat;
    this.lon = lon;
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
    seeing = in.readByte() != 0;
  }

  public String getBssid() {
    return bssid;
  }

  public String getSsid() {
    return ssid;
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

  public int getChannel() {
    return channel;
  }

  public String getCapabilities() {
    return capabilities;
  }

  public double getLat() {
    return lat;
  }

  public double getLon() {
    return lon;
  }

  public void setLocation(double lat, double lon) {
    this.lat = lat;
    this.lon = lon;
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
    parcel.writeByte((byte) (seeing ? 1 : 0));
  }
}
