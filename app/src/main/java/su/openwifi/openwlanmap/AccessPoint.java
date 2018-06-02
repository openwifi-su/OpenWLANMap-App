package su.openwifi.openwlanmap;

/**
 * Created by tali on 15.05.18.
 */

public class AccessPoint {
    private String bssid;
    private String ssid;
    private int rssid;
    private long timestamp;
    private double frequency;
    private int channel;
    private String encrypted;

    public AccessPoint(String bssid, String ssid, int rssid, long timestamp, double frequency, int channel, String encrypted) {
        this.bssid = bssid;
        this.ssid = ssid;
        this.rssid = rssid;
        this.timestamp = timestamp;
        this.frequency = frequency;
        this.channel = channel;
        this.encrypted = encrypted;
    }

    public AccessPoint(String bssid,String ssid, int rssid, long timestamp, double frequency) {
        this.bssid = bssid;
        this.ssid = ssid;
        this.rssid = rssid;
        this.timestamp = timestamp;
        this.frequency = frequency;
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

    public long getTimestamp() {
        return timestamp;
    }

    public double getFrequency() {
        return frequency;
    }

    public int getChannel() {
        return channel;
    }

    public String getEncrypted() {
        return encrypted;
    }
}
