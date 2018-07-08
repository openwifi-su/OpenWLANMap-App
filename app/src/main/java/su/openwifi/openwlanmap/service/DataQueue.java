package su.openwifi.openwlanmap.service;

import android.util.Log;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import su.openwifi.openwlanmap.AccessPoint;

/**
 * Created by tali on 30.06.18.
 */

public class DataQueue {
  public BlockingDeque<List<AccessPoint>> buffer;

  public DataQueue() {
    this.buffer = new LinkedBlockingDeque<>();
  }

  /**
   * This method gives the next data in the blocked queue.
   * If there is no data, the current thread will be interrupted
   * @return a List of AccessPoint objects
   */
  public List<AccessPoint> getNextData() {
    try {
      return buffer.take();
    } catch (InterruptedException e) {
      Log.i("DATAQUEUE", "interrupt exception caught");
      e.printStackTrace();
    }
    return null;
  }


  /**
   * This method puts the list of access point as the next element in the blocked queue.
   * @param ap : list of access point
   */
  public synchronized void putNextData(List<AccessPoint> ap) {
    buffer.add(ap);
  }

  public boolean isEmpty() {
    return this.buffer.isEmpty();
  }
}
