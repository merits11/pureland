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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
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
import merits.funskills.pureland.model.UpdateLog;
import merits.funskills.pureland.model.UserSetting;
import merits.funskills.pureland.utils.AppConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@Log4j2
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LambdaHanlderV2Test {

    private LambdaHandlerV2 lambdaHandler;
    private AudioPlayHelperV2 audioPlayHelper;

    private static String userId;
    private static String fullUserId;
    private static String deviceId;
    private static String fullDeviceId;

    private Serializer serializer = new JacksonSerializer();
    private Speeches speeches = Speeches.getSpeeches();

    @Mock
    private Context context;

    @BeforeClass
    public static void init() {
        userId = String.format("TestUser%d", System.currentTimeMillis());
        deviceId = String.format("TestDevice%d", System.currentTimeMillis());
        fullUserId = "amzn1.ask.account." + userId;
        fullDeviceId = "amzn1.ask.device." + deviceId;
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getFunctionName()).thenReturn("pureland-Alpha");
        AppConfig.init(context);
        lambdaHandler = new LambdaHandlerV2();
        audioPlayHelper = AudioPlayHelperV2.getInstance();
    }

    @Test
    public void test001LaunchRequest() throws Exception {
        //A new user launches skill.
        Response speechletResponse = handleRequest(getResource("Launch.js"));
        assertNotNull(speechletResponse.getOutputSpeech());
        assertTrue(speechletResponse.getOutputSpeech().toString().contains(speeches.get("lang.prompt")));

        //User set language
        String langInput = getIntentInput("LangIntent","Lang","English");
        handleRequest(langInput);
        UserSetting userSetting = audioPlayHelper.getUserSettings(fullUserId);
        assertEquals(userSetting.getLanguage(), "English");

        speechletResponse = handleRequest(getResource("Launch.js"));
        //User has lang set, should hear latest update
        UpdateLog.Update latestUpdate = UpdateLog.getLatestUpdate();
        assertTrue(speechletResponse.toString().contains(latestUpdate.getUpdate()));

        //User has heard latest update,
        userSetting.setLastHeardVersion(latestUpdate.getVersion());
        userSetting.setHeardTimes(100);
        audioPlayHelper.saveUserSettings(userSetting);
        speechletResponse = handleRequest(getResource("Launch.js"));
        assertTrue(speechletResponse.toString().contains(speeches.get("play.introduction")));
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
    public void testNoSlotPlayIntents() throws Exception {
        //{"RandomList", "DharmaTalk", "SutraIntent", "MusicIntent", "ChantIntent"};
        String[] tagIntents = { "RandomList", "DharmaTalk", "SutraIntent", "MusicIntent", "ChantIntent",
            "MorningService", "EveningService" };
        Arrays.stream(tagIntents).forEach(intent -> {
            String input = getIntentInput(intent);
            Response response = handleRequest(input);
            PlayDirective playDirective = getPlayDirective(response);
            assertEquals(PlayBehavior.REPLACE_ALL, playDirective.getPlayBehavior());
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
    public void testPublicListIntents() throws Exception {
        PlayListUtils.getPublicNonVirtualLists()
            .stream().filter(pl -> pl.getListNumber() > 0)
            .forEach(pl ->
                {
                    log.info("Testing {}", pl);
                    try {
                        testPlayListIntent(String.valueOf(pl.getListNumber()), false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("Test failed for list " + pl);
                    }
                }
            );
    }

    @Test
    public void testVirtualListIntent() throws Exception {
        testPlayListIntent(String.valueOf(PlayList.AllMusic.getListNumber()), true);
    }

    @Test
    public void testPrivelListIntent() throws Exception {
        testPlayListIntent(String.valueOf(PlayList.PersonalList.getListNumber()), false);
    }

    @Test
    public void testCustomNameIntent() throws Exception {
        Response speechletResponse = handleRequest(getIntentInput("CustomNameIntent",
            "EmptyCloud", "Empty Cloud"));
        PlayDirective playDirective = getPlayDirective(speechletResponse);
        assertEquals(PlayBehavior.REPLACE_ALL, playDirective.getPlayBehavior());
        String firstToken = playDirective.getAudioItem().getStream().getToken();
        PlayState playState = audioPlayHelper.getPlayStateByStreamToken(firstToken);
        assertEquals(playState.currentPlayList(), PlayList.Twelve);
    }

    private void testPlayListIntent(final String listNum, boolean testPlayback) {
        Response speechletResponse = handleRequest(getIntentInput("PlayList", "LIST_NUMBER", listNum));
        PlayDirective playDirective = getPlayDirective(speechletResponse);
        assertEquals(PlayBehavior.REPLACE_ALL, playDirective.getPlayBehavior());
        String token1 = playDirective.getAudioItem().getStream().getToken();
        assertTrue(token1.endsWith(",0"));
        if (testPlayback) {
            String startedInput = getResource("Started.js")
                .replace("{TOKEN}", token1);
            handleRequest(startedInput);

            String nearlyFinished = getResource("NearlyFinished.js")
                .replace("{TOKEN}", token1);
            speechletResponse = handleRequest(nearlyFinished);
            playDirective = getPlayDirective(speechletResponse);
            assertEquals(PlayBehavior.ENQUEUE, playDirective.getPlayBehavior());
            String token2 = playDirective.getAudioItem().getStream().getToken();
            assertTrue(token2.endsWith(",1"));

            String nextIntent = getIntentInput("AMAZON.NextIntent")
                .replace("{TOKEN}", token1);

            speechletResponse = handleRequest(nextIntent);
            playDirective = getPlayDirective(speechletResponse);
            String token3 = playDirective.getAudioItem().getStream().getToken();
            assertNotEquals(token2, token3);

            String suffleOffInput = getIntentInput("AMAZON.ShuffleOffIntent")
                .replace("{TOKEN}", token3);
            speechletResponse = handleRequest(suffleOffInput);
            playDirective = getPlayDirective(speechletResponse);
            String token4 = playDirective.getAudioItem().getStream().getToken();
            assertEquals(PlayBehavior.REPLACE_ALL, playDirective.getPlayBehavior());
            assertNotEquals(token3, token4);

            String fastforwardInput = getIntentInput("FastForward", "Minutes", "5")
                .replace("{TOKEN}", token4);
            speechletResponse = handleRequest(fastforwardInput);
            playDirective = getPlayDirective(speechletResponse);
            String token5 = playDirective.getAudioItem().getStream().getToken();
            assertEquals(PlayBehavior.REPLACE_ALL, playDirective.getPlayBehavior());
            assertNotEquals(token4, token5);

            String rewindInput = getIntentInput("Rewind", "Minutes", "5")
                .replace("{TOKEN}", token5);
            speechletResponse = handleRequest(rewindInput);
            playDirective = getPlayDirective(speechletResponse);
            assertEquals(PlayBehavior.REPLACE_ALL, playDirective.getPlayBehavior());
        }
    }

    private PlayDirective getPlayDirective(final Response speechletResponse) {
        if (speechletResponse.getDirectives().isEmpty()) {
            return null;
        }
        return (PlayDirective) speechletResponse.getDirectives().get(0);
    }

    private String getIntentInput(final String intent, final String slotName, final String slotValue) {
        String template = getResource("IntentOneSlot.js");
        template = template.replace("{INTENT}", intent);
        template = template.replace("{SLOT_NAME}", slotName);
        template = template.replace("{SLOT_VALUE}", slotValue);
        return template;
    }

    private String getIntentInput(final String intent) {
        String template = getResource("IntentNoSlot.js");
        template = template.replace("{INTENT}", intent);
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
                "/requests/" + resourceName), StandardCharsets.UTF_8);
            template = template.replace("{DEVICEID}", deviceId);
            template = template.replace("{USERID}", userId);
            return template;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
