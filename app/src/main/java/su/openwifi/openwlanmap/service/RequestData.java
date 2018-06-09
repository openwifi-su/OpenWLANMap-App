package su.openwifi.openwlanmap.service;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by tali on 07.06.18.
 */

class RequestData {
  public Set<String> bssids;

  public RequestData() {
    bssids = new HashSet<>();
  }
}
