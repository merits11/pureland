package merits.funskills.pureland;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.Context;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioPlayerState;
import com.amazon.speech.speechlet.interfaces.audioplayer.PlayBehavior;
import com.amazon.speech.speechlet.interfaces.audioplayer.PlayerActivity;
import com.amazon.speech.speechlet.interfaces.audioplayer.Stream;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackFailedRequest;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackNearlyFinishedRequest;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackStartedRequest;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SimpleCard;

import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.model.NameMapping;
import merits.funskills.pureland.model.PlayItem;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayListUtils;
import merits.funskills.pureland.model.PlayState;
import merits.funskills.pureland.model.Tag;
import merits.funskills.pureland.model.Token;
import merits.funskills.pureland.model.UserSetting;

import static merits.funskills.pureland.model.Tag.Chinese;
import static merits.funskills.pureland.model.Tag.English;
import static merits.funskills.pureland.model.Tag.Private;
import static merits.funskills.pureland.model.Tag.Sutra;
import static merits.funskills.pureland.model.Tag.TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Log4j2
public class PureLandMusicHandlerTestAlpha extends AlphaAwsBaseTestCase {

    private static final Integer INITIAL_SEQ = 5;
    private AudioPlayerState audioPlayerState;
    private PlayState initialPlayState;
    private PureLandMusicHandler pureLandMusicHandler;

    @Before
    public void setup() {
        initSystemState();
        pureLandMusicHandler = new PureLandMusicHandler(audioPlayHelper);
        initialPlayState = defaultPlayState()
            .currentSeq(INITIAL_SEQ)
            .currentList("OneHundredFour")
            .offsetInMs(500L)
            .build();
        audioPlayerState = AudioPlayerState.builder()
            .withOffsetInMilliseconds(505L)
            .withPlayerActivity(PlayerActivity.STOPPED)
            .withToken(Token.fromPlayState(initialPlayState).getStreamToken())
            .build();
    }

    @Test
    public void testHandleLoopRequest() throws Exception {
        audioPlayHelper.savePlayState(initialPlayState);
        SpeechletResponse speechletResponse = pureLandMusicHandler.handleLoopOnRequest(audioPlayerState, systemState);
        PlayState dbPlayState = getPlayStateFromDynamo();
        assertTrue(dbPlayState.isRepeat());
        assertEquals(INITIAL_SEQ, dbPlayState.getCurrentSeq());
        speechletResponse = pureLandMusicHandler.handleLoopOffRequest(audioPlayerState, systemState);
        dbPlayState = getPlayStateFromDynamo();
        assertFalse(dbPlayState.isRepeat());
    }

    @Test
    public void testHandleResumeRequestWithToken() throws Exception {
        initialPlayState.setPaused(true);
        audioPlayHelper.savePlayState(initialPlayState);
        SpeechletResponse speechletResponse = pureLandMusicHandler.handleResumeRequest(audioPlayerState, systemState);
        assertEquals(1, speechletResponse.getDirectives().size());
        PlayDirective directive = (PlayDirective) speechletResponse.getDirectives().get(0);
        assertEquals(PlayBehavior.REPLACE_ALL, directive.getPlayBehavior());
        Stream stream = getStreamFromResponse(speechletResponse);
        assertEquals(initialPlayState.getOffsetInMs(), stream.getOffsetInMilliseconds());
        Token token = Token.fromStreamToken(stream.getToken());
        assertEquals(INITIAL_SEQ.intValue(), token.getListSequence());
        PlayState newPlayState = audioPlayHelper.getPlayStateByStreamToken(stream.getToken());
        assertEquals(INITIAL_SEQ, newPlayState.getCurrentSeq());
        assertFalse(newPlayState.isPaused());
        assertNotEquals(newPlayState.getToken(), initialPlayState.getToken());
        assertNull(getPlayStateFromDynamo());
    }

