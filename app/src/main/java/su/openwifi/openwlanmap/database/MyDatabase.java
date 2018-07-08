package su.openwifi.openwlanmap.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import su.openwifi.openwlanmap.AccessPoint;

/**
 * This class creates a singleton database object.
 */
@Database(entities = {AccessPoint.class}, version = 1)
public abstract class MyDatabase extends RoomDatabase {
  private static final String DB_NAME = "scan_result.db";
  private static volatile MyDatabase instance;

  /**
   * This method creates a singleton database object.
   * @param context : application context
   * @return MyDatabase object
   */
  public static synchronized MyDatabase getInstance(Context context) {
    if (instance == null) {
      instance = create(context);
    }
    return instance;
  }

  /**
   * This method creates a MyDatabase object.
   * @param context : application context
   * @return MyDatabase object
   */
  private static MyDatabase create(Context context) {
    return Room.databaseBuilder(
        context,
        MyDatabase.class,
        DB_NAME
    ).build();
  }

  //class have an abstract method with no parameters and
  // returns the class that is annotated with @Dao
  public abstract AccessPointDao getAccessPointDao();

}
