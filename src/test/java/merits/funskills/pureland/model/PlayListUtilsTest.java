package merits.funskills.pureland.model;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static merits.funskills.pureland.model.PlayListUtils.getPlaylist;
import static merits.funskills.pureland.model.Tag.TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Log4j2
public class PlayListUtilsTest {
    @Test
    public void testSplitByCamelCase() throws Exception {

        String testStr = "TwentyOne";
        String[] output = PlayListUtils.splitByCamelCase(testStr);
        assertEquals(2, output.length);
        assertEquals("Twenty", output[0]);
        assertEquals("One", output[1]);
    }

    @Test
    public void testGetPlayList() throws Exception {
        assertEquals(PlayList.Twenty, getPlaylist("20"));
        assertEquals(PlayList.Five, getPlaylist("5"));
        assertEquals(PlayList.Five, getPlaylist("five"));
        assertEquals(PlayList.Two, getPlaylist("two"));
        assertEquals(PlayList.OneHundredThree, getPlaylist("103"));
        assertEquals(PlayList.MorningService, getPlaylist("MorningService"));
        assertNull(getPlaylist("1234"));
    }

    @Test
    public void testGetListByTags() throws Exception {
        PlayList playList = PlayListUtils.getListByTags(Lists.newArrayList(), Lists.newArrayList());
        assertNotNull(playList);
        assertTrue(playList.isAccessible(Collections.emptyList()));

        List<Tag> tags = TAG(Tag.Sutra, Tag.Chinese);
        PlayList selectPlayList1 = PlayListUtils.getListByTags(tags, Lists.newArrayList());
        assertTrue(selectPlayList1.isTagged(tags));

        List<PlayList> recentPlayed = Lists.newArrayList(selectPlayList1);
        PlayList selectPlayList2 = PlayListUtils.getListByTags(tags, recentPlayed);
        assertTrue(selectPlayList2.isTagged(tags));
        recentPlayed.add(selectPlayList2);

        log.info("Recently played list - {} will be excluded",
                Joiner.on(",").join(
                        recentPlayed.stream().map(v -> String.format(" [%d - %s] ", v.getListNumber(), v))
                                .collect(Collectors.toList())));
        IntStream.range(1, 100).forEach(
                v -> {
                    PlayList myList = PlayListUtils.getListByTags(tags, recentPlayed);
                    log.info("Selected playlist #{}: {} - {}", v, myList.getListNumber(), myList);
                    assertTrue(myList != selectPlayList1 && myList != selectPlayList2);
                });
    }

}
