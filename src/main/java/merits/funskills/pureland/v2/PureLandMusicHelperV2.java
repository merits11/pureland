package merits.funskills.pureland.v2;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.audioplayer.AudioPlayerState;
import com.amazon.ask.model.interfaces.audioplayer.PlayBehavior;
import com.amazon.ask.model.interfaces.audioplayer.Stream;
import com.amazon.ask.model.interfaces.system.SystemState;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import merits.funskills.pureland.model.PlayItem;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayState;
import merits.funskills.pureland.model.Tag;
import merits.funskills.pureland.model.Token;
import merits.funskills.pureland.model.UserSetting;

@RequiredArgsConstructor
public class PureLandMusicHelperV2 {

    private final AudioPlayHelperV2 playHelper;
    private final ResponseHelperV2 responseHelper;

    public UserSetting getUserSettings(final SystemState systemState) {
        String userId = systemState.getUser().getUserId();
        UserSetting userSettings = playHelper.getUserSettings(userId);
        return userSettings;
    }

    public void setLanguageTag(SystemState systemState, Tag langTag) {
        String userId = systemState.getUser().getUserId();
        String lang = langTag.equals(Tag.None) ? "" : langTag.toString();
        UserSetting language = UserSetting.builder()
            .userId(userId)
            .language(lang)
            .build();
        playHelper.saveUserSettings(language);
    }

    public Tag getLanguageTag(final SystemState systemState) {
        String userId = systemState.getUser().getUserId();
        UserSetting settings = playHelper.getUserSettings(userId);
        if (settings == null) {
            return null;
        }
        return settings.languageTag();
    }

    public PlayState newPlayState(PlayList playList, SystemState systemState) {
        PlayItem playItem = playHelper.getPlayItem(playList, 0);
        return PlayState.builder()
            .currentList(playList.toString())
            .currentSeq(playItem.getSeqNo())
            .deviceId(systemState.getDevice().getDeviceId())
            .userid(systemState.getUser().getUserId())
            .offsetInMs(0L)
            .build();
    }

    public PlayState saveAudioPlayState(final AudioPlayerState playerState) {
        if (playerState != null && StringUtils.isNotEmpty(playerState.getToken())) {
            final String streamToken = playerState.getToken();
            Token token = Token.fromStreamToken(streamToken);
            PlayState dbPlayState = playHelper.getPlayStateByStreamToken(streamToken);
            if (dbPlayState != null) {
                dbPlayState.setNextSeq(null);
                dbPlayState.setPaused(true);
                dbPlayState.setOffsetInMs(playerState.getOffsetInMilliseconds());
                dbPlayState.setCurrentSeq(token.getListSequence());
                playHelper.savePlayState(dbPlayState);
                return dbPlayState;
            }
        }
        return null;
    }

