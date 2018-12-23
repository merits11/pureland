package merits.funskills.pureland.v2.handlers;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.request.Predicates;

import merits.funskills.pureland.model.Tag;

public class LanguageRequestHandler extends BaseRequestHandler {

    private static final String SLOT_LANG = "Lang";

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.intentName("LangIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        Intent intent = getIntent(input);
        if (!intent.getSlots().containsKey(SLOT_LANG)) {
            return delegate(input);
        }
        Slot slot = intent.getSlots().get(SLOT_LANG);
        if (StringUtils.isEmpty(slot.getValue())) {
            return delegate(input);
        }
        String lang = slot.getValue().toLowerCase();
        Tag langTag = Tag.AllLanguages;
        switch (lang) {
            case "english":
                langTag = Tag.English;
                break;
            case "chinese":
                langTag = Tag.Chinese;
                break;
            default:
                break;
        }
        toolbox.setLanguageTag(systemState(input), langTag);
        return askResponse(text("lang.set", slot.getValue()), text("play.reprompt"));
    }

    private Optional<Response> delegate(HandlerInput input) {
        return input.getResponseBuilder()
            .addDelegateDirective(Intent.builder()
                .withName("LangIntent")
                .withSlots(ImmutableMap.of("Lang", Slot.builder().withName("Lang").build()))
                .build())
            .build();
    }
}
