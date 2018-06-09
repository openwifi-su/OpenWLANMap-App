package su.openwifi.openwlanmap.service;

import android.net.wifi.ScanResult;

/**
 * Created by tali on 04.06.18.
 */

public class WifiFilterer {
    private WifiFilterer() {
    }

    public boolean isOpenWifi(ScanResult ap) {
        return ((!ap.capabilities.contains("WEP")) &&
                (!ap.capabilities.contains("WPA")) &&
                (!ap.capabilities.contains("TKIP")) &&
                (!ap.capabilities.contains("CCMP")) &&
                (!ap.capabilities.contains("PSK")));
    }

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

    public boolean isFreifunk(ScanResult ap) {
        return ap.SSID.toLowerCase().contains("freifunk");
    }

    public boolean isNoMapMarked(ScanResult ap) {
        return ap.SSID.endsWith("_nomap");
    }

    public boolean isMobilHotspot(ScanResult ap) {
        if (ap.SSID.startsWith("Audi") || ap.SSID.startsWith("Volkswagen")) {
            return true;
        }
        String lowerSSID = ap.SSID.toLowerCase();
        return (lowerSSID.contains("iphone") ||                 // mobile AP
                (lowerSSID.contains("ipad")) ||            // mobile AP
                (lowerSSID.contains("android")) ||         // mobile AP
                (lowerSSID.contains("motorola")) ||        // mobile AP
                (lowerSSID.contains("deinbus.de")) ||      // WLAN network on board of German bus
                (lowerSSID.contains("db ic bus")) ||       // WLAN network on board of German bus
                (lowerSSID.contains("fernbus")) ||         // WLAN network on board of German bus
                (lowerSSID.contains("flixbus")) ||         // WLAN network on board of German bus
                (lowerSSID.contains("flixmedia")) ||       // WLAN network on board of German bus
                (lowerSSID.contains("postbus")) ||         // WLAN network on board of bus line
                (lowerSSID.contains("megabus")) ||         // WLAN network on board of bus line
                (lowerSSID.contains("ecolines")) ||        // WLAN network on board of German bus
                (lowerSSID.contains("eurolines")) ||       // WLAN network on board of German bus
                (lowerSSID.contains("contiki-wifi")) ||    // WLAN network on board of bus
                (lowerSSID.contains("muenchenlinie")) ||   // WLAN network on board of bus
                (lowerSSID.contains("guest@ms ")) ||       // WLAN network on Hurtigruten ships
                (lowerSSID.contains("admin@ms ")) ||       // WLAN network on Hurtigruten ships
                (lowerSSID.contains("mobile hotspot")) ||  // e.g. BlackBerry devices
                (lowerSSID.contains("mobilewifi")) ||      // e.g. some Vodafone devices
                (lowerSSID.contains("portable hotspot")) ||// e.g. HTC devices
                (lowerSSID.contains("nokia lumia")) ||     // portable hotspot
                (lowerSSID.contains("telekom_ice")) ||     // WLAN network on DB trains
                (lowerSSID.contains("nsb_interakti")) ||   // WLAN network in NSB trains)
                (lowerSSID.contains("mobyclick"))          // hotspot in bus in Hamburg
        );
    }

    private boolean isNonInfrastructureNetwork(ScanResult result){
        return false;
    }

}
