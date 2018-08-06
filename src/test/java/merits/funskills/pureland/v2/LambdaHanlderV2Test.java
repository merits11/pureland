package merits.funskills.pureland.v2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.Context;

import com.amazon.ask.model.Response;
import com.amazon.ask.model.ResponseEnvelope;
import com.amazon.ask.model.interfaces.audioplayer.PlayBehavior;
import com.amazon.ask.model.interfaces.audioplayer.PlayDirective;
import com.amazon.ask.model.services.Serializer;
import com.amazon.ask.util.JacksonSerializer;

import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayListUtils;
import merits.funskills.pureland.model.PlayState;
import merits.funskills.pureland.utils.AppConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@Log4j2
public class LambdaHanlderV2Test {

    private LambdaHandlerV2 lambdaHandler;
    private AudioPlayHelperV2 audioPlayHelper;

    private static String userId;
    private static String deviceId;

    private Serializer serializer = new JacksonSerializer();

    @Mock
    private Context context;

    @BeforeClass
    public static void init() {
        userId = String.format("TestUser%d", System.currentTimeMillis());
        deviceId = String.format("TestDevice%d", System.currentTimeMillis());
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getFunctionName()).thenReturn("pureland-Alpha");
        AppConfig.init(context);
        lambdaHandler = new LambdaHandlerV2();
        audioPlayHelper = AudioPlayHelperV2.getInstance();
        PlayState.PlayStateBuilder builder = PlayState.builder()
            .userid(userId)
            .deviceId(deviceId)
            .offsetInMs(500)
            .currentSeq(1);
        audioPlayHelper.savePlayState(builder
            .token("Valid:" + System.currentTimeMillis())
            .currentList(PlayList.SutraChantings.name())
            .build());
        audioPlayHelper.savePlayState(builder
            .token("InValid:" + System.currentTimeMillis())
            .currentList("NotExists")
            .build());
    }

    public Response handleRequest(final String input) {
        final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        try {
            lambdaHandler.handleRequest(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                byteOutputStream, context);
            return parseOutput(byteOutputStream.toString(StandardCharsets.UTF_8.name()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    @Test
    public void testTagIntents() throws Exception {
        //{"RandomList", "DharmaTalk", "SutraIntent", "MusicIntent", "ChantIntent"};
        String[] tagIntents = { "RandomList", "DharmaTalk", "SutraIntent", "MusicIntent", "ChantIntent" };
        Arrays.stream(tagIntents).forEach(intent -> {
            testIntent(intent, "");
        });
    }

    @Test
    public void testUpdateLibrary() throws Exception {
        final String input = "{\"s3\":\"purelandmusic\"}";
        final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        lambdaHandler.handleRequest(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            byteOutputStream, context);
        String output = new String(byteOutputStream.toByteArray());
        log.info("testUpdateLibrary output: {}", output);
    }

    @Test
    public void testMorningEveningService() throws Exception {
        testIntent("MorningService", "", true);
        testIntent("EveningService", "", false);
    }

    @Test
    public void testLaunchRequest() throws Exception {
        Response speechletResponse = handleRequest(getResource("Launch.js"));
        assertNotNull(speechletResponse.getOutputSpeech());
    }

    @Test
    public void testPublicListIntents() throws Exception {
        PlayListUtils.getPublicNonVirtualLists()
            .stream().filter(pl -> pl.getListNumber() > 0)
            .forEach(pl ->
                {
                    log.info("Testing {}", pl);
                    try {
                        testIntent("PlayList", String.valueOf(pl.getListNumber()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("Test failed for list " + pl);
                    }
                }
            );
    }

    @Test
    public void testVirtualListIntent() throws Exception {
        testIntent("PlayList", String.valueOf(PlayList.AllMusic));
    }

    @Test
    public void testPrivelListIntent() throws Exception {
        testIntent("PlayList", String.valueOf(PlayList.PersonalList));
    }

    private void testIntent(final String intent, final String listNum) {
        testIntent(intent, listNum, false);
    }

    private void testIntent(final String intent, final String listNum, boolean testPlayback) {
        Response speechletResponse = handleRequest(getIntentInput(intent, listNum));
        PlayDirective playDirective = getPlayDirective(speechletResponse);
        assertEquals(PlayBehavior.REPLACE_ALL, playDirective.getPlayBehavior());
        String firstToken = playDirective.getAudioItem().getStream().getToken();
        assertTrue(firstToken.endsWith(",0"));
        if (testPlayback) {
            String startedInput = getResource("Started.js")
                .replace("{TOKEN}", firstToken);
            handleRequest(startedInput);

            String nearlyFinished = getResource("NearlyFinished.js")
                .replace("{TOKEN}", firstToken);
            speechletResponse = handleRequest(nearlyFinished);
            playDirective = getPlayDirective(speechletResponse);
            assertEquals(PlayBehavior.ENQUEUE, playDirective.getPlayBehavior());
            String secondToken = playDirective.getAudioItem().getStream().getToken();
            assertTrue(secondToken.endsWith(",1"));

            String nextIntent = getResource("Next.js")
                .replace("{TOKEN}", firstToken);

            speechletResponse = handleRequest(nextIntent);
            String thirdToken = playDirective.getAudioItem().getStream().getToken();
            assertEquals(thirdToken, secondToken);
        }
    }

    private PlayDirective getPlayDirective(final Response speechletResponse) {
        if (speechletResponse.getDirectives().isEmpty()) {
            return null;
        }
        return (PlayDirective) speechletResponse.getDirectives().get(0);
    }

    private String getIntentInput(final String intent, final String listNum) {
        String template = getResource("Intent.js");
        template = template.replace("{INTENT}", intent);
        template = template.replace("{LISTNUM}", listNum);
        return template;
    }

    private Response parseOutput(final String output) {
        try {
            ResponseEnvelope envelope = serializer.deserialize(output, ResponseEnvelope.class);
            return envelope.getResponse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getResource(String resourceName) {
        try {
            String template = IOUtils.toString(getClass().getResourceAsStream(
                "/requests/" + resourceName));
            template = template.replace("{DEVICEID}", deviceId);
            template = template.replace("{USERID}", userId);
            return template;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
