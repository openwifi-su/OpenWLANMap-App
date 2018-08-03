package su.openwifi.openwlanmap.utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class describes a ranking result from backend.
 */
public class RankingObject implements Parcelable {
  public static final Creator<RankingObject> CREATOR = new Creator<RankingObject>() {

    @Override
    public RankingObject createFromParcel(Parcel source) {
      return new RankingObject(source);
    }

    @Override
    public RankingObject[] newArray(int size) {
      return new RankingObject[0];
    }
  };
  public int remoteVersion;
  public int uploadedCount;
  public int uploadedRank;
  public int newAps;
  public int updAps;
  public int delAps;
  public int newPoints;

  public RankingObject() {
  }

  protected RankingObject(Parcel in) {
    uploadedRank = in.readInt();
    uploadedCount = in.readInt();
    newAps = in.readInt();
    updAps = in.readInt();
    delAps = in.readInt();
    newPoints = in.readInt();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(uploadedRank);
    dest.writeInt(uploadedCount);
    dest.writeInt(newAps);
    dest.writeInt(updAps);
    dest.writeInt(delAps);
    dest.writeInt(newPoints);
  }
}
