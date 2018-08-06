package merits.funskills.pureland.v2.handlers;

import java.util.Optional;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.canfulfill.CanFulfillIntentRequest;
import com.amazon.ask.model.canfulfill.CanFulfillIntentValues;

import static org.junit.Assert.assertEquals;

public class CanFulfillHandlerTest {

    @Test
    public void testCanFulfill() throws Exception {
        CanFulfillHandler handler = new CanFulfillHandler();
        HandlerInput input = createInput(Intent.builder()
            .withName("PlayList")
            .withSlots(ImmutableMap
                .of("LIST_NUMBER", Slot.builder().withName("LIST_NUMBER").withValue("1").build()))
            .build());
        Optional<Response> response = handler.handle(input);
        System.out.println(response.get());
        assertEquals(CanFulfillIntentValues.YES, response.get().getCanFulfillIntent().getCanFulfill());

        input = createInput(Intent.builder()
            .withName("PlayList")
            .withSlots(ImmutableMap
                .of("LIST_NUMBER", Slot.builder().withName("LIST_NUMBER").withValue("-1").build()))
            .build());
        response = handler.handle(input);
        System.out.println(response.get());
        assertEquals(CanFulfillIntentValues.NO, response.get().getCanFulfillIntent().getCanFulfill());

        input = createInput(Intent.builder()
            .withName("CustomNameIntent")
            .withSlots(ImmutableMap
                .of("A", Slot.builder().withName("A").withValue(null).build(),
                    "B", Slot.builder().withName("B").withValue("NonEmpty").build()
                ))
            .build());
        response = handler.handle(input);
        System.out.println(response.get());
        assertEquals(CanFulfillIntentValues.YES, response.get().getCanFulfillIntent().getCanFulfill());

        input = createInput(Intent.builder()
            .withName("CustomNameIntent")
            .withSlots(ImmutableMap
                .of("A", Slot.builder().withName("A").withValue(null).build(),
                    "B", Slot.builder().withName("B").withValue(null).build()
                ))
            .build());
        response = handler.handle(input);
        System.out.println(response.get());
        assertEquals(CanFulfillIntentValues.NO, response.get().getCanFulfillIntent().getCanFulfill());
    }

    private HandlerInput createInput(Intent intent) {
        return HandlerInput.builder()
            .withRequestEnvelope(RequestEnvelope.builder()
                .withRequest(CanFulfillIntentRequest.builder().withIntent(intent).build())
                .build())
            .build();
    }
}
