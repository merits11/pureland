package merits.funskills.pureland.v2.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.google.common.collect.ImmutableSet;
import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayListUtils;

import java.util.Optional;
import java.util.Set;

public class PlaylistRequestHandler extends BaseRequestHandler {

    private static final String SLOT_LIST_NAME = "LIST_NUMBER";
    private static final Set<String> INTENTS = ImmutableSet.of("PlayList", "EveningService", "MorningService", "AMAZON.FallbackIntent");

    @Override
    public boolean canHandle(HandlerInput input) {
        return intentNameInSet(input, INTENTS);
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        Intent intent = getIntent(input);
        if (intent.getName().equals("PlayList")) {
            Optional<Slot> slot = getSlot(input, SLOT_LIST_NAME);
            if (!slot.isPresent()) {
                return askResponse(text("playlist.noslot"), text("play.reprompt"));
            }
            String listUtterance = slot.get().getValue();
            PlayList myList = PlayListUtils.getPlaylist(listUtterance);
            if (myList != null) {
                return playList(input, myList);
            } else {
                return askResponse(text("playlist.notfound", listUtterance), text("play.reprompt"));
            }
        } else if ("EveningService".equals(intent.getName())) {
            return playList(input, PlayList.EveningService);
        } else if ("MorningService".equals(intent.getName())) {
            return playList(input, PlayList.MorningService);
        } else if ("AMAZON.FallbackIntent".equals(intent.getName())) {
            return askResponse(text("fallback.prompt"), text("play.reprompt"));
        }
        return askResponse(text("playlist.noslot"), text("play.reprompt"));
    }

}
