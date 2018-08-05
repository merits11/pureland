package merits.funskills.pureland;

import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletRequestHandler;
import com.amazon.speech.speechlet.SpeechletRequestHandlerException;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.speechlet.lambda.LambdaSpeechletRequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.model.PlayListUtils;
import merits.funskills.pureland.utils.AppConfig;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

@Log4j2
public class LambdaHandler implements RequestStreamHandler {
    private static final Set<String> supportedApplicationIds;

    static {
        /*
         * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
         * Alexa Skill and put the relevant Application Ids in this Set.
         */
        supportedApplicationIds = new HashSet<String>();
        supportedApplicationIds.add("amzn1.ask.skill.e256969a-f018-4ed5-967c-e231ecf51a81");
    }

    public LambdaHandler() {
        this(new PureLandMusicHandler(AudioPlayHelper.getInstance()), supportedApplicationIds);
    }


    private final SpeechletV2 speechlet;
    private final SpeechletRequestHandler speechletRequestHandler;

    private LambdaHandler(final SpeechletV2 speechlet, final Set<String> supportedApplicationIds) {
        this.speechlet = speechlet;
        this.speechletRequestHandler = new LambdaSpeechletRequestHandler(supportedApplicationIds);
    }

    @Override
    public final void handleRequest(final InputStream input, final OutputStream output, final Context context)
            throws IOException {
        AppConfig.init(context);
        byte[] serializedSpeechletRequest = IOUtils.toByteArray(input);
        byte[] outputBytes;
        final String inputJson = new String(serializedSpeechletRequest, Charsets.UTF_8);

        if (isS3Event(inputJson)) {
            AudioPlayHelper.getInstance().updateLibrary();
            return;
        }

        log.info("[DEBUGTAG] Request input: {}", inputJson);
        try {
            outputBytes =
                    speechletRequestHandler.handleSpeechletCall(speechlet,
                            serializedSpeechletRequest);
        } catch (SpeechletRequestHandlerException | SpeechletException ex) {
            throw new RuntimeException(ex);
        }
        final String outputJson = new String(outputBytes, Charsets.UTF_8);
        log.info("[DEBUGTAG] Request output: {}", outputJson);
        output.write(outputBytes);
    }

    private boolean isS3Event(final String json) {
        return json.contains(PlayListUtils.getBucket()) && json.contains("s3");
    }
}
