package merits.funskills.pureland.v2;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class SpeechesTest {

    @Test
    public void shouldGetValidText() {
        Speeches speeches = Speeches.getSpeeches();
        assertNotNull(speeches.get("app.name"));
        assertNotNull(speeches.get("app.name"));
    }

}
