package merits.funskills.pureland.v2.handlers;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import merits.funskills.pureland.model.PlayListUtils;

public class HelpRequestHandler extends BaseRequestHandler {
    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.intentName("AMAZON.HelpIntent"));
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
