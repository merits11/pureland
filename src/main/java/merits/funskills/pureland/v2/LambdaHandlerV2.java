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

import merits.funskills.pureland.model.PlayListUtils;
import merits.funskills.pureland.utils.AppConfig;
import merits.funskills.pureland.v2.handlers.ControlRequestHandler;
import merits.funskills.pureland.v2.handlers.CustomNameRequestHandler;
import merits.funskills.pureland.v2.handlers.GeneralCategoryRequestHandler;
import merits.funskills.pureland.v2.handlers.HelpRequestHandler;
import merits.funskills.pureland.v2.handlers.LanguageRequestHandler;
import merits.funskills.pureland.v2.handlers.LaunchRequestHandler;
import merits.funskills.pureland.v2.handlers.PlaybackRequestHandler;
import merits.funskills.pureland.v2.handlers.PlaylistRequestHandler;

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
                new HelpRequestHandler()
            )
            .withAutoCreateTable(false)
            .withSkillId(SKILL_ID)
            .build();
    }

    private final Skill skill;
    private final Serializer serializer;

    public LambdaHandlerV2() {
        this.serializer = new JacksonSerializer();
        this.skill = getSkill();
    }

    @Override
    public final void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        AppConfig.init(context);
        byte[] serializedSpeechletRequest = IOUtils.toByteArray(input);
        final String inputJson = new String(serializedSpeechletRequest, StandardCharsets.UTF_8);
        if (isS3Event(inputJson)) {
            AudioPlayHelperV2.getInstance().updateLibrary();
            return;
        }
        RequestEnvelope requestEnvelope = serializer.deserialize(inputJson, RequestEnvelope.class);
        ResponseEnvelope response = skill.invoke(requestEnvelope);
        serializer.serialize(response, output);
    }

    private boolean isS3Event(final String json) {
        if (json.contains("amzn1.ask.")) {
            return false;
        }
        return json.contains(PlayListUtils.getBucket()) && json.contains("s3");
    }
}
