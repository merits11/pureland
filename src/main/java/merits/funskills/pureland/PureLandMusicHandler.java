package merits.funskills.pureland;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.speechlet.dialog.directives.DialogIntent;
import com.amazon.speech.speechlet.dialog.directives.DialogSlot;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioPlayer;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioPlayerInterface;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioPlayerState;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackFailedRequest;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackFinishedRequest;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackNearlyFinishedRequest;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackStartedRequest;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackStoppedRequest;
import com.amazon.speech.speechlet.interfaces.playbackcontroller.PlaybackController;
import com.amazon.speech.speechlet.interfaces.playbackcontroller.request.NextCommandIssuedRequest;
import com.amazon.speech.speechlet.interfaces.playbackcontroller.request.PauseCommandIssuedRequest;
import com.amazon.speech.speechlet.interfaces.playbackcontroller.request.PlayCommandIssuedRequest;
import com.amazon.speech.speechlet.interfaces.playbackcontroller.request.PreviousCommandIssuedRequest;
import com.amazon.speech.speechlet.interfaces.system.SystemInterface;
import com.amazon.speech.speechlet.interfaces.system.SystemState;

import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.model.NameMapping;
import merits.funskills.pureland.model.PlayItem;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayListUtils;
import merits.funskills.pureland.model.PlayState;
import merits.funskills.pureland.model.Tag;
import merits.funskills.pureland.model.UserSetting;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static merits.funskills.pureland.model.PlayState.getDisplaySequence;
import static merits.funskills.pureland.model.UpdateLog.getLatestUpdate;
import static merits.funskills.pureland.model.UpdateLog.shouldPlayLatest;

@Log4j2
public class PureLandMusicHandler implements SpeechletV2, AudioPlayer, PlaybackController {

    private static final String SKILL_NAME = "pure land";
    private static final String SLOT_LIST_NAME = "LIST_NUMBER";
    private static final String SLOT_LANG = "Lang";
    private static final String SUPPORTED_PLAY_TEXT =
        " play Buddhist music, chanting, dharma talk, sutra," +
            " random, a list name, or list number. ";
    private static final int NUM_RECENT_DAYS = 14;

    private AudioPlayHelper playHelper;
    private PureLandMusicHelper toolbox;
    private ResponseHelper responseHelper = new ResponseHelper();

    public PureLandMusicHandler(final AudioPlayHelper helper) {
        this.playHelper = helper;
        this.toolbox = new PureLandMusicHelper(this.playHelper, this.responseHelper);
    }

