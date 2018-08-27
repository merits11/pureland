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

import static java.util.concurrent.TimeUnit.MINUTES;
import static merits.funskills.pureland.model.Constants.SLOT_NAME_MINUTES;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ControlRequestHandlerTest {

    private ControlRequestHandler controlRequestHandler;

    @Mock
    private AudioPlayHelperV2 audioPlayHelper;

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
        HandlerInput input = buildInput("FastForward", MINUTES.toMillis(25), 10);
        Optional<Response> response = controlRequestHandler.handle(input);
        verifyOffset(2, MINUTES.toMillis(5), response);
    }

    @Test
    public void testFastForward() throws Exception {
        HandlerInput input = buildInput("FastForward", MINUTES.toMillis(5), 5);
        Optional<Response> response = controlRequestHandler.handle(input);
        verifyOffset(1, MINUTES.toMillis(10), response);
    }

    @Test
    public void testRewind() throws Exception {
        HandlerInput input = buildInput("Rewind", MINUTES.toMillis(15), 5);
        Optional<Response> response = controlRequestHandler.handle(input);
        verifyOffset(1, MINUTES.toMillis(10), response);
    }

    @Test
    public void testRewindToPrevious() throws Exception {
        HandlerInput input = buildInput("Rewind", MINUTES.toMillis(5), 10);
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

    private HandlerInput buildInput(String intent, long currentOffset, int minutes) {
        return HandlerInput.builder()
            .withRequestEnvelope(RequestEnvelope.builder()
                .withContext(Context.builder()
                    .withAudioPlayer(AudioPlayerState.builder()
                        .withOffsetInMilliseconds(currentOffset)
                        .withToken("Test")
                        .build())
                    .build())
                .withRequest(IntentRequest.builder()
                    .withIntent(Intent.builder()
                        .withName(intent)
                        .putSlotsItem(SLOT_NAME_MINUTES, Slot.builder().withName(SLOT_NAME_MINUTES)
                            .withValue(String.valueOf(minutes))
                            .build())
                        .build())
                    .build())
                .build())
            .build();
    }

}
