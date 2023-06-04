package merits.funskills.pureland.v2.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import merits.funskills.pureland.model.PlayListUtils;

import java.util.Optional;
import java.util.Set;

public class HelpRequestHandler extends BaseRequestHandler {
    @Override
    public boolean canHandle(HandlerInput input) {
        return intentNameInSet(input, Set.of("AMAZON.HelpIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        return input.getResponseBuilder()
                .withShouldEndSession(false)
                .withSpeech(text("app.help"))
                .withReprompt(text("play.reprompt"))
                .withSimpleCard(text("card.help.title"), PlayListUtils.getCardText())
                .build();

    }
}
