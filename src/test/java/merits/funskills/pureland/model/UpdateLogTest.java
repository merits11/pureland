package merits.funskills.pureland.model;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class UpdateLogTest {

    @Test
    public void testUpdateLog() {
        assertNotEquals("000.0000.0001", UpdateLog.getLatestUpdate().getVersion());
        assertNotEquals("dummy", UpdateLog.getLatestUpdate().getVersion());
    }
}