    @Override
    public void onSessionStarted(final SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {

    }

    @Override
    public SpeechletResponse onLaunch(final SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        SystemState systemState = requestEnvelope.getContext().getState(SystemInterface.class, SystemState.class);
        List<PlayList> recentPlayed = this.playHelper.getRecentPlayed(systemState, 180);
        StringBuffer sb = new StringBuffer();
        sb.append("Welcome to " + SKILL_NAME + "! ");
        UserSetting settings = toolbox.getUserSettings(systemState);
        if (settings == null || StringUtils.isEmpty(settings.getLanguage())) {
            sb.append(
                "I need to know your language preference. Which do you prefer: English, Chinese, all languages?");
            return responseHelper.newAskResponse(sb.toString(), "English, Chinese, all languages?");
        } else if (shouldPlayLatest(settings)) {
            sb.append(getLatestUpdate().getUpdate());
            settings.setLastHeardVersion(getLatestUpdate().getVersion());
            playHelper.saveUserSettings(settings);
        } else if (recentPlayed == null || recentPlayed.size() < 3) {
            sb.append("I can " + SUPPORTED_PLAY_TEXT);
        }
        sb.append("What to play? ");
        return responseHelper.newAskResponse(sb.toString(),
            "What to play? Music, chanting, talk, sutra, a list name, a number or say: help.");
    }

    @Override
    public SpeechletResponse onIntent(final SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        IntentRequest request = requestEnvelope.getRequest();
        Session session = requestEnvelope.getSession();
        Intent intent = request.getIntent();
        String intentName = intent.getName();
        SystemState systemState = requestEnvelope.getContext().getState(SystemInterface.class, SystemState.class);
        AudioPlayerState audioPlayerState = toolbox.getAudioPlayerState(requestEnvelope);
        log.info("Intent={} UserId={} DeviceId={} requestId={}, sessionId={} ",
            intentName,
            systemState.getUser().getUserId(),
            systemState.getDevice().getDeviceId(),
            request.getRequestId(),
            session.getSessionId());

        if ("MorningService".equals(intentName)) {
            return resumePlayList("Playing morning service.", systemState, PlayList.MorningService);
        } else if ("EveningService".equals(intentName)) {
            return resumePlayList("Playing evening service.", systemState, PlayList.EveningService);
        } else if ("PlayList".equals(intentName)) {
            return handlePlayListRequest(intent, systemState);
        } else if ("RandomList".equals(intentName)) {
            return handleTagIntent(systemState, newArrayList(), "");
        } else if ("DharmaTalk".equals(intentName)) {
            return handleTagIntent(systemState, newArrayList(Tag.DharmaTalk), "");
        } else if ("SutraIntent".equals(intentName)) {
            return handleTagIntent(systemState, newArrayList(Tag.Sutra), "");
        } else if ("MusicIntent".equals(intentName)) {
            return handleTagIntent(systemState, newArrayList(Tag.Music), "");
        } else if ("ChantIntent".equals(intentName)) {
            return handleTagIntent(systemState, newArrayList(Tag.Chanting), "");
        } else if ("LangIntent".equals(intentName)) {
            return handleLangIntent(request, systemState);
        } else if ("AMAZON.NextIntent".equals(intentName)) {
            return handleNextIntent(audioPlayerState, true);
        } else if ("AMAZON.PreviousIntent".equals(intentName)) {
            return handleNextIntent(audioPlayerState, false);
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            return handleHelpRequest(systemState);
        } else if ("AMAZON.StopIntent".equals(intentName)) {
            return toolbox.pauseAudio("Paused.", audioPlayerState);
        } else if ("AMAZON.CancelIntent".equals(intentName)) {
            return toolbox.pauseAudio("Canceled.", audioPlayerState);
        } else if ("AMAZON.ShuffleOnIntent".equals(intentName)) {
            return handleShuffleOnRequest(audioPlayerState, systemState);
        } else if ("AMAZON.ShuffleOffIntent".equals(intentName)) {
            return handleShuffleOffRequest(audioPlayerState, systemState);
        } else if ("AMAZON.PauseIntent".equals(intentName)) {
            return toolbox.pauseAudio("Paused.", audioPlayerState);
        } else if ("AMAZON.ResumeIntent".equals(intentName)) {
            return handleResumeRequest(audioPlayerState, systemState);
        } else if ("AMAZON.RepeatIntent".equals(intentName)) {
            return handleRepeatRequest(audioPlayerState, systemState);
        } else if ("AMAZON.LoopOnIntent".equals(intentName)) {
            return handleLoopOnRequest(audioPlayerState, systemState);
        } else if ("AMAZON.LoopOffIntent".equals(intentName)) {
            return handleLoopOffRequest(audioPlayerState, systemState);
        } else if ("AMAZON.StartOverIntent".equals(intentName)) {
            return handleStartOverRequest(systemState, audioPlayerState);
        } else if ("CustomNameIntent".equals(intentName)) {
            return handleCustomNameIntent(intent, audioPlayerState, systemState);
        } else {
            return responseHelper.newTellResponse("Sorry, I do not understand this one.");
        }
    }

    SpeechletResponse handleTagIntent(SystemState systemState, List<Tag> tags, String preSpeech) {
        Tag langTag = toolbox.getLanguageTag(systemState);
        if (langTag != null) {
            tags.add(langTag);
        }
        List<PlayList> recentPlayLists = playHelper.getRecentPlayed(systemState, NUM_RECENT_DAYS);
        PlayList candidateList = PlayListUtils.getListByTags(tags, recentPlayLists);
        return resumePlayList(preSpeech + " Here is a list you might like: " + candidateList.getListNumber()
            + ", " + candidateList.getText(), systemState, candidateList);
    }

    SpeechletResponse playOneOfTheseLists(SystemState systemState, Set<PlayList> playLists) {
        Tag langTag = toolbox.getLanguageTag(systemState);
        List<PlayList> fullList = Lists.newArrayList(playLists);
        List<PlayList> candidateLists = fullList;
        if (langTag != null) {
            candidateLists = playLists.stream().filter(p -> p.isTagged(langTag)).collect(Collectors.toList());
            if (candidateLists.isEmpty()) {
                candidateLists = fullList;
            }
        }
        if (candidateLists.size() > 1) {
            List<PlayList> recentPlayLists = playHelper.getRecentPlayed(systemState, NUM_RECENT_DAYS);
            PlayList mostRecentlyPlayed = recentPlayLists.isEmpty() ? null : recentPlayLists.get(0);
            candidateLists = candidateLists.stream().filter(p -> !Objects.equals(mostRecentlyPlayed, p))
                .collect(Collectors.toList());
        }
        int selected = RandomUtils.nextInt(0, candidateLists.size());
        PlayList playList = candidateLists.get(selected);
        return resumePlayList("Playing " + playList.getListNumber() + ", " + playList.getText() + ".",
            systemState, playList);
    }

    private SpeechletResponse handleLangIntent(IntentRequest intentRequest, SystemState systemState) {
        Intent intent = intentRequest.getIntent();
        if (intent.getSlots().containsKey(SLOT_LANG) && StringUtils.isNotBlank(intent.getSlot(SLOT_LANG).getValue())) {
            Slot slot = intent.getSlot(SLOT_LANG);
            String lang = slot.getValue().toLowerCase();
            Tag langTag = Tag.AllLanguages;
            switch (lang) {
                case "english":
                    langTag = Tag.English;
                    break;
                case "chinese":
                    langTag = Tag.Chinese;
                    break;
                default:
                    break;
            }
            toolbox.setLanguageTag(systemState, langTag);
            return responseHelper.newAskResponse("Language has been set to " + slot.getValue() + "." +
                    " You can always reset by saying 'change language'. Now what do you like to play? I can "
                    + SUPPORTED_PLAY_TEXT,
                "What to play? Say help for instructions.");
        } else {
            DialogIntent dialogIntent = new DialogIntent(
                "LangIntent",
                ImmutableMap.of("Lang", new DialogSlot("Lang", null)));
            return responseHelper.newDialogDelegateResponse(dialogIntent);
        }
    }

    SpeechletResponse handleCustomNameIntent(
        final Intent intent,
        final AudioPlayerState audioPlayerState,
        final SystemState systemState) {
        Collection<Slot> slots = intent.getSlots().values();
        Optional<Slot> firstSlot = slots.stream().filter(entry -> StringUtils.isNotBlank(entry.getValue())).findFirst();
        if (firstSlot.isPresent()) {
            return playOneOfTheseLists(systemState, NameMapping.getPlayLists(firstSlot.get().getName()));
        }
        return responseHelper.newAskResponse("What to play?",
            "Music, chanting, talk, sutra, a number or say: help.");
    }

    SpeechletResponse handleResumeRequest(
        final AudioPlayerState audioPlayerState,
        final SystemState systemState) {
        PlayState playState;
        if (audioPlayerState != null) {
            playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());

        } else {
            playState = playHelper.getPlayStateBySystemState(systemState);
        }
        if (playState != null) {
            //changing to a new token
            return toolbox.playLastSong(playState, "Resume playing " + playState.getCurrentList() + ", sequence " +
                getDisplaySequence(playState) + ".");
        }
        return responseHelper.newTellResponse("I don't know what to resume.");
    }