    @Test
    public void testHandleNextRequest() throws Exception {
        audioPlayHelper.savePlayState(initialPlayState);
        String streamTokentoken = Token.fromPlayState(initialPlayState).getStreamToken();
        audioPlayerState = AudioPlayerState.builder()
            .withOffsetInMilliseconds(initialPlayState.getOffsetInMs() + 500)
            .withPlayerActivity(PlayerActivity.STOPPED)
            .withToken(streamTokentoken)
            .build();
        SpeechletResponse speechletResponse = pureLandMusicHandler.handleNextIntent(audioPlayerState, true);
        assertEquals(1, speechletResponse.getDirectives().size());
        PlayDirective directive = (PlayDirective) speechletResponse.getDirectives().get(0);
        assertEquals(PlayBehavior.REPLACE_ALL, directive.getPlayBehavior());
        Stream stream = getStreamFromResponse(speechletResponse);
        Token token = Token.fromStreamToken(stream.getToken());
        PlayState playstateAfterNext = audioPlayHelper.getPlayStateByStreamToken(stream.getToken());
        if (token.getListSequence() == INITIAL_SEQ) {
            assertTrue(stream.getOffsetInMilliseconds() > audioPlayerState.getOffsetInMilliseconds());
            assertEquals(INITIAL_SEQ, playstateAfterNext.getCurrentSeq());
        } else {
            assertEquals(INITIAL_SEQ + 1, token.getListSequence());
            assertEquals(INITIAL_SEQ + 1, playstateAfterNext.getCurrentSeq().intValue());
        }

        assertFalse(playstateAfterNext.isPaused());
        assertNotEquals(playstateAfterNext.getToken(), initialPlayState.getToken());
        assertNull(getPlayStateFromDynamo());

        audioPlayerState = AudioPlayerState.builder()
            .withToken(Token.fromPlayState(playstateAfterNext).getStreamToken())
            .withOffsetInMilliseconds(playstateAfterNext.getOffsetInMs())
            .withPlayerActivity(PlayerActivity.STOPPED)
            .build();
        speechletResponse = pureLandMusicHandler.handleNextIntent(audioPlayerState, false);
        stream = getStreamFromResponse(speechletResponse);
        Token newToken = Token.fromStreamToken(stream.getToken());

        if (newToken.getListSequence() == playstateAfterNext.getCurrentSeq()) {
            assertTrue(stream.getOffsetInMilliseconds() < playstateAfterNext.getOffsetInMs());
        } else {
            assertEquals(newToken.getListSequence(), playstateAfterNext.getCurrentSeq() - 1);
        }
        assertNotEquals(playstateAfterNext.getToken(), newToken.getUuid());
    }

    @Test
    public void testHandleResumeRequestWithoutToken() throws Exception {
        initialPlayState.setPaused(true);
        audioPlayHelper.savePlayState(initialPlayState);
        SpeechletResponse speechletResponse = pureLandMusicHandler.handleResumeRequest(null, systemState);
        assertEquals(1, speechletResponse.getDirectives().size());
        PlayDirective directive = (PlayDirective) speechletResponse.getDirectives().get(0);
        assertEquals(PlayBehavior.REPLACE_ALL, directive.getPlayBehavior());
        Stream stream = getStreamFromResponse(speechletResponse);
        Token token = Token.fromStreamToken(stream.getToken());
        assertEquals(INITIAL_SEQ.intValue(), token.getListSequence());
        PlayState newPlayState = audioPlayHelper.getPlayStateByStreamToken(stream.getToken());
        assertEquals(INITIAL_SEQ, newPlayState.getCurrentSeq());
        assertFalse(newPlayState.isPaused());
        assertNotEquals(newPlayState.getToken(), initialPlayState.getToken());
        assertNull(getPlayStateFromDynamo());
    }

