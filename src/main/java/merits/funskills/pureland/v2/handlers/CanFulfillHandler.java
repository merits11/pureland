package merits.funskills.pureland.v2.handlers;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.canfulfill.CanFulfillIntent;
import com.amazon.ask.model.canfulfill.CanFulfillIntentRequest;
import com.amazon.ask.model.canfulfill.CanFulfillIntentValues;
import com.amazon.ask.model.canfulfill.CanFulfillSlot;
import com.amazon.ask.model.canfulfill.CanFulfillSlotValues;
import com.amazon.ask.model.canfulfill.CanUnderstandSlotValues;
import com.amazon.ask.request.Predicates;

import merits.funskills.pureland.model.PlayListUtils;

public class CanFulfillHandler extends BaseRequestHandler {

    private static final String LIST_NUMBER_SLOT = "LIST_NUMBER";

    private static final Set<String> MAYBE_INTENTS = ImmutableSet
        .of("RandomList");

    private static final Set<String> YES_INTENTS = ImmutableSet
        .of("SutraIntent", "ChantIntent", "EveningService", "MorningService", "MusicIntent");

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.requestType(CanFulfillIntentRequest.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        CanFulfillIntentRequest request = (CanFulfillIntentRequest) input.getRequestEnvelope().getRequest();
        Intent intent = request.getIntent();
        if (intent.getName().equals("PlayList")) {
            return canfulfillPlayList(input, intent);
        } else if (intent.getName().equals("CustomNameIntent")) {
            return canfulfillName(input, intent);
        } else if (YES_INTENTS.contains(intent.getName())) {
            input.getResponseBuilder().withCanFulfillIntent(
                CanFulfillIntent.builder().withCanFulfill(CanFulfillIntentValues.YES).build()
            ).build();
        } else if (MAYBE_INTENTS.contains(intent.getName())) {
            input.getResponseBuilder().withCanFulfillIntent(
                CanFulfillIntent.builder().withCanFulfill(CanFulfillIntentValues.MAYBE).build()
            ).build();
        }
        return input.getResponseBuilder().withCanFulfillIntent(
            CanFulfillIntent.builder().withCanFulfill(CanFulfillIntentValues.NO).build()
        ).build();
    }

    private Optional<Response> canfulfillPlayList(HandlerInput input, Intent intent) {
        Map<String, Slot> slots = intent.getSlots();
        CanFulfillIntent.Builder builder = CanFulfillIntent.builder();
        if (slots.containsKey(LIST_NUMBER_SLOT)) {
            builder.withCanFulfill(CanFulfillIntentValues.NO);
            String number = slots.get(LIST_NUMBER_SLOT).getValue();
            Integer listNum = -1;
            try {
                listNum = Integer.valueOf(number);
            } catch (Exception e) {
                //ignore
            }
            if (listNum > 0) {
                CanFulfillSlot.Builder slotBuilder = CanFulfillSlot.builder();
                slotBuilder.withCanUnderstand(CanUnderstandSlotValues.YES);
                if (PlayListUtils.getPlaylist(number) != null) {
                    slotBuilder.withCanFulfill(CanFulfillSlotValues.YES);
                    builder.withCanFulfill(CanFulfillIntentValues.YES);
                } else {
                    slotBuilder.withCanFulfill(CanFulfillSlotValues.NO);
                }
                builder.putSlotsItem(LIST_NUMBER_SLOT, slotBuilder.build());
            }
        } else {
            builder.withCanFulfill(CanFulfillIntentValues.NO);
        }
        return input.getResponseBuilder().withCanFulfillIntent(builder.build()).build();
    }

    private Optional<Response> canfulfillName(HandlerInput input, Intent intent) {
        Map<String, Slot> slots = intent.getSlots();
        CanFulfillIntent.Builder builder = CanFulfillIntent.builder();
        builder.withCanFulfill(CanFulfillIntentValues.NO);
        for (Slot slot : slots.values()) {
            CanFulfillSlot canFulfillSlot = CanFulfillSlot.builder()
                .withCanFulfill(CanFulfillSlotValues.YES)
                .withCanUnderstand(CanUnderstandSlotValues.YES)
                .build();
            builder.putSlotsItem(slot.getName(), canFulfillSlot);
            if (slot.getValue() != null) {
                builder.withCanFulfill(CanFulfillIntentValues.YES);
            }
        }
        return input.getResponseBuilder().withCanFulfillIntent(builder.build()).build();
    }

}