    SpeechletResponse handleStartOverRequest(
        final SystemState systemState,
        final AudioPlayerState audioPlayerState) {
        if (audioPlayerState != null) {
            PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
            PlayState newPlayState = toolbox.newPlayState(playState.currentPlayList(), systemState);
            return toolbox.playLastSong(newPlayState, "Start list " + playState.getCurrentList() + " from sequence 1.");
        }
        return responseHelper.newTellResponse("I don't know which play list to start over.");
    }

    SpeechletResponse handleLoopOnRequest(final AudioPlayerState audioPlayerState, final SystemState systemState) {
        if (audioPlayerState != null) {
            PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
            if (playState != null) {
                playState.setRepeatSeq(0);
                return toolbox.replaceEnqued(playState,
                    "Will loop sequence " + getDisplaySequence(playState) + " until you say next.");
            }
        }
        return responseHelper.newTellResponse("I don't know what to loop.");
    }

    SpeechletResponse handleLoopOffRequest(final AudioPlayerState audioPlayerState, final SystemState systemState) {
        if (audioPlayerState != null) {
            PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
            if (playState != null) {
                playState.setRepeat(false);
                return toolbox.replaceEnqued(playState, "Will end loop.");
            }
        }
        return responseHelper.newTellResponse("I don't know what to loop.");
    }

