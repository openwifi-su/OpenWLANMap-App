package su.openwifi.openwlanmap.service;

import java.util.Observable;

public class TotalAPWrapper extends Observable {
  private long totalAps;

  public TotalAPWrapper() {
  }

  /**
   * Setter.
   * @param totalAps : total aps in database
   */
  public void setTotalAps(long totalAps) {
    this.totalAps = totalAps;
    setChanged();
    notifyObservers();
  }

  public long getTotalAps() {
    return this.totalAps;
  }
}
