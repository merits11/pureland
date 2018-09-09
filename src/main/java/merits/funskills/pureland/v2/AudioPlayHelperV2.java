package merits.funskills.pureland.v2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.base.Preconditions;

import com.amazon.ask.model.interfaces.system.SystemState;

import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.model.PlayItem;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayListUtils;
import merits.funskills.pureland.model.PlayState;
import merits.funskills.pureland.model.Token;
import merits.funskills.pureland.model.UpdateLog;
import merits.funskills.pureland.model.UserSetting;
import merits.funskills.pureland.utils.AppConfig;
import merits.funskills.pureland.utils.PlayListManager;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;

@Log4j2
public class AudioPlayHelperV2 {

    private static AudioPlayHelperV2 _instance;

    private AmazonS3 s3Client;
    private DynamoDBMapper dynamoDBMapper;
    private PlayListManager playListManager;

    private AudioPlayHelperV2(final AmazonS3 amazonS3Client, final AmazonDynamoDB dynamoClient) {
        this.s3Client = amazonS3Client;
        this.dynamoDBMapper = new DynamoDBMapper(dynamoClient);
        this.playListManager = new PlayListManager(amazonS3Client);
    }

    void updateLibrary() {
        playListManager.updateLibrary();
    }

    public static AudioPlayHelperV2 getInstance() {
        if (_instance != null) {
            return _instance;
        }
        AmazonS3 s3client = AmazonS3ClientBuilder.standard()
            .withRegion("us-east-1")
            .build();
        AmazonDynamoDB dynamoClient = AmazonDynamoDBClientBuilder.standard()
            .withRegion("us-east-1")
            .build();
        _instance = new AudioPlayHelperV2(s3client, dynamoClient);
        return _instance;
    }

    private DynamoDBMapperConfig getConfig() {
        return DynamoDBMapperConfig.builder()
            .withTableNameOverride(new TableNameOverride(AppConfig.getPlayTable()))
            .build();
    }

    private DynamoDBMapperConfig getConfig(final DynamoDBMapperConfig.SaveBehavior saveBehavior) {
        return DynamoDBMapperConfig.builder()
            .withTableNameOverride(new TableNameOverride(AppConfig.getPlayTable()))
            .withSaveBehavior(saveBehavior)
            .build();
    }

    public PlayItem getPlayItem(final PlayList playList, final int seq) {
        List<PlayItem> playItems = playListManager.getListItems(playList);
        int listSize = playItems.size();
        Preconditions.checkState(listSize > 0, "%s has zero items!", playList);
        int index = seq;
        if (seq >= listSize) {
            index = seq % listSize;
        } else if (seq < 0) {
            index = (seq + listSize) % listSize;
        }
        return playItems.get(index);
    }

    public PlayItem getNextPlayItem(final PlayState playState) {
        PlayList playList = playState.currentPlayList();
        if (playState.isRepeat() || playState.isPaused()) {
            PlayItem playItem = getPlayItem(playList, playState.getCurrentSeq());
            log.info("Returning current song {} to play next", playItem);
            return playItem;
        }
        if (playList.isShuffle() && playState.isShuffle()) {
            int size = playListManager.getListItems(playList).size();
            int nextSeq = RandomUtils.nextInt(0, size);
            while (nextSeq == playState.getCurrentSeq()) {
                nextSeq = RandomUtils.nextInt(0, size);
            }
            return getPlayItem(playList, nextSeq);

        } else {
            return getPlayItem(playList, playState.getCurrentSeq() + 1);
        }
    }

    public String getUrl(final PlayItem playItem) {
        final Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        return s3Client.generatePresignedUrl(
            playItem.getBucket(), playItem.getObjectKey(), calendar.getTime()).toExternalForm();
    }

    public void savePlayState(final PlayState playState) {
        final Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        playState.setTokenExpireTime(calendar.getTimeInMillis());
        Date lastModified = playState.getLastModified();
        if (lastModified == null || lastModified.compareTo(new Date()) < 0) {
            playState.setLastModified(new Date());
        }
        this.dynamoDBMapper.save(playState, getConfig());
        log.debug("Play state saved into DB: {}", playState);
    }

