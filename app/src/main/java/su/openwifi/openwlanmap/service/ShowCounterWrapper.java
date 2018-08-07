package su.openwifi.openwlanmap.service;

import java.util.Observable;

public class ShowCounterWrapper extends Observable {
  private boolean shouldShow;

  public ShowCounterWrapper(boolean start) {
    this.shouldShow = start;
  }

  public void setShouldShow(boolean shouldShow) {
    this.shouldShow = shouldShow;
    setChanged();
    notifyObservers(shouldShow);
  }

  public boolean getShouldShow() {
    return this.shouldShow;
  }
}