    @Test
    public void testHandleEnqueRequest() throws Exception {
        audioPlayHelper.savePlayState(initialPlayState);
        Token token = Token.fromPlayState(initialPlayState);
        PlaybackNearlyFinishedRequest request = PlaybackNearlyFinishedRequest.builder()
            .withToken(token.getStreamToken())
            .withRequestId("0001")
            .build();
        SpeechletRequestEnvelope<PlaybackNearlyFinishedRequest> envelope1 =
            SpeechletRequestEnvelope.<PlaybackNearlyFinishedRequest>builder()
                .withRequest(request)
                .build();

        SpeechletResponse speechletResponse = pureLandMusicHandler.onPlaybackNearlyFinished(envelope1);
        Stream stream = getStreamFromResponse(speechletResponse);
        Token newToken = Token.fromStreamToken(stream.getToken());
        assertEquals(token.getUuid(), newToken.getUuid());
        assertEquals(INITIAL_SEQ + 1, newToken.getListSequence());
        PlayState dbPlaystate = getPlayStateFromResponse(speechletResponse);
        assertEquals(INITIAL_SEQ, dbPlaystate.getCurrentSeq());
        assertEquals(INITIAL_SEQ + 1, dbPlaystate.getNextSeq().intValue());

        PlaybackStartedRequest startedRequest = PlaybackStartedRequest.builder()
            .withToken(stream.getToken())
            .withRequestId("0002")
            .build();
        SpeechletRequestEnvelope<PlaybackStartedRequest> envelope2 =
            SpeechletRequestEnvelope.<PlaybackStartedRequest>builder()
                .withRequest(startedRequest)
                .build();
        speechletResponse = pureLandMusicHandler.onPlaybackStarted(envelope2);
        dbPlaystate = audioPlayHelper.getPlayStateByStreamToken(stream.getToken());
        assertEquals(INITIAL_SEQ + 1, dbPlaystate.getCurrentSeq().intValue());
        assertNull(dbPlaystate.getNextSeq());
    }

    @Test
    public void testHandleStarOverRequest() throws Exception {
        audioPlayHelper.savePlayState(initialPlayState);
        SpeechletResponse speechletResponse = pureLandMusicHandler.handleStartOverRequest(
            systemState, audioPlayerState);
        PlayDirective playDirective = (PlayDirective) speechletResponse.getDirectives().get(0);
        assertEquals(PlayBehavior.REPLACE_ALL, playDirective.getPlayBehavior());
        Stream stream = getStreamFromResponse(speechletResponse);
        Token token = Token.fromStreamToken(stream.getToken());
        assertEquals(0, token.getListSequence());
        assertEquals(0L, stream.getOffsetInMilliseconds());
        PlayState dbPlayState = getPlayStateFromResponse(speechletResponse);
        assertEquals(Integer.valueOf(0), dbPlayState.getCurrentSeq());
    }

    @Test
    public void testHandleShufflePrivateListRequest() throws Exception {
        UserSetting language = UserSetting.builder()
            .userId(TEST_USER_ID)
            .language("Chinese")
            .build();
        audioPlayHelper.saveUserSettings(language);
        audioPlayHelper.savePlayState(initialPlayState);
        SpeechletResponse speechletResponse = pureLandMusicHandler.handleShuffleOnRequest(
            audioPlayerState, systemState);
        PlayDirective playDirective = (PlayDirective) speechletResponse.getDirectives().get(0);
        assertEquals(PlayBehavior.REPLACE_ALL, playDirective.getPlayBehavior());
        Stream stream = getStreamFromResponse(speechletResponse);
        Token token = Token.fromStreamToken(stream.getToken());

        PlayList newPlayList = PlayListUtils.getPlaylist(token.getListName());
        log.info("New play list: {}", newPlayList);
        assertTrue(newPlayList.isTagged(Private));
        assertNotEquals(newPlayList, initialPlayState.currentPlayList());
        List<Tag> contentTags = initialPlayState.currentPlayList().getTags()
            .stream().filter(Tag::isContent)
            .collect(Collectors.toList());
        assertFalse(contentTags.isEmpty());
        assertTrue(newPlayList.isTagged(contentTags));
        assertEquals(initialPlayState.currentPlayList().isAccessible(Collections.emptyList()),
            newPlayList.isAccessible(Collections.emptyList()));
    }

