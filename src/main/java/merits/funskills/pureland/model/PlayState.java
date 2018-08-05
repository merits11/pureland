package merits.funskills.pureland.model;

import java.util.Date;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Builder(toBuilder = true)
@EqualsAndHashCode
@DynamoDBTable(tableName = "TO_BE_REPLACED")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
public class PlayState {

    private String userid;
    private String token;
    private String currentList;
    private Integer currentSeq;
    private Integer nextSeq;
    private Date lastModified;
    private boolean shuffle;
    private String deviceId;
    private long offsetInMs;
    private boolean paused;
    private long tokenExpireTime;
    private Integer repeatSeq;
    private Integer forwardMinutes;

    @DynamoDBHashKey
    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    @DynamoDBAttribute
    public long getOffsetInMs() {
        return offsetInMs;
    }

    public void setOffsetInMs(long offsetInMs) {
        this.offsetInMs = offsetInMs;
    }

    @DynamoDBAttribute
    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(final boolean shuffle) {
        this.shuffle = shuffle;
    }

    @DynamoDBAttribute
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(final String deviceId) {
        this.deviceId = deviceId;
    }

    @DynamoDBAttribute
    public boolean isRepeat() {
        return repeatSeq != null;
    }

    public void setRepeat(boolean repeat) {
        if (!repeat) {
            this.repeatSeq = null;
        } else {
            this.repeatSeq = this.repeatSeq != null ? this.repeatSeq : 0;
        }
    }

    @DynamoDBAttribute
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(final Date lastModified) {
        this.lastModified = lastModified;
    }

    @DynamoDBAttribute
    public String getCurrentList() {
        return currentList;
    }

    public void setCurrentList(final String currentList) {
        this.currentList = currentList;
    }

    @DynamoDBAttribute
    public Integer getCurrentSeq() {
        return currentSeq;
    }

    public void setCurrentSeq(final Integer currentSeq) {
        this.currentSeq = currentSeq;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "UserIndex")
    public String getUserid() {
        return userid;
    }

    public void setUserid(final String userid) {
        this.userid = userid;
    }

    @DynamoDBAttribute
    public Integer getNextSeq() {
        return nextSeq;
    }

    public void setNextSeq(final Integer nextSeq) {
        this.nextSeq = nextSeq;
    }

    @DynamoDBAttribute
    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @DynamoDBAttribute
    public long getTokenExpireTime() {
        return tokenExpireTime;
    }

    public void setTokenExpireTime(long tokenExpireTime) {
        this.tokenExpireTime = tokenExpireTime;
    }

    public PlayList currentPlayList() {
        return PlayListUtils.getPlaylist(this.currentList);
    }

    @DynamoDBAttribute
    public Integer getRepeatSeq() {
        return repeatSeq;
    }

    public void setRepeatSeq(Integer repeatSeq) {
        this.repeatSeq = repeatSeq;
    }

    @DynamoDBAttribute
    public Integer getForwardMinutes() {
        return forwardMinutes;
    }

    public void setForwardMinutes(Integer forwardMinutes) {
        this.forwardMinutes = forwardMinutes;
    }

    public static int getDisplaySequence(final PlayState playState) {
        return playState.currentSeq + 1;
    }

}
