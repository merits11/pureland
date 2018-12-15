package merits.funskills.pureland.v2.handlers;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.interfaces.audioplayer.AudioPlayerState;
import com.amazon.ask.model.interfaces.audioplayer.PlayDirective;

import merits.funskills.pureland.model.PlayItem;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayState;
import merits.funskills.pureland.v2.AudioPlayHelperV2;
import merits.funskills.pureland.v2.PureLandMusicHelperV2;

import static java.util.concurrent.TimeUnit.MINUTES;
import static merits.funskills.pureland.model.Constants.SLOT_NAME_MINUTES;
import static merits.funskills.pureland.model.Constants.SLOT_NAME_SEQUENCE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ControlRequestHandlerTest {

    private ControlRequestHandler controlRequestHandler;

    @Mock
    private AudioPlayHelperV2 audioPlayHelper;

    @Mock
    private PureLandMusicHelperV2 toolbox;

    @Captor
    private ArgumentCaptor<PlayState> playStateArgumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        controlRequestHandler = new ControlRequestHandler(audioPlayHelper);
        PlayState playState = PlayState.builder()
            .token("Test")
            .currentList(PlayList.SutraChantings.toString())
            .currentSeq(1)
            .build();
        when(audioPlayHelper.getPlayStateByStreamToken(any())).thenReturn(playState);

        when(audioPlayHelper.getPlayItem(any(), eq(0))).thenReturn(
            PlayItem.builder()
                .listName(PlayList.SutraChantings)
                .seqNo(0)
                .size(16 * MINUTES.toMillis(20))
                .build());
        when(audioPlayHelper.getPlayItem(any(), eq(1))).thenReturn(
            PlayItem.builder()
                .listName(PlayList.SutraChantings)
                .seqNo(1)
                .size(16 * MINUTES.toMillis(30))
                .build());
        when(audioPlayHelper.getPlayItem(any(), eq(2))).thenReturn(
            PlayItem.builder()
                .listName(PlayList.SutraChantings)
                .seqNo(2)
                .size(16 * MINUTES.toMillis(15))
                .build());
    }

    @Test
    public void testFastForwardToNext() throws Exception {
        HandlerInput input = buildForwardInput("FastForward", MINUTES.toMillis(25), 10);
        Optional<Response> response = controlRequestHandler.handle(input);
        verifyOffset(2, MINUTES.toMillis(5), response);
    }

    @Test
    public void testFastForward() throws Exception {
        HandlerInput input = buildForwardInput("FastForward", MINUTES.toMillis(5), 5);
        Optional<Response> response = controlRequestHandler.handle(input);
        verifyOffset(1, MINUTES.toMillis(10), response);
    }

    @Test
    public void testRewind() throws Exception {
        HandlerInput input = buildForwardInput("Rewind", MINUTES.toMillis(15), 5);
        Optional<Response> response = controlRequestHandler.handle(input);
        verifyOffset(1, MINUTES.toMillis(10), response);
    }

    @Test
    public void testGotoItem() throws Exception {
        controlRequestHandler = new ControlRequestHandler(audioPlayHelper, toolbox);
        when(audioPlayHelper.getFlooredDisplaySequence(eq(PlayList.Ten), eq(3))).thenReturn(3);
        when(audioPlayHelper.getMaxDisplaySequence(eq(PlayList.Ten))).thenReturn(15);
        HandlerInput input = buildInput("GotoItem", 5, SLOT_NAME_SEQUENCE, 3);
        controlRequestHandler.handle(input);
        verify(toolbox, times(1)).playNextSong(playStateArgumentCaptor.capture(), anyString());
        PlayState playState = playStateArgumentCaptor.getValue();
        assertEquals(1, playState.getCurrentSeq().intValue());
        assertEquals(0, playState.getOffsetInMs());
    }

    @Test
    public void testGotoItemOverMax() throws Exception {
        controlRequestHandler = new ControlRequestHandler(audioPlayHelper, toolbox);
        when(audioPlayHelper.getFlooredDisplaySequence(eq(PlayList.Ten), eq(15))).thenReturn(10);
        when(audioPlayHelper.getMaxDisplaySequence(eq(PlayList.Ten))).thenReturn(10);
        HandlerInput input = buildInput("GotoItem", 5, SLOT_NAME_SEQUENCE, 15);
        controlRequestHandler.handle(input);
        verify(toolbox, times(1)).playNextSong(playStateArgumentCaptor.capture(), anyString());
        PlayState playState = playStateArgumentCaptor.getValue();
        assertEquals(8, playState.getCurrentSeq().intValue());
        assertEquals(0, playState.getOffsetInMs());
    }

    @Test
    public void testRewindToPrevious() throws Exception {
        HandlerInput input = buildForwardInput("Rewind", MINUTES.toMillis(5), 10);
        Optional<Response> response = controlRequestHandler.handle(input);
        verifyOffset(0, MINUTES.toMillis(15), response);
    }

    private void verifyOffset(int seq, long offset, Optional<Response> response) {
        PlayDirective playDirective = getPlayDirective(response);
        assertEquals(playDirective.getAudioItem().getStream().getOffsetInMilliseconds().longValue(), offset);
        verify(audioPlayHelper, times(1)).savePlayState(playStateArgumentCaptor.capture());
        PlayState playState = playStateArgumentCaptor.getValue();
        assertEquals(playState.getOffsetInMs(), offset);
        assertEquals(playState.getCurrentSeq().intValue(), seq);
    }

    private PlayDirective getPlayDirective(Optional<Response> response) {
        return (PlayDirective) response.get().getDirectives().stream()
            .filter(d -> d instanceof PlayDirective).findFirst()
            .get();
    }

    private HandlerInput buildInput(String intent, long currentOffset, String slotName, int slotVal) {
        PlayState playState = PlayState.builder()
            .token("391d4162-5e51-4444-beed-7b2239d970b4")
            .currentList(PlayList.Ten.toString())
            .currentSeq(1)
            .build();
        when(audioPlayHelper.getPlayStateByStreamToken(eq("391d4162-5e51-4444-beed-7b2239d970b4,Ten,1")))
            .thenReturn(playState);
        return HandlerInput.builder()
            .withRequestEnvelope(RequestEnvelope.builder()
                .withContext(Context.builder()
                    .withAudioPlayer(AudioPlayerState.builder()
                        .withOffsetInMilliseconds(currentOffset)
                        .withToken("391d4162-5e51-4444-beed-7b2239d970b4,Ten,1")
                        .build())
                    .build())
                .withRequest(IntentRequest.builder()
                    .withIntent(Intent.builder()
                        .withName(intent)
                        .putSlotsItem(slotName, Slot.builder().withName(slotName)
                            .withValue(String.valueOf(slotVal))
                            .build())
                        .build())
                    .build())
                .build())
            .build();
    }

    private HandlerInput buildForwardInput(String intent, long currentOffset, int minutes) {
        return buildInput(intent, currentOffset, SLOT_NAME_MINUTES, minutes);
    }

}
