package merits.funskills.pureland.v2.handlers;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.Response;

import merits.funskills.pureland.model.Tag;

public class GeneralCategoryRequestHandler extends BaseRequestHandler {

    private static final Set<String> INTENTS = ImmutableSet.of(
        "RandomList", "DharmaTalk", "SutraIntent", "MusicIntent", "ChantIntent");

    @Override
    public boolean canHandle(HandlerInput input) {
        return intentNameInSet(input, INTENTS);
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        Intent intent = getIntent(input);
        List<Tag> tags = Lists.newArrayList();
        switch (intent.getName()) {
            case "DharmaTalk":
                tags.add(Tag.DharmaTalk);
                break;
            case "SutraIntent":
                tags.add(Tag.Sutra);
                break;
            case "MusicIntent":
                tags.add(Tag.Music);
                break;
            case "ChantIntent":
                tags.add(Tag.Chanting);
                break;
            default:
        }
        return handleTagIntent(input, tags, "");
    }
}
