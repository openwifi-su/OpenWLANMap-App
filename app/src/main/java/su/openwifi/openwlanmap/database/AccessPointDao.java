package su.openwifi.openwlanmap.database;

import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_CAP;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_CHANNEL;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_FREQ;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_ID;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_LAT;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_LON;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_RSSID;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_SSID;
import static su.openwifi.openwlanmap.database.DatabaseConstant.COLUMN_UPDATE;
import static su.openwifi.openwlanmap.database.DatabaseConstant.TABLE_NAME;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import java.util.List;
import su.openwifi.openwlanmap.AccessPoint;

/**
 * This class contains methods to access the database.
 */

@Dao
public abstract class AccessPointDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  public abstract long insert(AccessPoint ap);

  @Query("UPDATE " + TABLE_NAME
      + " SET " + COLUMN_SSID + "=:ssid " + ","
      + COLUMN_RSSID + "=:rssid " + ","
      + COLUMN_FREQ + "=:frequency " + ","
      + COLUMN_CHANNEL + "=:channel " + ","
      + COLUMN_CAP + "=:capabilities " + ","
      + COLUMN_LAT + "=:lat " + ","
      + COLUMN_UPDATE + "=:toUpdate " + ","
      + COLUMN_LON + "=:lon "
      + " WHERE " + COLUMN_ID + "=:bssid " + " AND " + COLUMN_RSSID + "< :rssid"
  )
  public abstract void update(String bssid, String ssid, int rssid, double frequency, int channel,
                              String capabilities, double lat, double lon, boolean toUpdate);

  @Query("SELECT COUNT(*) FROM " + TABLE_NAME)
  public abstract long countEntries();

  @Query("DELETE FROM " + TABLE_NAME)
  public abstract void deleteAll();

  @Delete
  public abstract int delete(List<AccessPoint> entries);

  @Query("SELECT * FROM " + TABLE_NAME)
  public abstract List<AccessPoint> getAllDataEntries();

  @Query("SELECT * FROM " + TABLE_NAME + " LIMIT " + DatabaseConstant.UPLOAD_MAX)
  public abstract List<AccessPoint> getAllDataEntriesToUpload();

  @Query("SELECT * FROM " + TABLE_NAME + " WHERE " + DatabaseConstant.COLUMN_ID + "=:id")
  public abstract AccessPoint getDataEntry(String id);

  /**
   * This transaction update/insert a list of access point.
   * Update: if exist and the rssid is bigger.
   * Insert: if not exist in the database.
   * @param list : an collection of wifi access points
   */
  @Transaction
  public void insertOrUpdateIfExisted(List<AccessPoint> list) {
    for (AccessPoint en : list) {
      long insert = insert(en);
      if (insert == -1) {
        update(en.getBssid(), en.getSsid(), en.getRssid(), en.getFrequency(), en.getChannel(),
            en.getCapabilities(), en.getLat(), en.getLon(), en.isToUpdate());
      }
    }
  }

}