    SpeechletResponse handleRepeatRequest(final AudioPlayerState audioPlayerState, final SystemState systemState) {
        PlayState playState = null;
        if (audioPlayerState != null) {
            playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
        }
        if (playState != null) {
            playState.setOffsetInMs(0L);
            return toolbox.playLastSong(playState,
                "Repeat sequence " + getDisplaySequence(playState) + " of  " + playState.getCurrentList() + ".");
        }
        return responseHelper.newTellResponse("I don't know what to repeat.");
    }

    SpeechletResponse handleShuffleOffRequest(final AudioPlayerState audioPlayerState, final SystemState systemState) {
        return handleShuffleOffAsFastForwardIntent(audioPlayerState);
    }

    SpeechletResponse handleShuffleOnRequest(final AudioPlayerState audioPlayerState, final SystemState systemState) {
        if (audioPlayerState != null) {
            PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
            if (playState != null) {
                toolbox.pauseAudio("", audioPlayerState);
                PlayList playList = PlayListUtils.getPlaylist(playState.getCurrentList());
                return handleTagIntent(systemState,
                    playList.getTags().stream()
                        .filter(t -> t.isAccess() || t.isContent())
                        .collect(Collectors.toList()),
                    "Shuffling now. ");
            }
        }
        return responseHelper.newTellResponse("Sorry, I can't shuffle this.");
    }

    SpeechletResponse handleNextIntent(final AudioPlayerState audioPlayerState, boolean forward) {
        PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
        if (playState != null) {
            log.info("Retrieved play state for current system: {} ", playState);
            if (forward) {
                int nextDisplaySeq = playHelper
                    .getPlayItem(playState.currentPlayList(), playState.getCurrentSeq() + 1)
                    .getDisplaySequence();
                return toolbox.playNextSong(playState, format("Playing next, sequence %d.", nextDisplaySeq));

            } else {
                if (playState.isShuffle()) {
                    return responseHelper.newTellResponse("Hmm, I cannot go back under shuffle mode.");
                }
                int nextDisplaySeq = playHelper.getPlayItem(playState.currentPlayList(), playState.getCurrentSeq() - 1)
                    .getSeqNo() + 1;
                return toolbox.playPreviousSong(playState, format("Playing previous, sequence %d.", nextDisplaySeq));
            }
        } else {
            return responseHelper.newTellResponse("Hmm, I cannot retrieve play state.");
        }
    }

