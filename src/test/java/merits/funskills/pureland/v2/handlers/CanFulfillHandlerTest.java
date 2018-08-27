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
import com.amazon.ask.model.slu.entityresolution.Resolution;
import com.amazon.ask.model.slu.entityresolution.Resolutions;
import com.amazon.ask.model.slu.entityresolution.Status;
import com.amazon.ask.model.slu.entityresolution.StatusCode;

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
        assertEquals(CanFulfillIntentValues.NO, response.get().getCanFulfillIntent().getCanFulfill());

        Slot slotA = Slot.builder().withName("A").withValue("A")
            .withResolutions(Resolutions.builder().addResolutionsPerAuthorityItem(
                Resolution.builder().withStatus(Status.builder().withCode(StatusCode.ER_SUCCESS_MATCH).build()).build())
                .build()
            ).build();

        Slot slotNotMatched = Slot.builder().withName("A").withValue("A")
            .withResolutions(Resolutions.builder().addResolutionsPerAuthorityItem(
                Resolution.builder().withStatus(Status.builder().withCode(StatusCode.ER_SUCCESS_NO_MATCH).build())
                    .build())
                .build()
            ).build();

        input = createInput(Intent.builder()
            .withName("CustomNameIntent")
            .withSlots(ImmutableMap
                .of("A", slotA,
                    "B", Slot.builder().withName("B").withValue(null).build()
                ))
            .build());
        response = handler.handle(input);
        System.out.println(response.get());
        assertEquals(CanFulfillIntentValues.YES, response.get().getCanFulfillIntent().getCanFulfill());

        input = createInput(Intent.builder()
            .withName("CustomNameIntent")
            .withSlots(ImmutableMap
                .of("A", slotNotMatched,
                    "B", Slot.builder().withName("B").withValue(null).build()
                ))
            .build());
        response = handler.handle(input);
        System.out.println(response.get());
        assertEquals(CanFulfillIntentValues.NO, response.get().getCanFulfillIntent().getCanFulfill());


        input = createInput(Intent.builder()
            .withName("FastForward")
            .withSlots(ImmutableMap
                .of("MINUTES", Slot.builder().withName("MINUTES").withValue("5").build()
                ))
            .build());
        response = handler.handle(input);
        System.out.println(response.get());
        assertEquals(CanFulfillIntentValues.YES, response.get().getCanFulfillIntent().getCanFulfill());

        input = createInput(Intent.builder()
            .withName("FastForward")
            .withSlots(ImmutableMap
                .of("MINUTES", Slot.builder().withName("MINUTES").withValue("1001").build()
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