    @Test
    public void testHandleShuffleOpenListRequest() throws Exception {
        initialPlayState = defaultPlayState()
            .currentSeq(INITIAL_SEQ)
            .currentList("Five")
            .offsetInMs(500L)
            .build();
        audioPlayerState = AudioPlayerState.builder()
            .withOffsetInMilliseconds(505L)
            .withPlayerActivity(PlayerActivity.STOPPED)
            .withToken(Token.fromPlayState(initialPlayState).getStreamToken())
            .build();
        audioPlayHelper.savePlayState(initialPlayState);
        SpeechletResponse speechletResponse = pureLandMusicHandler.handleShuffleOnRequest(
            audioPlayerState, systemState);
        PlayDirective playDirective = (PlayDirective) speechletResponse.getDirectives().get(0);
        assertEquals(PlayBehavior.REPLACE_ALL, playDirective.getPlayBehavior());
        Stream stream = getStreamFromResponse(speechletResponse);
        Token token = Token.fromStreamToken(stream.getToken());

        PlayList newPlayList = PlayListUtils.getPlaylist(token.getListName());
        assertFalse(newPlayList.isTagged(Private));
        log.info("New play list: {}", newPlayList);
        assertNotEquals(newPlayList, initialPlayState.currentPlayList());
        List<Tag> contentTags = initialPlayState.currentPlayList().getTags()
            .stream().filter(Tag::isContent)
            .collect(Collectors.toList());
        assertFalse(contentTags.isEmpty());
        assertTrue(newPlayList.isTagged(contentTags));
        assertEquals(initialPlayState.currentPlayList().isAccessible(Collections.emptyList()),
            newPlayList.isAccessible(Collections.emptyList()));
    }

    @Test
    public void testHandleFailedRequest() throws Exception {
        audioPlayHelper.savePlayState(initialPlayState);
        String token = Token.fromPlayState(initialPlayState).getStreamToken();
        PlaybackFailedRequest failedRequest = PlaybackFailedRequest.builder()
            .withToken(token)
            .withRequestId("failedRequestId")
            .build();
        long failedTimeMs = initialPlayState.getOffsetInMs() + 500;
        AudioPlayerState stateWhenFailed = AudioPlayerState.builder()
            .withOffsetInMilliseconds(failedTimeMs)
            .withPlayerActivity(PlayerActivity.STOPPED)
            .withToken(token)
            .build();
        SpeechletRequestEnvelope<PlaybackFailedRequest> envelope =
            SpeechletRequestEnvelope.<PlaybackFailedRequest>builder()
                .withContext(Context.builder()
                    .addState(stateWhenFailed)
                    .build())
                .withRequest(failedRequest)
                .build();

        SpeechletResponse speechletResponse = pureLandMusicHandler.onPlaybackFailed(
            envelope);
        Stream stream = getStreamFromResponse(speechletResponse);
        Token tokenObject = Token.fromStreamToken(stream.getToken());
        assertEquals(initialPlayState.getCurrentSeq().intValue(), tokenObject.getListSequence());
        assertEquals(failedTimeMs, stream.getOffsetInMilliseconds());
    }

    @Test
    public void testOnLaunch() throws Exception {
        LaunchRequest launchRequest = LaunchRequest.builder().withRequestId("1234").build();
        SpeechletRequestEnvelope<LaunchRequest> requestEnvelope = SpeechletRequestEnvelope.<LaunchRequest>builder()
            .withRequest(launchRequest)
            .withContext(Context.builder().addState(unkownUserSystemState).build())
            .build();
        SpeechletResponse speechletResponse = pureLandMusicHandler.onLaunch(requestEnvelope);
        PlainTextOutputSpeech txtSpeech = (PlainTextOutputSpeech) speechletResponse.getOutputSpeech();
        //assertTrue(txtSpeech.getText().contains(PlayListUtils.getNumberedListsDescription()));
    }

