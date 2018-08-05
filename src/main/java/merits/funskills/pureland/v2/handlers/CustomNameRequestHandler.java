package merits.funskills.pureland.v2.handlers;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.request.Predicates;

import merits.funskills.pureland.model.NameMapping;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.Tag;

public class CustomNameRequestHandler extends BaseRequestHandler {
    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.intentName("CustomNameIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        Intent intent = getIntent(input);
        Collection<Slot> slots = intent.getSlots().values();
        Optional<Slot> firstSlot = slots.stream().filter(entry -> StringUtils.isNotBlank(entry.getValue())).findFirst();
        if (firstSlot.isPresent()) {
            return playOneOfTheseLists(input, NameMapping.getPlayLists(firstSlot.get().getName()));
        }
        return askResponse(text("name.noslot"), text("play.reprompt"));
    }

    private Optional<Response> playOneOfTheseLists(HandlerInput input, Set<PlayList> playLists) {
        Tag langTag = toolbox.getLanguageTag(systemState(input));
        List<PlayList> fullList = Lists.newArrayList(playLists);
        List<PlayList> candidateLists = fullList;
        if (langTag != null) {
            candidateLists = playLists.stream().filter(p -> p.isTagged(langTag)).collect(Collectors.toList());
            if (candidateLists.isEmpty()) {
                candidateLists = fullList;
            }
        }
        if (candidateLists.size() > 1) {
            List<PlayList> recentPlayLists = playHelper.getRecentPlayed(systemState(input), NUM_RECENT_DAYS);
            PlayList mostRecentlyPlayed = recentPlayLists.isEmpty() ? null : recentPlayLists.get(0);
            candidateLists = candidateLists.stream().filter(p -> !Objects.equals(mostRecentlyPlayed, p))
                .collect(Collectors.toList());
        }
        int selected = RandomUtils.nextInt(0, candidateLists.size());
        PlayList playList = candidateLists.get(selected);
        return playList(input, playList);
    }

}
