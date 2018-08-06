package merits.funskills.pureland.v2.handlers;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.SessionEndedRequest;
import com.amazon.ask.request.Predicates;

public class SessionEndedRequestHandler extends BaseRequestHandler {
    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.requestType(SessionEndedRequest.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        return input.getResponseBuilder().build();
    }
}
