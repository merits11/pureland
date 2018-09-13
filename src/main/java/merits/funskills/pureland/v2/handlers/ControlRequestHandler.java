package merits.funskills.pureland.v2.handlers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;

import com.google.common.collect.ImmutableSet;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.audioplayer.AudioPlayerState;
import com.amazon.ask.model.interfaces.system.SystemState;

import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.model.PlayItem;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayListUtils;
import merits.funskills.pureland.model.PlayState;
import merits.funskills.pureland.model.Token;
import merits.funskills.pureland.v2.AudioPlayHelperV2;

import static java.util.concurrent.TimeUnit.MINUTES;
import static merits.funskills.pureland.model.Constants.SLOT_NAME_MINUTES;
import static merits.funskills.pureland.model.PlayState.getDisplaySequence;

@Log4j2
@NoArgsConstructor
public class ControlRequestHandler extends BaseRequestHandler {

    private static final ImmutableSet<String> INTENTS = ImmutableSet.of(
        "AMAZON.CancelIntent", "AMAZON.NextIntent", "AMAZON.PreviousIntent", "AMAZON.StopIntent",
        "AMAZON.ShuffleOnIntent", "AMAZON.ShuffleOffIntent", "AMAZON.PauseIntent", "AMAZON.ResumeIntent",
        "AMAZON.RepeatIntent", "AMAZON.LoopOnIntent", "AMAZON.LoopOffIntent", "AMAZON.StartOverIntent", "FastForward",
        "Rewind"
    );

    ControlRequestHandler(AudioPlayHelperV2 audioPlayHelper) {
        super(audioPlayHelper);
    }

    @Override
    public boolean canHandle(HandlerInput input) {
        return intentNameInSet(input, INTENTS);
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        Intent intent = getIntent(input);
        switch (intent.getName()) {
            case "AMAZON.CancelIntent":
            case "AMAZON.StopIntent":
                return stop(input, text("control.canceled"));
            case "AMAZON.PauseIntent":
                return stop(input, text("control.paused"));
            case "AMAZON.NextIntent":
                return next(input, true);
            case "AMAZON.PreviousIntent":
                return next(input, false);
            case "AMAZON.ShuffleOnIntent":
                return shuffleOn(input);
            case "AMAZON.ResumeIntent":
                return resume(input);
            case "AMAZON.RepeatIntent":
                return repeat(input);
            case "AMAZON.LoopOnIntent":
                return loop(input, true);
            case "AMAZON.LoopOffIntent":
                return loop(input, false);
            case "AMAZON.StartOverIntent":
                return startOver(input);
            case "AMAZON.ShuffleOffIntent":
                return shuffleOff(input);
            case "FastForward":
            case "Rewind":
                return fastForward(input);
        }
        return input.getResponseBuilder().build();
    }

