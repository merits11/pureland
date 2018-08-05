package merits.funskills.pureland.model;

import org.junit.Test;

import java.util.List;

import static merits.funskills.pureland.model.Tag.Chanting;
import static merits.funskills.pureland.model.Tag.Chinese;
import static merits.funskills.pureland.model.Tag.Music;
import static merits.funskills.pureland.model.Tag.Sutra;
import static merits.funskills.pureland.model.Tag.TAG;
import static org.junit.Assert.*;

public class PlayListTest {

    @Test
    public void testTags() throws Exception {
        List<Tag> tags = TAG(Chanting, Sutra);
        assertFalse(PlayList.AmitabhaChanting.isTagged(tags));
        tags = TAG(Chanting, Chinese);
        assertTrue(PlayList.MorningService.isTagged(tags));
        tags = TAG(Music, Chinese);
        assertTrue(PlayList.BuddhistSongs.isTagged(tags));
    }

}