    @Test
    public void testPlayItem() throws Exception {
        PlayItem playItem = audioPlayHelper.getPlayItem(PlayList.OneHundredFour, 1000);
        log.info("Play item: {}", playItem);
        assertTrue(playItem.getSeqNo() < 1000);
    }

    @Test
    public void testHelpIntent() throws Exception {
        TAG(English, Chinese).stream().forEach(tag -> {
            UserSetting language = UserSetting.builder()
                .userId(systemState.getUser().getUserId())
                .language(tag.toString())
                .build();
            audioPlayHelper.saveUserSettings(language);
            SpeechletResponse speechletResponse = pureLandMusicHandler.handleHelpRequest(systemState);
            SimpleCard simpleCard = (SimpleCard) speechletResponse.getCard();
            PlainTextOutputSpeech txtSpeech = (PlainTextOutputSpeech) speechletResponse.getOutputSpeech();
            final String speechTxt = txtSpeech.getText();
            log.info("Card title: {}", simpleCard.getTitle());
            log.info("Card contents: {}", simpleCard.getContent());
            log.info("Help speech: {}", speechTxt);

            Matcher matcher = Pattern.compile("[0-9]+").matcher(speechTxt);
            List<PlayList> playLists = extractPlayListFromText(speechTxt);
            for (PlayList playList : playLists) {
                log.info("List in help: {}", playList);
                assertTrue(playList.isTagged(tag));
            }
        });
    }

    @Test
    public void testTaggedIntent() throws Exception {
        TAG(English, Chinese).stream().forEach(tag -> {
            UserSetting language = UserSetting.builder()
                .userId(systemState.getUser().getUserId())
                .language(tag.toString())
                .build();
            audioPlayHelper.saveUserSettings(language);
            SpeechletResponse speechletResponse = pureLandMusicHandler.handleTagIntent(systemState,
                TAG(Sutra), "XYZ");
            PlainTextOutputSpeech txtSpeech = (PlainTextOutputSpeech) speechletResponse.getOutputSpeech();
            final String speechTxt = txtSpeech.getText();

            List<PlayList> playLists = extractPlayListFromText(speechTxt);
            assertTrue(playLists.size() == 1);
            assertTrue(playLists.get(0).isTagged(TAG(Sutra, tag)));
        });
    }

    @Test
    public void testCustomNameIntent() throws Exception {
        NameMapping.getNames().stream().forEach(name -> {
            Slot slot1 = Slot.builder().withName("SutraChanting").build();
            Slot slot2 = Slot.builder().withName("EarthStore").build();
            Slot slot3 = Slot.builder().withName(name).withValue(name).build();
            Intent intent = Intent.builder().withName("CustomNameIntent")
                .withSlot(slot1).withSlot(slot2).withSlot(slot3).build();
            SpeechletResponse speechletResponse = pureLandMusicHandler.handleCustomNameIntent(
                intent, null, systemState);
            PlainTextOutputSpeech txtSpeech = (PlainTextOutputSpeech) speechletResponse.getOutputSpeech();
            final String speechTxt = txtSpeech.getText();

            List<PlayList> playLists = extractPlayListFromText(speechTxt);
            assertTrue(playLists.size() == 1);
            PlayList playList = playLists.get(0);
            assertTrue(NameMapping.getPlayLists(name).contains(playList));
        });
    }

    private List<PlayList> extractPlayListFromText(final String speechTxt) {
        List<PlayList> playLists = Lists.newArrayList();
        Matcher matcher = Pattern.compile("[0-9]+").matcher(speechTxt);
        while (matcher.find()) {
            final String result = speechTxt.substring(matcher.start(), matcher.end());
            PlayList playList = PlayListUtils.getPlaylist(result);
            playLists.add(playList);
        }
        return playLists;
    }
}
