package merits.funskills.pureland.v2;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.audioplayer.PlayBehavior;
import com.amazon.ask.model.interfaces.audioplayer.StopDirective;
import com.amazon.ask.model.interfaces.audioplayer.Stream;
import com.amazon.ask.response.ResponseBuilder;

public class ResponseHelperV2 {

    public Optional<Response> askResponse(String speechText, String repromptText) {
        return new ResponseBuilder()
            .withSpeech(speechText)
            .withReprompt(repromptText)
            .withShouldEndSession(false)
            .build();

    }

    public Optional<Response> newPlayResponse(
        final String speechText,
        final Stream stream,
        final PlayBehavior playBehavior) {
        return newPlayResponse(speechText, stream, playBehavior, null, null);
    }

    public Optional<Response> newPlayResponse(
        final String speechText,
        final Stream stream,
        final PlayBehavior playBehavior,
        final String cardTitle,
        final String cardText) {
        ResponseBuilder builder = new ResponseBuilder();
        if (StringUtils.isNotBlank(speechText)) {
            builder.withSpeech(speechText);
        }
        builder.addAudioPlayerPlayDirective(
            playBehavior, stream.getOffsetInMilliseconds(), stream.getExpectedPreviousToken(), stream.getToken(),
            stream.getUrl());
        if (StringUtils.isNotBlank(cardTitle)) {
            builder.withSimpleCard(cardTitle, cardText);
        }
        return builder.build();
    }

    public Optional<Response> newStopResponse(String speechText) {
        ResponseBuilder builder = new ResponseBuilder()
            .addDirective(StopDirective.builder().build());
        if (StringUtils.isNotBlank(speechText)) {
            builder.withSpeech(speechText);
        }
        return builder.build();
    }
}
