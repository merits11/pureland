package merits.funskills.pureland.v2.handlers;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Request;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.audioplayer.AudioPlayerState;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackFailedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackFinishedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackNearlyFinishedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackStartedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackStoppedRequest;
import com.amazon.ask.model.interfaces.playbackcontroller.NextCommandIssuedRequest;
import com.amazon.ask.model.interfaces.playbackcontroller.PauseCommandIssuedRequest;
import com.amazon.ask.model.interfaces.playbackcontroller.PlayCommandIssuedRequest;
import com.amazon.ask.model.interfaces.playbackcontroller.PreviousCommandIssuedRequest;
import com.amazon.ask.model.interfaces.system.SystemState;

import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.model.PlayItem;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayState;

@Log4j2
public class PlaybackRequestHandler extends BaseRequestHandler {

    private static final Set<Class<? extends Request>> REQUESTS = ImmutableSet.of(
            PlaybackStartedRequest.class,
            PlaybackFailedRequest.class,
            PlaybackNearlyFinishedRequest.class,
            PlaybackStoppedRequest.class,
            PlaybackFinishedRequest.class,
            PauseCommandIssuedRequest.class,
            NextCommandIssuedRequest.class,
            PreviousCommandIssuedRequest.class,
            PlayCommandIssuedRequest.class
    );

    @Override
    public boolean canHandle(HandlerInput input) {
        return requestTypeInSet(input, REQUESTS);
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        Request request = input.getRequestEnvelope().getRequest();
        PlayState playState = getPlayState(audioPlayer(input), systemState(input));
        if (request instanceof PauseCommandIssuedRequest) {
            return toolbox.pauseAudio("", audioPlayer(input));
        } else if (request instanceof NextCommandIssuedRequest) {
            return toolbox.playNextSong(playState, "");
        } else if (request instanceof PreviousCommandIssuedRequest) {
            return toolbox.playPreviousSong(playState, "");
        } else if (request instanceof PlayCommandIssuedRequest) {
            return toolbox.resumePlayList("", systemState(input), playState.currentPlayList());
        } else if (request instanceof PlaybackStartedRequest) {
            return playbackStarted((PlaybackStartedRequest) request, input);
        } else if (request instanceof PlaybackFailedRequest) {
            return playbackFailed((PlaybackFailedRequest) request, input);
        } else if (request instanceof PlaybackNearlyFinishedRequest) {
            return playbackNearlyFinished((PlaybackNearlyFinishedRequest) request, input);
        } else if (request instanceof PlaybackStoppedRequest) {
            return playbackStopped((PlaybackStoppedRequest) request, input);
        }
        return input.getResponseBuilder().build();
    }

    private Optional<Response> playbackStopped(PlaybackStoppedRequest request, HandlerInput input) {
        final String token = request.getToken();
        PlayState playState = playHelper.getPlayStateByStreamToken(token);
        if (playState != null) {
            playState.setOffsetInMs(request.getOffsetInMilliseconds());
            playHelper.savePlayState(playState);
        }
        return input.getResponseBuilder().build();
    }

    private Optional<Response> playbackStarted(PlaybackStartedRequest request, HandlerInput input) {
        final String token = request.getToken();
        PlayState playState = playHelper.getPlayStateByStreamToken(token);
        if (playState == null) {
            log.error("Cannot retrieve playstate by token {}", token);
        }
        if (playState != null && playState.getNextSeq() != null) {
            final String oldState = playState.toString();
            playState.setCurrentSeq(playState.getNextSeq());
            playState.setNextSeq(null);
            playState.setOffsetInMs(request.getOffsetInMilliseconds());
            playHelper.savePlayState(playState);
            final String newState = playState.toString();
            log.info("Changing play state from {} to {}", oldState, newState);
        }
        return input.getResponseBuilder().build();
    }

    private Optional<Response> playbackFailed(PlaybackFailedRequest request, HandlerInput input) {
        final String token = request.getToken();
        PlayState playState = getPlayState(audioPlayer(input), systemState(input));
        if (playState != null) {
            PlayList playList = playState.currentPlayList();
            PlayItem playItem = playHelper.getPlayItem(playList, playState.getCurrentSeq());
            playState.setOffsetInMs(request.getCurrentPlaybackState().getOffsetInMilliseconds());
            return toolbox.playNextSong(playState, "");
        }
        return input.getResponseBuilder().build();
    }

    private Optional<Response> playbackNearlyFinished(PlaybackNearlyFinishedRequest request, HandlerInput input) {
        final String token = request.getToken();
        PlayState playState = playHelper.getPlayStateByStreamToken(token);
        if (playState != null) {
            playState.setOffsetInMs(request.getOffsetInMilliseconds());
            return toolbox.enqueNextSong(playState, "");
        }
        return input.getResponseBuilder().build();
    }

    private PlayState getPlayState(AudioPlayerState audioPlayerState, SystemState systemState) {
        PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
        if (playState == null) {
            playState = playHelper.getPlayStateBySystemState(systemState);
        }
        if (audioPlayerState != null
                && audioPlayerState.getOffsetInMilliseconds() != null
                && audioPlayerState.getOffsetInMilliseconds() > playState.getOffsetInMs()) {
            playState.setOffsetInMs(audioPlayerState.getOffsetInMilliseconds());
        }
        return playState;
    }
}
