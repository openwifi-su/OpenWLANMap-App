package su.openwifi.openwlanmap.service;

import java.util.Observable;

public class TotalApWrapper extends Observable {
  private long totalAps;

  public TotalApWrapper(long start) {
    this.totalAps = start;
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
