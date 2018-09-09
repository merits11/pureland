package merits.funskills.pureland.v2.handlers;

import java.util.Optional;

import org.junit.Test;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.ui.PlainTextOutputSpeech;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amazon.ask.response.ResponseBuilder;

import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.v2.AudioPlayHelperV2;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Log4j2
public class RequestErrorHandlerTest {

    @Test
    public void testOutputSpeech() {
        AudioPlayHelperV2 playHelperV2 = mock(AudioPlayHelperV2.class);
        //AudioPlayHelperV2 playHelperV2 = AudioPlayHelperV2.getInstance();
        HandlerInput input = mock(HandlerInput.class);
        when(input.getResponseBuilder()).thenReturn(new ResponseBuilder());
        when(input.getRequestEnvelope()).thenReturn(RequestEnvelope.builder().build());
        RequestErrorHandler handler = new RequestErrorHandler(playHelperV2);
        Optional<Response> responseOptional = handler
            .handle(input, new NullPointerException("Just a test"));
        assertTrue(responseOptional.isPresent());
        SsmlOutputSpeech outputSpeech = (SsmlOutputSpeech) responseOptional.get().getOutputSpeech();
        assertNotNull(outputSpeech);
        assertNotNull(outputSpeech.getSsml());
    }
}
