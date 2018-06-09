package su.openwifi.openwlanmap.service;

import android.net.wifi.ScanResult;

/**
 * Created by tali on 04.06.18.
 */

public class WifiFilterer {
  public static final String[] mobilHotspot = {
      "iphone",              // mobile AP
      "ipad",            // mobile AP
      "android",         // mobile AP
      "motorola",       // mobile AP
      "deinbus.de",     // WLAN network on board of German bus
      "db ic bus",      // WLAN network on board of German bus
      "fernbus",        // WLAN network on board of German bus
      "flixbus",         // WLAN network on board of German bus
      "flixmedia",       // WLAN network on board of German bus
      "postbus",        // WLAN network on board of bus line
      "megabus",        // WLAN network on board of bus line
      "ecolines",       // WLAN network on board of German bus
      "eurolines",      // WLAN network on board of German bus
      "contiki-wifi",  // WLAN network on board of bus
      "muenchenlinie",   // WLAN network on board of bus
      "guest@ms ",      // WLAN network on Hurtigruten ships
      "admin@ms ",      // WLAN network on Hurtigruten ships
      "mobile hotspot",  // e.g. BlackBerry devices
      "mobilewifi",     // e.g. some Vodafone devices
      "portable hotspot", // e.g. HTC devices
      "nokia lumia",    // portable hotspot
      "telekom_ice",    // WLAN network on DB trains
      "nsb_interakti",   // WLAN network in NSB trains)
      "mobyclick"         // hotspot in bus in Hamburg
  };

  private WifiFilterer() {
  }

  /**
   * This methods checks if the ScanResult wifi is an open wifi.
   * @param ap : ScanResult object
   * @return true if it is open wifi, otherwise false
   */
  public boolean isOpenWifi(ScanResult ap) {
    return ((!ap.capabilities.contains("WEP"))
        && (!ap.capabilities.contains("WPA"))
        && (!ap.capabilities.contains("TKIP"))
        && (!ap.capabilities.contains("CCMP"))
        && (!ap.capabilities.contains("PSK")));
  }

  /**
   * This methods checks if the ScanResult wifi is free hotspot.
   * @param ap : ScanResult object
   * @return true if it is free hotspot, otherwise false
   */
  public boolean isFreeHotspot(ScanResult ap) {
    if (isOpenWifi(ap)) {
      String ssid = ap.SSID.toLowerCase();
      if (ssid.contains("free-hotspot.com")
          || ssid.contains("the cloud")) {
        return true;
      }
    }
    return false;
  }

  /**
   * This methods checks if the ScanResult wifi is from freifunk.
   * @param ap : ScanResult object
   * @return true if it is from freifunk, otherwise false
   */
  public boolean isFreifunk(ScanResult ap) {
    return ap.SSID.toLowerCase().contains("freifunk");
  }

  /**
   * This methods checks if the ScanResult wifi is marked with _nomap.
   * @param ap : ScanResult object
   * @return true if it is marked, otherwise false
   */
  public boolean isNoMapMarked(ScanResult ap) {
    return ap.SSID.endsWith("_nomap");
  }

  /**
   * This methods checks if the ScanResult wifi is a mobil hotspot.
   * @param ap : ScanResult object
   * @return true if it is a mobil hotspot, otherwise false
   */
  public boolean isMobilHotspot(ScanResult ap) {
    if (ap.SSID.startsWith("Audi") || ap.SSID.startsWith("Volkswagen")) {
      return true;
    }
    String lowerSsid = ap.SSID.toLowerCase();
    for (String hotspot: mobilHotspot) {
      if (lowerSsid.contains(hotspot)) {
        return true;
      }
    }
    return false;
  }

  /**
   * This methods checks if the ScanResult wifi is a non-infrastructure wifi access point.
   * @param result : ScanResult object
   * @return true if it is non-infrastructure access point, otherwise false
   */
  private boolean isNonInfrastructureNetwork(ScanResult result) {
    return false;
  }

}