    private Date getMinutesLater(final int mins) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, mins);
        return calendar.getTime();
    }

    public Optional<Response> playLastSong(@NonNull final PlayState currentState,
        final String textResponse) {
        PlayState nextState = currentState.toBuilder().build();
        if (nextState.isRepeat()) {
            nextState.setRepeatSeq(null);
        }
        nextState.setPaused(false);
        PlayItem newPlayItem = playHelper.getPlayItem(currentState.currentPlayList(), nextState.getCurrentSeq());
        if (nextState.getOffsetInMs() >= newPlayItem.getApproximateDuration()) {
            nextState.setOffsetInMs(0L);
        }
        Token newToken = Token.newToken(newPlayItem);
        nextState.setCurrentSeq(newPlayItem.getSeqNo());
        nextState.setNextSeq(null);
        nextState.setToken(newToken.getUuid());
        //nextState.setLastModified(getMinutesLater(ADVANCE_MINS));
        playHelper.savePlayState(nextState);
        Stream stream = newStream(newPlayItem, null, nextState, false);
        if (currentState.getToken() != null) {
            playHelper.deletePlayState(Token.fromPlayState(currentState));
        }
        return responseHelper.newPlayResponse(textResponse, stream, PlayBehavior.REPLACE_ALL);
    }

    public Optional<Response> playNextSong(@NonNull final PlayState currentState, final String textResponse) {
        //Next will end repeat
        PlayState nextState = currentState.toBuilder().build();
        if (nextState.isRepeat()) {
            nextState.setRepeatSeq(null);
        }
        nextState.setPaused(false);
        PlayItem newPlayItem = playHelper.getNextPlayItem(nextState);
        Token newToken = Token.newToken(newPlayItem);
        nextState.setOffsetInMs(0L);
        nextState.setCurrentSeq(newPlayItem.getSeqNo());
        nextState.setNextSeq(null);
        nextState.setToken(newToken.getUuid());
        //nextState.setLastModified(getMinutesLater(ADVANCE_MINS));
        Stream stream = newStream(newPlayItem, null, nextState, false);
        playHelper.savePlayState(nextState);
        if (currentState.getToken() != null) {
            playHelper.deletePlayState(Token.fromPlayState(currentState));
        }
        return responseHelper.newPlayResponse(textResponse, stream, PlayBehavior.REPLACE_ALL);
    }

    public Optional<Response> playPreviousSong(@NonNull final PlayState currentState, final String textResponse) {
        //Next will end repeat
        PlayState nextState = currentState.toBuilder().build();
        if (nextState.isRepeat()) {
            nextState.setRepeatSeq(null);
        }
        nextState.setPaused(false);
        PlayItem newPlayItem = playHelper.getPlayItem(
            currentState.currentPlayList(),
            currentState.getCurrentSeq() - 1);
        Token newToken = Token.newToken(newPlayItem);
        nextState.setOffsetInMs(0L);
        nextState.setCurrentSeq(newPlayItem.getSeqNo());
        nextState.setNextSeq(null);
        nextState.setToken(newToken.getUuid());
        Stream stream = newStream(newPlayItem, null, nextState, false);
        playHelper.savePlayState(nextState);
        if (currentState.getToken() != null) {
            playHelper.deletePlayState(Token.fromPlayState(currentState));
        }
        return responseHelper.newPlayResponse(textResponse, stream, PlayBehavior.REPLACE_ALL);
    }

    private Stream newStream(PlayItem playItem, PlayState currentState, PlayState nextState, boolean isEnque) {
        Token newToken = Token.builder()
            .listName(nextState.getCurrentList().toString())
            .listSequence(isEnque ? nextState.getNextSeq() : nextState.getCurrentSeq())
            .uuid(nextState.getToken())
            .repeatSeq(nextState.isRepeat() ? nextState.getRepeatSeq() : null)
            .build();
        Stream.Builder builder = Stream.builder()
            .withOffsetInMilliseconds(isEnque ? 0 : nextState.getOffsetInMs())
            .withToken(newToken.getStreamToken())
            .withUrl(playHelper.getUrl(playItem));
        if (isEnque) {
            Token currentToken = Token.fromPlayState(currentState);
            builder.withExpectedPreviousToken(currentToken.getStreamToken());
        }
        return builder.build();
    }

    public Optional<Response> pauseAudio(final String speechText,
        final AudioPlayerState playerState) {
        saveAudioPlayState(playerState);
        return responseHelper.newStopResponse(speechText);
    }

    public Optional<Response> resumePlayList(final String speechText, final SystemState systemState,
        final PlayList myList) {
        PlayState playState = playHelper.getPlayStateBySystemState(systemState, myList);
        if (playState != null) {
            return playLastSong(playState, speechText);
        } else {
            return playLastSong(newPlayState(myList, systemState),
                speechText);
        }
    }

    public Optional<Response> replaceEnqued(@NonNull final PlayState currentState, final String textResponse) {
        PlayState nextState = currentState.toBuilder().build();
        nextState.setPaused(false);
        if (nextState.isRepeat()) {
            nextState.setRepeatSeq(currentState.getRepeatSeq() + 1);
        }
        nextState.setOffsetInMs(0);
        nextState.setPaused(false);
        PlayItem newPlayItem = playHelper.getNextPlayItem(currentState);
        Token newToken = Token.fromPlayState(nextState);
        nextState.setCurrentSeq(currentState.getCurrentSeq());
        nextState.setNextSeq(newPlayItem.getSeqNo());
        nextState.setToken(newToken.getUuid());
        Stream stream = newStream(newPlayItem, null, nextState, false);
        playHelper.savePlayState(nextState);
        return responseHelper.newPlayResponse(textResponse, stream, PlayBehavior.REPLACE_ENQUEUED);
    }

    public Optional<Response> enqueNextSong(@NonNull final PlayState currentState, final String textResponse) {
        PlayState nextState = currentState.toBuilder().build();
        nextState.setPaused(false);
        if (currentState.isRepeat()) {
            nextState.setRepeatSeq(currentState.getRepeatSeq() + 1);
        }
        PlayItem newPlayItem = playHelper.getNextPlayItem(currentState);
        nextState.setNextSeq(newPlayItem.getSeqNo());
        Stream stream = newStream(newPlayItem, currentState, nextState, true);
        //nextState.setLastModified(getMinutesLater(ADVANCE_MINS));
        playHelper.savePlayState(nextState);
        return responseHelper.newPlayResponse(textResponse, stream, PlayBehavior.ENQUEUE);
    }
}
