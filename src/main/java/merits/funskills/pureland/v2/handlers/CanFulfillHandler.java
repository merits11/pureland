package merits.funskills.pureland.v2.handlers;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.amazonaws.util.CollectionUtils;

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
import com.amazon.ask.model.slu.entityresolution.Resolution;
import com.amazon.ask.model.slu.entityresolution.StatusCode;
import com.amazon.ask.request.Predicates;

import merits.funskills.pureland.model.PlayListUtils;

import static merits.funskills.pureland.model.Constants.SLOT_NAME_LIST_NUMBER;
import static merits.funskills.pureland.model.Constants.SLOT_NAME_MINUTES;
import static merits.funskills.pureland.model.Constants.SLOT_NAME_SEQUENCE;

public class CanFulfillHandler extends BaseRequestHandler {

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.requestType(CanFulfillIntentRequest.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        CanFulfillIntentRequest request = (CanFulfillIntentRequest) input.getRequestEnvelope().getRequest();
        Intent intent = request.getIntent();
        if (intent.getName().equals("PlayList")) {
            return canfulfillSingleNumberIntent(input, intent, this::canfulfillPlayList);
        } else if (intent.getName().equals("CustomNameIntent")) {
            return canfulfillName(input, intent);
        } else if (intent.getName().equals("FastForward") || intent.getName().equals("Rewind")) {
            return canfulfillSingleNumberIntent(input, intent, this::canfulfillForward);
        } else if (intent.getName().equals("GotoItem")) {
            return canfulfillSingleNumberIntent(input, intent, this::canfulfillGotoItem);
        } else if (intent.getName().equals("WhatsThis")) {
            return canfulfillNoSlotIntent(input, intent);
        }
        return sayNo(input, intent);

    }

    private boolean canfulfillPlayList(Slot slot) {
        return slot.getName().equals(SLOT_NAME_LIST_NUMBER) && PlayListUtils.getPlaylist(slot.getValue()) != null;
    }

    private boolean canfulfillForward(Slot slot) {
        if (!slot.getName().equals(SLOT_NAME_MINUTES)) {
            return false;
        }
        int minutes = Integer.parseInt(slot.getValue());
        return minutes > 0 && minutes <= 100;

    }

    private boolean canfulfillGotoItem(Slot slot) {
        if (!slot.getName().equals(SLOT_NAME_SEQUENCE)) {
            return false;
        }
        int sequence = Integer.parseInt(slot.getValue());
        return sequence >= 0 && sequence < 9999;

    }

    private Optional<Response> sayNo(HandlerInput input, Intent intent) {
        return input.getResponseBuilder().withCanFulfillIntent(
            CanFulfillIntent.builder().withCanFulfill(CanFulfillIntentValues.NO)
                .build()
        ).build();
    }

    private Optional<Response> sayYes(HandlerInput input, Intent intent) {
        return input.getResponseBuilder().withCanFulfillIntent(
            CanFulfillIntent.builder().withCanFulfill(CanFulfillIntentValues.YES)
                .build()
        ).build();
    }

    private Optional<Response> canfulfillNoSlotIntent(HandlerInput input, Intent intent) {
        if (intent.getSlots() == null || intent.getSlots().size() == 0) {
            return sayYes(input, intent);
        }
        return sayNo(input, intent);
    }

    private Optional<Response> canfulfillSingleNumberIntent(HandlerInput input, Intent intent,
        Predicate<Slot> slotPredicate) {
        Map<String, Slot> slots = intent.getSlots();
        if (slots == null || slots.isEmpty()) {
            return sayNo(input, intent);
        }
        CanFulfillIntent.Builder builder = CanFulfillIntent.builder();
        MutableBoolean canFulfillIntent = new MutableBoolean(false);
        MutableBoolean canNotFulfillIntent = new MutableBoolean(false);
        slots.forEach((name, slot) -> {
            String value = slot.getValue();
            if (StringUtils.isEmpty(value)) {
                return;
            }
            if (StringUtils.isNumeric(slot.getValue()) && slotPredicate.test(slot)) {
                builder.putSlotsItem(name,
                    CanFulfillSlot.builder()
                        .withCanFulfill(CanFulfillSlotValues.YES)
                        .withCanUnderstand(CanUnderstandSlotValues.YES)
                        .build()
                );
                canFulfillIntent.setTrue();
                return;
            }
            builder.putSlotsItem(name,
                CanFulfillSlot.builder()
                    .withCanFulfill(CanFulfillSlotValues.NO)
                    .withCanUnderstand(CanUnderstandSlotValues.NO)
                    .build()
            );
            canNotFulfillIntent.setTrue();
        });
        return getResponse(input, builder, canFulfillIntent, canNotFulfillIntent);
    }

    private Optional<Response> canfulfillName(HandlerInput input, Intent intent) {
        Map<String, Slot> slots = intent.getSlots();
        CanFulfillIntent.Builder builder = CanFulfillIntent.builder();
        MutableBoolean canFulfillIntent = new MutableBoolean(false);
        MutableBoolean canNotFulfillIntent = new MutableBoolean(false);
        for (Slot slot : slots.values()) {
            String value = slot.getValue();
            if (StringUtils.isEmpty(value)) {
                continue;
            }
            CanFulfillSlot.Builder canFulfillSlotBuilder = CanFulfillSlot.builder();
            if (slot.getResolutions() == null || CollectionUtils.isNullOrEmpty(
                slot.getResolutions().getResolutionsPerAuthority())) {
                canNotFulfillIntent.setTrue();
                canFulfillSlotBuilder.withCanFulfill(CanFulfillSlotValues.NO)
                    .withCanUnderstand(CanUnderstandSlotValues.NO)
                    .build();
                builder.putSlotsItem(slot.getName(), canFulfillSlotBuilder.build());
                continue;
            }

            Optional<Resolution> resolutionOptional = slot.getResolutions().getResolutionsPerAuthority().stream()
                .filter(resolution -> resolution.getStatus().getCode() == StatusCode.ER_SUCCESS_MATCH)
                .findAny();
            if (resolutionOptional.isPresent()) {
                canFulfillSlotBuilder.withCanFulfill(CanFulfillSlotValues.YES)
                    .withCanUnderstand(CanUnderstandSlotValues.YES)
                    .build();
                canFulfillIntent.setTrue();
            } else {
                canFulfillSlotBuilder.withCanFulfill(CanFulfillSlotValues.NO)
                    .withCanUnderstand(CanUnderstandSlotValues.NO)
                    .build();
                canNotFulfillIntent.setTrue();
            }
            builder.putSlotsItem(slot.getName(), canFulfillSlotBuilder.build());
        }
        return getResponse(input, builder, canFulfillIntent, canNotFulfillIntent);
    }

    private Optional<Response> getResponse(HandlerInput input, CanFulfillIntent.Builder builder,
        MutableBoolean canFulfillIntent, MutableBoolean canNotFulfillIntent) {
        if (canFulfillIntent.isFalse() || canNotFulfillIntent.isTrue()) {
            builder.withCanFulfill(CanFulfillIntentValues.NO);
        } else {
            builder.withCanFulfill(CanFulfillIntentValues.YES);
        }
        return input.getResponseBuilder().withCanFulfillIntent(builder.build()).build();
    }

}