    private Optional<Response> next(HandlerInput input, boolean forward) {
        AudioPlayerState audioPlayerState = audioPlayer(input);
        if (audioPlayerState == null || !Token.isValidToken(audioPlayerState.getToken())) {
            return input.getResponseBuilder().withSpeech(text("error.noAudioState")).build();
        }
        PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayer(input).getToken());
        if (playState != null) {
            log.debug("Retrieved play state for current system: {} ", playState);
            if (forward) {
                int nextDisplaySeq = playHelper
                    .getPlayItem(playState.currentPlayList(), playState.getCurrentSeq() + 1)
                    .getDisplaySequence();
                return toolbox.playNextSong(playState, text("control.next", nextDisplaySeq));
            } else {
                if (playState.isShuffle()) {
                    return input.getResponseBuilder().withSpeech(text("control.noNextUnderShuffle")).build();
                }
                int nextDisplaySeq = playHelper.getPlayItem(playState.currentPlayList(), playState.getCurrentSeq() - 1)
                    .getSeqNo() + 1;
                return toolbox.playPreviousSong(playState, text("control.previous", nextDisplaySeq));
            }
        } else {
            return input.getResponseBuilder().withSpeech(text("error.noPlayState")).build();
        }
    }

    private Optional<Response> stop(HandlerInput input, String speechText) {
        toolbox.saveAudioPlayState(audioPlayer(input));
        return responseHelper.newStopResponse(speechText);
    }

    private Optional<Response> shuffleOn(HandlerInput input) {
        AudioPlayerState audioPlayerState = audioPlayer(input);
        if (audioPlayerState != null) {
            PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
            if (playState != null) {
                toolbox.pauseAudio("", audioPlayerState);
                PlayList playList = PlayListUtils.getPlaylist(playState.getCurrentList());
                return handleTagIntent(input, playList.getTags().stream()
                    .filter(t -> t.isAccess() || t.isContent())
                    .collect(Collectors.toList()), text("control.shuffling"));
            }
        }
        return input.getResponseBuilder().withSpeech(text("error.cannotShuffle")).build();
    }

    private Optional<Response> shuffleOff(HandlerInput input) {
        AudioPlayerState audioPlayerState = audioPlayer(input);
        if (audioPlayerState != null) {
            PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
            if (playState != null) {
                toolbox.pauseAudio("", audioPlayerState);
                List<PlayList> recentPlayed = playHelper.getRecentPlayed(systemState(input), 30);
                Optional<PlayList> previousPlayed = recentPlayed.stream()
                    .filter(pl -> pl != playState.currentPlayList())
                    .findAny();
                if (!previousPlayed.isPresent()) {
                    input.getResponseBuilder().withSpeech(text("control.shuffleOff")).build();
                }
                return toolbox.resumePlayList(text("control.backToPrevious"),
                    systemState(input), previousPlayed.get());
            }
        }
        return input.getResponseBuilder().withSpeech(text("error.cannotShuffle")).build();
    }

    private Optional<Response> loop(HandlerInput input, boolean isLoppOn) {
        AudioPlayerState audioPlayerState = audioPlayer(input);
        if (audioPlayerState != null) {
            PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
            if (playState != null) {
                if (isLoppOn) {
                    playState.setRepeatSeq(0);
                    return toolbox.replaceEnqued(playState, text("control.loopOn", getDisplaySequence(playState)));
                } else {
                    playState.setRepeat(false);
                    return toolbox.replaceEnqued(playState, text("control.loopOff"));
                }
            }
        }
        return input.getResponseBuilder().withSpeech(text("error.cannotLoop")).build();
    }

    private Optional<Response> fastForward(HandlerInput input) {
        AudioPlayerState audioPlayerState = audioPlayer(input);
        if (audioPlayerState == null || !Token.isValidToken(audioPlayerState.getToken())) {
            return input.getResponseBuilder().withSpeech(text("error.noAudioState")).build();
        }
        PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
        if (playState == null) {
            return input.getResponseBuilder().withSpeech(text("error.noPlayState")).build();
        }
        boolean forward = true;
        int minutes = -1;
        Intent intent = getIntent(input);
        if (intent.getName().equals("Rewind")) {
            forward = false;
        }
        if (intent.getSlots() != null && intent.getSlots().containsKey(SLOT_NAME_MINUTES)) {
            minutes = Integer.parseInt(intent.getSlots().get(SLOT_NAME_MINUTES).getValue());
        }

        PlayItem playItem = progress(forward, minutes, playState, audioPlayerState);
        long percent = Math.round(playState.getOffsetInMs() * 100.0 / playItem.getApproximateDuration());
        return toolbox.playLastSong(playState,
            text("control.forward", percent, playItem.getDisplaySequence()));
    }

    private PlayItem progress(boolean goForward, int minutes, PlayState playState, AudioPlayerState audioPlayerState) {
        PlayItem playItem = playHelper.getPlayItem(playState.currentPlayList(), playState.getCurrentSeq());
        long totalMs = playItem.getApproximateDuration();
        long elapsedMs = audioPlayerState.getOffsetInMilliseconds();
        long remainingMs = totalMs - elapsedMs;
        long moveMs;
        if (minutes > 0) {
            moveMs = MINUTES.toMillis(minutes);
        } else {
            moveMs = Math.max(MINUTES.toMillis(RandomUtils.nextInt(5, 11)), remainingMs / 5);
        }
        long newOffset = (goForward ? moveMs : -moveMs) + elapsedMs;
        if (newOffset < 0) {
            playItem = playHelper.getPlayItem(playState.currentPlayList(), playState.getCurrentSeq() - 1);
            newOffset = (newOffset + playItem.getApproximateDuration()) % playItem.getApproximateDuration();
        } else if (newOffset >= totalMs) {
            playItem = playHelper.getPlayItem(playState.currentPlayList(), playState.getCurrentSeq() + 1);
            newOffset = newOffset % playItem.getApproximateDuration();
        }
        playState.setOffsetInMs(newOffset);
        playState.setCurrentSeq(playItem.getSeqNo());
        return playItem;
    }

    private Optional<Response> resume(HandlerInput input) {
        AudioPlayerState audioPlayerState = audioPlayer(input);
        SystemState systemState = systemState(input);
        PlayState playState;
        if (audioPlayerState != null) {
            playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());

        } else {
            playState = playHelper.getPlayStateBySystemState(systemState);
        }
        if (playState != null) {
            //changing to a new token
            return toolbox.playLastSong(playState,
                text("control.resume", playState.currentPlayList(), getDisplaySequence(playState)));
        }
        return input.getResponseBuilder().withSpeech(text("error.cannotResume")).build();
    }

    private Optional<Response> repeat(HandlerInput input) {
        AudioPlayerState audioPlayerState = audioPlayer(input);
        PlayState playState = null;
        if (audioPlayerState != null) {
            playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
        }
        if (playState != null) {
            playState.setOffsetInMs(0L);
            return toolbox.playLastSong(playState,
                text("control.repeat", getDisplaySequence(playState), playState.getCurrentList()));
        }
        return input.getResponseBuilder().withSpeech(text("error.cannotRepeat")).build();
    }

    private Optional<Response> startOver(HandlerInput input) {
        AudioPlayerState audioPlayerState = audioPlayer(input);
        SystemState systemState = systemState(input);
        if (audioPlayerState != null) {
            PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
            PlayState newPlayState = toolbox.newPlayState(playState.currentPlayList(), systemState);
            return toolbox.playLastSong(newPlayState, text("control.startOver", playState.currentPlayList()));
        }
        return input.getResponseBuilder().withSpeech(text("error.cannotStartOver")).build();
    }
}
