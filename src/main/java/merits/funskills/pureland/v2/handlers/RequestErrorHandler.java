package merits.funskills.pureland.v2.handlers;

import java.util.Optional;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.Serializer;
import com.amazon.ask.util.JacksonSerializer;

import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.v2.Speeches;

@Log4j2
public class RequestErrorHandler implements ExceptionHandler {

    private Speeches speeches = Speeches.getSpeeches();
    private Serializer serializer = new JacksonSerializer();

    @Override
    public boolean canHandle(HandlerInput input, Throwable throwable) {
        return true;
    }

    @Override
    public Optional<Response> handle(HandlerInput input, Throwable throwable) {
        log.error("Exception Error Happened: " + throwable.getMessage(), throwable);
        log.error("Exception Error Happened with input: \n{}", serializer.serialize(input.getRequestEnvelope()));
        return input.getResponseBuilder()
            .withSpeech(speeches.get("error.exception"))
            .build();
    }
}
