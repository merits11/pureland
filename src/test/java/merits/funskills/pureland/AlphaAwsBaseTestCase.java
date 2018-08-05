package merits.funskills.pureland;

import com.amazon.speech.speechlet.Device;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.User;
import com.amazon.speech.speechlet.interfaces.audioplayer.Stream;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective;
import com.amazon.speech.speechlet.interfaces.system.SystemState;
import com.amazonaws.services.lambda.runtime.Context;
import merits.funskills.pureland.model.PlayState;
import merits.funskills.pureland.utils.AppConfig;

import org.junit.BeforeClass;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AlphaAwsBaseTestCase {

    protected static final String TEST_TOKEN = "TEST-UUID-ALPHA-001";
    private static final String MOCK_STREAM_TOKEN = "TEST-UUID-ALPHA-001,OneHundredFour,0";
    protected static final String TEST_USER_ID = "TestUserId";
    protected static final String UNKOWN_USER_ID = "UnknownUserId";
    protected static final String TEST_DEVICE_ID = "TestDeviceId";
    protected static AudioPlayHelper audioPlayHelper;
    protected SystemState systemState;
    protected SystemState unkownUserSystemState;
    protected User user;
    protected Device device;

    @BeforeClass
    public static void initialize() {
        Context context = mock(Context.class);
        when(context.getFunctionName()).thenReturn("PureLandDynamoAlpha");
        AppConfig.init(context);
        audioPlayHelper = AudioPlayHelper.getInstance();
    }

    protected void initSystemState() {
        user = User.builder().withUserId(TEST_USER_ID).build();
        device = Device.builder()
                .withDeviceId(TEST_DEVICE_ID)
                .build();
        systemState = SystemState.builder()
                .withDevice(device)
                .withUser(user)
                .build();
        unkownUserSystemState = SystemState.builder()
                .withDevice(Device.builder().withDeviceId("Unknown").build())
                .withUser(User.builder().withUserId(UNKOWN_USER_ID).build())
                .build();
    }

    protected PlayState getPlayStateFromDynamo() {
        return audioPlayHelper.getPlayStateByStreamToken(MOCK_STREAM_TOKEN);
    }

    protected PlayState.PlayStateBuilder defaultPlayState() {
        return PlayState.builder()
                .token(TEST_TOKEN)
                .userid(TEST_USER_ID)
                .deviceId(TEST_DEVICE_ID);
    }


    protected Stream getStreamFromResponse(final SpeechletResponse speechletResponse) {
        PlayDirective directive = (PlayDirective) speechletResponse.getDirectives().get(0);
        Stream stream = directive.getAudioItem().getStream();
        return stream;
    }

    protected PlayState getPlayStateFromResponse(final SpeechletResponse speechletResponse) {
        Stream stream = getStreamFromResponse(speechletResponse);
        return audioPlayHelper.getPlayStateByStreamToken(stream.getToken());
    }
}
