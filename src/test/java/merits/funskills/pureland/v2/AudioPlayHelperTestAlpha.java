package merits.funskills.pureland.v2;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.amazon.ask.model.User;
import com.amazon.ask.model.interfaces.system.SystemState;

import lombok.extern.log4j.Log4j2;
import merits.funskills.pureland.model.PlayItem;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayListUtils;
import merits.funskills.pureland.model.PlayState;
import merits.funskills.pureland.model.Token;
import merits.funskills.pureland.model.UserSetting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Log4j2
public class AudioPlayHelperTestAlpha extends AlphaAwsBaseTestCase {

    private static final String ANOTHER_TEST_TOKEN = "TEST-UUID-ALPHA-002";

    @Before
    public void setup() {
        initSystemState();
    }

    @Test
    public void testBasicFlow() throws Exception {
        PlayItem playItem = audioPlayHelper.getPlayItem(PlayList.OneHundredFour, 0);
        PlayState playState = defaultPlayState()
            .currentList(playItem.getListName().toString())
            .currentSeq(playItem.getSeqNo())
            .build();
        Date currentDate = new Date();
        audioPlayHelper.savePlayState(playState);
        PlayState savedPlayState = getPlayStateFromDynamo();
        assertNotNull(savedPlayState);
        assertTrue(savedPlayState.getLastModified().compareTo(currentDate) >= 0);
        assertTrue(!savedPlayState.isRepeat() && !savedPlayState.isPaused());
        log.info(savedPlayState.toString());
    }

    @Test
    public void testGetPlayStateBySystemState() throws Exception {
        PlayState playState1 = defaultPlayState()
            .currentList(PlayList.OneHundredThree.toString())
            .currentSeq(4)
            .build();
        PlayState playState2 = defaultPlayState()
            .token(ANOTHER_TEST_TOKEN)
            .currentList(PlayList.Two.toString())
            .currentSeq(7)
            .build();
        audioPlayHelper.savePlayState(playState1);
        Thread.sleep(100);
        audioPlayHelper.savePlayState(playState2);
        PlayState dbPlayState = audioPlayHelper.getPlayStateBySystemState(systemState);
        assertEquals(ANOTHER_TEST_TOKEN, dbPlayState.getToken());
        assertEquals(playState2.getCurrentList(), dbPlayState.getCurrentList());
        assertEquals(playState2.getCurrentSeq(), dbPlayState.getCurrentSeq());

        dbPlayState = audioPlayHelper.getPlayStateBySystemState(systemState, PlayList.OneHundredThree);
        assertEquals(TEST_TOKEN, dbPlayState.getToken());
        assertEquals(playState1.getCurrentList(), dbPlayState.getCurrentList());
        assertEquals(playState1.getCurrentSeq(), dbPlayState.getCurrentSeq());
    }

    @Test
    public void testGetNextPlayItem() throws Exception {
        AudioPlayHelperV2 playHelper = AudioPlayHelperV2.getInstance();
        PlayList playList = PlayList.OneHundredFour;
        PlayItem playItem = playHelper.getPlayItem(playList, 0);
        Token token = Token.builder()
            .listName(playItem.getListName().toString())
            .listSequence(playItem.getSeqNo())
            .uuid(TEST_TOKEN)
            .build();
        PlayState playState = PlayState.builder()
            .deviceId(TEST_DEVICE_ID)
            .userid(TEST_USER_ID)
            .currentList(playItem.getListName().toString())
            .currentSeq(playItem.getSeqNo())
            .token(token.getUuid())
            .build();
        PlayItem nextPlayItem = playHelper.getNextPlayItem(playState);
        assertEquals(playItem.getSeqNo() + 1, nextPlayItem.getSeqNo());

        playState.setPaused(true);
        nextPlayItem = playHelper.getNextPlayItem(playState);
        assertEquals(playItem.getSeqNo(), nextPlayItem.getSeqNo());
        playState.setPaused(false);

        playState.setRepeatSeq(0);
        nextPlayItem = playHelper.getNextPlayItem(playState);
        assertEquals(playItem.getSeqNo(), nextPlayItem.getSeqNo());
        playState.setRepeatSeq(null);

        playState.setShuffle(true);
        boolean notNext = false;
        nextPlayItem = playHelper.getNextPlayItem(playState);
        notNext = notNext || (nextPlayItem.getSeqNo() != (playItem.getSeqNo() + 1));
        nextPlayItem = playHelper.getNextPlayItem(playState);
        notNext = notNext || (nextPlayItem.getSeqNo() != (playItem.getSeqNo() + 1));
        assertTrue(notNext);
    }

    @Test
    public void testGetPlayItem() throws Exception {
        PlayListUtils.getPublicNonVirtualLists().forEach(playList -> {
            log.info("Getting play item for {}", playList);
            PlayItem playItem = audioPlayHelper.getPlayItem(playList, 0);
            assertNotNull(playItem);
            assertTrue(playItem.getApproximateDuration() > 0);
        });
    }

    @Test
    public void testLangSetting() throws Exception {
        UserSetting language = UserSetting.builder()
            .userId("User:" + System.nanoTime())
            .language("English")
            .build();
        audioPlayHelper.saveUserSettings(language);
        Thread.sleep(100);
        UserSetting dbCopy = audioPlayHelper.getUserSettings(language.getUserId());
        assertEquals(language, dbCopy);
        dbCopy = audioPlayHelper.getUserSettings(language.getUserId() + "NotFound");
        assertNull(dbCopy);
    }

    @Test
    public void testRecentlyPlayed() throws Exception {
        String userId = "RecentlyPlayedTestUser:" + System.currentTimeMillis();
        PlayState playState1 = defaultPlayState()
            .currentList(PlayList.HuayanSutra.toString())
            .userid(userId)
            .currentSeq(4)
            .build();
        PlayState playState2 = defaultPlayState()
            .token(ANOTHER_TEST_TOKEN)
            .currentList(PlayList.DizangSutra.toString())
            .userid(userId)
            .currentSeq(7)
            .build();
        audioPlayHelper.savePlayState(playState1);
        Thread.sleep(100);
        audioPlayHelper.savePlayState(playState2);
        Thread.sleep(100);

        SystemState systemState = SystemState.builder()
            .withUser(User.builder().withUserId(userId).build())
            .build();

        List<PlayList> recentPlayed = audioPlayHelper.getRecentPlayed(systemState, 1);
        assertEquals(2, recentPlayed.size());
        assertEquals(PlayList.DizangSutra, recentPlayed.get(0));
        assertEquals(PlayList.HuayanSutra, recentPlayed.get(1));
    }

}
