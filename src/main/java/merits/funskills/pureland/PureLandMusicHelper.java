package merits.funskills.pureland;

import java.util.Calendar;
import java.util.Date;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.speechlet.SpeechletRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioPlayerInterface;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioPlayerState;
import com.amazon.speech.speechlet.interfaces.audioplayer.PlayBehavior;
import com.amazon.speech.speechlet.interfaces.audioplayer.Stream;
import com.amazon.speech.speechlet.interfaces.system.SystemState;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import merits.funskills.pureland.model.PlayItem;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayState;
import merits.funskills.pureland.model.Tag;
import merits.funskills.pureland.model.Token;
import merits.funskills.pureland.model.UserSetting;

@RequiredArgsConstructor
public class PureLandMusicHelper {

    //private static final int ADVANCE_MINS = 1;

    private final AudioPlayHelper playHelper;
    private final ResponseHelper responseHelper;

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

    public <T extends SpeechletRequest> AudioPlayerState getAudioPlayerState(
        final SpeechletRequestEnvelope<T> requestEnvelope) {
        if (!requestEnvelope.getContext().hasState(AudioPlayerInterface.class)) {
            return null;
        }
        AudioPlayerState audioPlayerState = requestEnvelope.getContext().getState(
            AudioPlayerInterface.class, AudioPlayerState.class);
        if (audioPlayerState.getToken() != null) {
            return audioPlayerState;
        }
        return null;
    }

    public SpeechletResponse playNextSong(@NonNull final PlayState currentState, final String textResponse) {
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

    public SpeechletResponse playPreviousSong(@NonNull final PlayState currentState, final String textResponse) {
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
        //nextState.setLastModified(getMinutesLater(ADVANCE_MINS));
        Stream stream = newStream(newPlayItem, null, nextState, false);
        playHelper.savePlayState(nextState);
        if (currentState.getToken() != null) {
            playHelper.deletePlayState(Token.fromPlayState(currentState));
        }
        return responseHelper.newPlayResponse(textResponse, stream, PlayBehavior.REPLACE_ALL);
    }

    public SpeechletResponse playLastSong(@NonNull final PlayState currentState, final String textResponse) {
        //Next will end repeat
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

    public SpeechletResponse replaceEnqued(@NonNull final PlayState currentState, final String textResponse) {
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

    public SpeechletResponse enqueNextSong(@NonNull final PlayState currentState, final String textResponse) {
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

    private Stream newStream(PlayItem playItem, PlayState currentState, PlayState nextState, boolean isEnque) {
        Token newToken = Token.builder()
            .listName(nextState.getCurrentList().toString())
            .listSequence(isEnque ? nextState.getNextSeq() : nextState.getCurrentSeq())
            .uuid(nextState.getToken())
            .repeatSeq(nextState.isRepeat() ? nextState.getRepeatSeq() : null)
            .build();
        Stream stream = new Stream();
        stream.setOffsetInMilliseconds(isEnque ? 0 : nextState.getOffsetInMs());
        stream.setToken(newToken.getStreamToken());
        stream.setUrl(playHelper.getUrl(playItem));
        if (isEnque) {
            Token currentToken = Token.fromPlayState(currentState);
            stream.setExpectedPreviousToken(currentToken.getStreamToken());
        }
        return stream;
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

    public SpeechletResponse pauseAudio(final String speechText,
        final AudioPlayerState playerState) {
        saveAudioPlayState(playerState);
        return responseHelper.newStopResponse(speechText);
    }

    public PlayState saveAudioPlayState(final AudioPlayerState playerState) {
        if (playerState != null) {
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
}
