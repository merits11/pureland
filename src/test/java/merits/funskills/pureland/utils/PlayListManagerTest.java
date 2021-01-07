package merits.funskills.pureland.utils;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class PlayListManagerTest {

    @Mock
    private S3ObjectSummary s3ObjectSummary;


    @Test
    public void testGetDurationByName() throws Exception {
        when(s3ObjectSummary.getKey()).thenReturn("AmitabhaChanting/南無阿彌陀佛 慧普法師-284s.mp4");
        assertEquals(284000L, PlayListManager.getDuration(s3ObjectSummary));
    }

    @Test
    public void testGetDurationBySize() throws Exception {
        when(s3ObjectSummary.getKey()).thenReturn("AmitabhaChanting/南無阿彌陀佛 慧普法師.mp3");
        when(s3ObjectSummary.getSize()).thenReturn((128 / 8) * 1024 * 30L);
        assertEquals(30000L, PlayListManager.getDuration(s3ObjectSummary));
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }
}