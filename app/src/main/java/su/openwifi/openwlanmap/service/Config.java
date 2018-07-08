package su.openwifi.openwlanmap.service;

/**
 * This class contains all the config needed to control the
 * scanning service.
 */
public class Config {
  //TODO how to let storer thread update data/ or stop it and start new thread for it
  private static boolean isUploading = false;

  public static synchronized boolean getUploadStatus() {
    return isUploading;
  }

  public static synchronized void setUploadStatus(boolean uploadStatus) {
    isUploading = uploadStatus;
  }
}
