package merits.funskills.pureland;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.Test;

import merits.funskills.pureland.utils.AppConfig;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppConfigTest {

    private Context context;

    @Test
    public void testTableNames() {
        context = mock(Context.class);

        when(context.getFunctionName()).thenReturn("TestFunc-Alpha");
        AppConfig.init(context);
        assertEquals(AppConfig.getPlayTable(), "pureLandTable-Alpha");

        when(context.getFunctionName()).thenReturn("TestFunc-Beta");
        AppConfig.init(context);
        assertEquals(AppConfig.getPlayTable(), "pureLandTable-Beta");

        when(context.getFunctionName()).thenReturn("TestFunc-Prod");
        AppConfig.init(context);
        assertEquals(AppConfig.getPlayTable(), "pureLandTable-Prod");
    }

}
