package merits.funskills.pureland.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import com.amazon.ask.Skill;
import com.amazon.ask.Skills;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.ResponseEnvelope;
import com.amazon.ask.model.services.Serializer;
import com.amazon.ask.util.JacksonSerializer;

import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.model.PlayListUtils;
import merits.funskills.pureland.utils.AppConfig;
import merits.funskills.pureland.v2.handlers.CanFulfillHandler;
import merits.funskills.pureland.v2.handlers.ControlRequestHandler;
import merits.funskills.pureland.v2.handlers.CustomNameRequestHandler;
import merits.funskills.pureland.v2.handlers.GeneralCategoryRequestHandler;
import merits.funskills.pureland.v2.handlers.HelpRequestHandler;
import merits.funskills.pureland.v2.handlers.LanguageRequestHandler;
import merits.funskills.pureland.v2.handlers.LaunchRequestHandler;
import merits.funskills.pureland.v2.handlers.PlaybackRequestHandler;
import merits.funskills.pureland.v2.handlers.PlaylistRequestHandler;
import merits.funskills.pureland.v2.handlers.RequestErrorHandler;
import merits.funskills.pureland.v2.handlers.SessionEndedRequestHandler;

@Log4j2
public class LambdaHandlerV2 implements RequestStreamHandler {

    private static final String SKILL_ID = "amzn1.ask.skill.e256969a-f018-4ed5-967c-e231ecf51a81";

    private Skill getSkill() {
        return Skills.standard()
            .addRequestHandlers(
                new LaunchRequestHandler(),
                new PlaylistRequestHandler(),
                new CustomNameRequestHandler(),
                new GeneralCategoryRequestHandler(),
                new ControlRequestHandler(),
                new PlaybackRequestHandler(),
                new LanguageRequestHandler(),
                new HelpRequestHandler(),
                new SessionEndedRequestHandler(),
                new CanFulfillHandler()
            )
            .addExceptionHandler(new RequestErrorHandler())
            .withAutoCreateTable(false)
            .withSkillId(SKILL_ID)
            .build();
    }

    private final Serializer serializer;

    public LambdaHandlerV2() {
        this.serializer = new JacksonSerializer();
    }

    @Override
    public final void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        AppConfig.init(context);
        byte[] serializedSpeechletRequest = IOUtils.toByteArray(input);
        final String inputJson = new String(serializedSpeechletRequest, StandardCharsets.UTF_8);
        log.info("Request received: {}", inputJson);
        if (isS3Event(inputJson)) {
            AudioPlayHelperV2.getInstance().updateLibrary();
            return;
        }
        RequestEnvelope requestEnvelope = serializer.deserialize(inputJson, RequestEnvelope.class);
        AppConfig.setLocale(requestEnvelope.getRequest().getLocale());
        ResponseEnvelope response = getSkill().invoke(requestEnvelope);
        log.info("Our response: {}", serializer.serialize(response));
        serializer.serialize(response, output);
    }

    private boolean isS3Event(final String json) {
        if (json.contains("amzn1.ask.")) {
            return false;
        }
        return json.contains(PlayListUtils.getBucket()) && json.contains("s3");
    }
}
