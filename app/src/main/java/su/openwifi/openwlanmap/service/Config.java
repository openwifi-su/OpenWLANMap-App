package su.openwifi.openwlanmap.service;

/**
 * This class contains all the config needed to control the
 * scanning service.
 */
public class Config {
  public enum MODE {
    SCAN_MODE, UPLOAD_MODE, SUSPEND_MODE, KILL_MODE
  }
  private static MODE mode = MODE.SCAN_MODE;

  public static synchronized MODE getMode() {
    return mode;
  }

  public static synchronized void setMode(MODE modeToSet) {
    mode = modeToSet;
  }
}