    public void saveUserSettings(final UserSetting setting) {
        DynamoDBMapperConfig config = getConfig(DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES);
        this.dynamoDBMapper.save(setting, config);
        log.info("Settings saved into DB: {}", setting);
    }

    public void incrementHeardCount(final UserSetting setting, UpdateLog.Update update) {
        if (update.getVersion().equals(setting.getLastHeardVersion()) && setting.getHeardTimes() != null) {
            setting.setHeardTimes(setting.getHeardTimes() + 1);
        } else {
            setting.setHeardTimes(1);
        }
        setting.setLastHeardVersion(update.getVersion());
        saveUserSettings(setting);
    }

    public UserSetting getUserSettings(final String userId) {
        UserSetting settings =
            this.dynamoDBMapper.load(UserSetting.builder().userId(userId).build(), getConfig());
        return settings;
    }

    public PlayState getPlayStateBySystemState(final SystemState systemState) {
        return getPlayStateBySystemState(systemState, null);
    }

    public PlayState getPlayStateBySystemState(final SystemState systemState, final PlayList playList) {
        PlayState keyObj = PlayState.builder()
            .userid(systemState.getUser().getUserId())
            .build();
        final DynamoDBQueryExpression<PlayState> queryExpression = new DynamoDBQueryExpression<PlayState>()
            .withIndexName("UserIndex")
            .withHashKeyValues(keyObj)
            .withConsistentRead(false);
        final PaginatedQueryList<PlayState> results = this.dynamoDBMapper.query(
            PlayState.class, queryExpression, getConfig());

        if (results == null || results.isEmpty()) {
            return null;
        }

        Stream<PlayState> stream = results.stream();
        /*
        if (systemState.getDevice() != null && systemState.getDevice().getDeviceId() != null) {
            stream = stream.filter(v -> systemState.getDevice().getDeviceId().equals(v.getDeviceId()));
        }*/
        if (playList != null) {
            stream = stream.filter(v -> playList.toString().equals(v.getCurrentList()));
        }
        Optional<PlayState> playState = stream.sorted(
            comparing(PlayState::getLastModified, reverseOrder()))
            .filter(p -> p.currentPlayList() != null)
            .findFirst();
        if (!playState.isPresent()) {
            return null;
        }
        return dynamoDBMapper.load(PlayState.class, playState.get().getToken(), getConfig());
    }

    public List<PlayList> getRecentPlayed(final SystemState systemState, final int daysBack) {
        PlayState keyObj = PlayState.builder()
            .userid(systemState.getUser().getUserId())
            .build();
        final DynamoDBQueryExpression<PlayState> queryExpression = new DynamoDBQueryExpression<PlayState>()
            .withIndexName("UserIndex")
            .withHashKeyValues(keyObj)
            .withConsistentRead(false);

        final PaginatedQueryList<PlayState> results = this.dynamoDBMapper.query(
            PlayState.class, queryExpression, getConfig());

        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1 * daysBack);
        Date cutoffDate = calendar.getTime();
        List<PlayList> playLists = results.stream()
            .filter(v -> v.getLastModified().compareTo(cutoffDate) > 0)
            .sorted(comparing(PlayState::getLastModified, reverseOrder()))
            .map(v -> PlayListUtils.getPlaylist(v.getCurrentList()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return playLists.stream().distinct().collect(Collectors.toList());
    }

    public PlayState getPlayStateByStreamToken(final String streamToken) {
        Token tokenObj = Token.fromStreamToken(streamToken);
        PlayState playState = this.dynamoDBMapper.load(PlayState.class, tokenObj.getUuid(), getConfig());
        if (playState != null && playState.currentPlayList() == null) {
            return null;
        }
        return playState;
    }

    public void deletePlayState(final String streamToken) {
        PlayState playState = getPlayStateByStreamToken(streamToken);
        if (playState != null) {
            log.debug("Deleting play state:{}", playState);
            this.dynamoDBMapper.delete(playState, getConfig());
        }
    }

    public void deletePlayState(final Token token) {
        deletePlayState(token.getStreamToken());
    }

    public void putObjectToCodeBucket(String key, String content) {
        try {
            s3Client.putObject("purelandcodebucket", key, content);
        } catch (AmazonServiceException error) {
            log.error("Unable to put to code bucket", error);
        }
    }

}
