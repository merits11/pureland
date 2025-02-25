package merits.funskills.pureland.model;

import com.amazonaws.services.s3.model.S3ObjectSummary;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import merits.funskills.pureland.utils.PlayListManager;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString
public class PlayItem {
  @Getter private String bucket;
  @Getter private String objectKey;
  @Getter private PlayList listName;
  @Getter private int seqNo;
  @Getter private long size;
  @Getter private S3ObjectSummary s3Obj;

  private long duration;

  // 128kbps
  public long getApproximateDuration() {
    if (duration <= 0) {
      return (long) (1000.0 * size * 8 / 128000);
    }
    return duration;
  }

  public static PlayItem fromS3Object(
      final S3ObjectSummary s3ObjectSummary, final PlayList listName, final int seqNo) {
    PlayItem playItem = new PlayItem();
    playItem.bucket = s3ObjectSummary.getBucketName();
    playItem.objectKey = s3ObjectSummary.getKey();
    playItem.listName = listName;
    playItem.seqNo = seqNo;
    playItem.size = s3ObjectSummary.getSize();
    playItem.s3Obj = new S3ObjectSummary();
    playItem.s3Obj.setBucketName(s3ObjectSummary.getBucketName());
    playItem.s3Obj.setKey(s3ObjectSummary.getKey());
    playItem.s3Obj.setLastModified(s3ObjectSummary.getLastModified());
    playItem.s3Obj.setSize(s3ObjectSummary.getSize());
    playItem.s3Obj.setETag(s3ObjectSummary.getETag());
    playItem.duration = PlayListManager.getDuration(s3ObjectSummary);
    return playItem;
  }

  public int getDisplaySequence() {
    return seqNo + 1;
  }
}