    SpeechletResponse handleShuffleOffAsFastForwardIntent(final AudioPlayerState audioPlayerState) {
        PlayState playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());
        if (playState == null) {
            return responseHelper.newTellResponse("Hmm, I cannot retrieve play state.");
        }
        int currentDisplaySequence = getDisplaySequence(playState);
        PlayItem playItem = playHelper.getPlayItem(playState.currentPlayList(), playState.getCurrentSeq());
        long totalMs = playItem.getApproximateDuration();
        long elapsedMs = audioPlayerState.getOffsetInMilliseconds();
        long remainingMs = totalMs - elapsedMs;
        int randomMins = RandomUtils.nextInt(5, 11);
        long fastforwardMs = Math.max(MINUTES.toMillis(randomMins), remainingMs / 5);
        log.info("Retrieved play state for current system: {} ", playState);
        long newOffset = (fastforwardMs + elapsedMs) % totalMs;
        playState.setOffsetInMs(newOffset);
        long percent = Math.round(playState.getOffsetInMs() * 100.0 / totalMs);
        return toolbox.playLastSong(playState,
            format("Forward %d minutes to %d%% of sequence %d.", MILLISECONDS.toMinutes(fastforwardMs),
                percent, currentDisplaySequence));

    }

    SpeechletResponse handlePlayListRequest(final Intent intent, final SystemState systemState) {
        Slot slot = intent.getSlot(SLOT_LIST_NAME);
        if (slot == null || slot.getValue() == null) {
            return responseHelper.newAskResponse(
                "I could not understand your command. What to play?",
                "say that again?");
        }
        String listUtterance = slot.getValue();
        PlayList myList = PlayListUtils.getPlaylist(listUtterance);

        if (myList != null) {
            final String listDescription = myList.getListNumber() > 0 ?
                (myList.getListNumber() + ": " + myList.getText()) : myList.getText();
            return resumePlayList("Playing list " + listDescription + ".", systemState, myList);

        } else {
            return handleTagIntent(systemState, Lists.newArrayList(), "List " + listUtterance + " not found. ");
        }

    }

    private SpeechletResponse resumePlayList(final String speechText, final SystemState systemState,
        final PlayList myList) {
        PlayState playState = playHelper.getPlayStateBySystemState(systemState, myList);
        if (playState != null) {
            return toolbox.playLastSong(playState, speechText);
        } else {
            return toolbox.playLastSong(toolbox.newPlayState(myList, systemState),
                speechText);
        }
    }

    @Override
    public void onSessionEnded(final SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {

    }

    SpeechletResponse handleHelpRequest(final SystemState systemState) {
        return responseHelper.newAskResponse(
            "I've sent instructions to your Alexa app. You can ask me to " + SUPPORTED_PLAY_TEXT +
                " Say: set language to change your preferred language. What would you like to play?",
            "What to play?",
            "Amitabha 阿彌陀佛",
            PlayListUtils.getCardText()
        );

    }

    @Override
    public SpeechletResponse onPlaybackFailed(final SpeechletRequestEnvelope<PlaybackFailedRequest> requestEnvelope) {
        final String token = requestEnvelope.getRequest().getToken();
        log.info("PlaybackFailedRequest received with token:" + token);
        PlayState playState = playHelper.getPlayStateByStreamToken(token);
        AudioPlayerState audioPlayerState = toolbox.getAudioPlayerState(requestEnvelope);
        playState = toolbox.saveAudioPlayState(audioPlayerState);
        PlayList playList = playState.currentPlayList();
        PlayItem playItem = playHelper.getPlayItem(playList, playState.getCurrentSeq());
        log.error("[DEBUGTAG] Playback failed for play item: {}", playItem);
        return toolbox.playLastSong(playState, "");

    }

    @Override
    public SpeechletResponse onPlaybackFinished(
        final SpeechletRequestEnvelope<PlaybackFinishedRequest> requestEnvelope) {
        log.info("PlaybackFinishedRequest received with token:" + requestEnvelope.getRequest().getToken());
        return new SpeechletResponse();
    }

    @Override
    public SpeechletResponse onPlaybackNearlyFinished(
        final SpeechletRequestEnvelope<PlaybackNearlyFinishedRequest> requestEnvelope) {
        final String token = requestEnvelope.getRequest().getToken();
        log.info("PlaybackNearlyFinishedRequest received with token:" + token);
        PlayState playState = playHelper.getPlayStateByStreamToken(token);
        if (playState != null) {
            return toolbox.enqueNextSong(playState, null);
        }
        return new SpeechletResponse();
    }

    @Override
    public SpeechletResponse onPlaybackStarted(
        final SpeechletRequestEnvelope<PlaybackStartedRequest> requestEnvelope) {
        log.info("PlaybackStartedRequest received with token:" + requestEnvelope.getRequest().getToken());
        final String token = requestEnvelope.getRequest().getToken();
        PlayState playState = playHelper.getPlayStateByStreamToken(token);
        if (playState == null) {
            log.error("Cannot retrieve playstate by token {}", token);
        }
        if (playState != null && playState.getNextSeq() != null) {
            final String oldState = playState.toString();
            playState.setCurrentSeq(playState.getNextSeq());
            playState.setNextSeq(null);
            playHelper.savePlayState(playState);
            final String newState = playState.toString();
            log.info("Changing play state from {} to {}", oldState, newState);
        }
        return new SpeechletResponse();
    }

    /**
     * Sent when Alexa stops playing an audio stream in response to one of the following AudioPlayer directives:
     *
     * Stop
     * Play with a playBehavior of REPLACE_ALL.
     * ClearQueue with a clearBehavior of CLEAR_ALL.
     * This request is also sent if the user makes a voice request to Alexa, since this temporarily pauses the
     * playback. In this case, the playback begins automatically once the voice interaction is complete.
     *
     * Note: If playback stops because the audio stream comes to an end on its own, Alexa sends PlaybackFinished
     * instead of PlaybackStopped.
     */

    @Override
    public SpeechletResponse onPlaybackStopped(
        final SpeechletRequestEnvelope<PlaybackStoppedRequest> requestEnvelope) {
        log.info("PlaybackStoppedRequest received with token:" + requestEnvelope.getRequest().getToken());
        toolbox.saveAudioPlayState(toolbox.getAudioPlayerState(requestEnvelope));
        return new SpeechletResponse();
    }

    private <T extends SpeechletRequest> PlayState getPlayStateFromPlaybackControllerRequest(
        SpeechletRequestEnvelope<T> requestEnvelope) {
        AudioPlayerState state = toolbox.getAudioPlayerState(requestEnvelope);
        if (state == null) {
            return null;
        }
        return playHelper.getPlayStateByStreamToken(state.getToken());

    }

    @Override
    public SpeechletResponse onNextCommandIssued(SpeechletRequestEnvelope<NextCommandIssuedRequest> requestEnvelope) {
        PlayState playState = getPlayStateFromPlaybackControllerRequest(requestEnvelope);
        if (playState != null) {
            return toolbox.playNextSong(playState, "");
        }
        return null;
    }

    @Override
    public SpeechletResponse onPauseCommandIssued(SpeechletRequestEnvelope<PauseCommandIssuedRequest> requestEnvelope) {
        AudioPlayerState state = requestEnvelope.getContext().getState(
            AudioPlayerInterface.class, AudioPlayerState.class);
        return toolbox.pauseAudio("", state);
    }

    @Override
    public SpeechletResponse onPlayCommandIssued(SpeechletRequestEnvelope<PlayCommandIssuedRequest> requestEnvelope) {
        SystemState systemState = requestEnvelope.getContext().getState(
            SystemInterface.class, SystemState.class);
        AudioPlayerState audioPlayerState = requestEnvelope.getContext().getState(
            AudioPlayerInterface.class, AudioPlayerState.class);
        PlayState playState;
        if (audioPlayerState != null) {
            playState = playHelper.getPlayStateByStreamToken(audioPlayerState.getToken());

        } else {
            playState = playHelper.getPlayStateBySystemState(systemState);
        }
        return toolbox.playLastSong(playState, "");
    }

    @Override
    public SpeechletResponse onPreviousCommandIssued(
        SpeechletRequestEnvelope<PreviousCommandIssuedRequest> requestEnvelope) {
        PlayState playState = getPlayStateFromPlaybackControllerRequest(requestEnvelope);
        if (playState != null) {
            return toolbox.playPreviousSong(playState, "");
        }
        return null;
    }
}
