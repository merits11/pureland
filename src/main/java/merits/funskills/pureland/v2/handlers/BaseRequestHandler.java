package merits.funskills.pureland.v2.handlers;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Request;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.interfaces.audioplayer.AudioPlayerState;
import com.amazon.ask.model.interfaces.system.SystemState;
import com.amazon.ask.request.Predicates;

import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.PlayListUtils;
import merits.funskills.pureland.model.Tag;
import merits.funskills.pureland.v2.AudioPlayHelperV2;
import merits.funskills.pureland.v2.PureLandMusicHelperV2;
import merits.funskills.pureland.v2.ResponseHelperV2;
import merits.funskills.pureland.v2.Speeches;

public abstract class BaseRequestHandler implements RequestHandler {

    protected static final int NUM_RECENT_DAYS = 14;

    protected AudioPlayHelperV2 playHelper;
    protected PureLandMusicHelperV2 toolbox;
    protected Speeches speeches;
    protected ResponseHelperV2 responseHelper;

    public BaseRequestHandler() {
        this(AudioPlayHelperV2.getInstance());
    }

    BaseRequestHandler(AudioPlayHelperV2 audioPlayHelperV2) {
        this.responseHelper = new ResponseHelperV2();
        this.playHelper = audioPlayHelperV2;
        this.toolbox = new PureLandMusicHelperV2(playHelper, responseHelper);
        this.speeches = Speeches.getSpeeches();
    }

    @Override
    public abstract boolean canHandle(HandlerInput input);

    @Override
    public abstract Optional<Response> handle(HandlerInput input);

    protected String text(final String name, Object... paras) {
        if (paras == null || paras.length == 0) {
            return speeches.get(name);
        }
        return String.format(speeches.get(name), paras);
    }

    protected Optional<Response> askResponse(String speechText, String repromptText) {
        return responseHelper.askResponse(speechText, repromptText);

    }

    protected Intent getIntent(HandlerInput input) {
        Request request = input.getRequestEnvelope().getRequest();
        IntentRequest intentRequest = (IntentRequest) request;
        Intent intent = intentRequest.getIntent();
        return intent;
    }

    protected boolean intentNameInSet(HandlerInput input, Set<String> names) {
        if (input.getRequestEnvelope().getRequest() instanceof IntentRequest) {
            return names.contains(getIntent(input).getName());
        }
        return false;
    }

    protected boolean requestTypeInSet(HandlerInput input, Set<Class<? extends Request>> names) {
        for (Class<? extends Request> clazz : names) {
            if (input.matches(Predicates.requestType(clazz))) {
                return true;
            }
        }
        return false;
    }

    protected Optional<Slot> getSlot(HandlerInput input, String slotName) {
        Intent intent = getIntent(input);
        if (!intent.getSlots().containsKey(slotName)) {
            return Optional.empty();
        }
        return Optional.of(intent.getSlots().get(slotName));
    }

    protected SystemState systemState(HandlerInput handlerInput) {
        return handlerInput.getRequestEnvelope().getContext().getSystem();
    }

    protected AudioPlayerState audioPlayer(HandlerInput handlerInput) {
        return handlerInput.getRequestEnvelope().getContext().getAudioPlayer();
    }

    protected Optional<Response> handleTagIntent(HandlerInput input, List<Tag> tags, String preSpeech) {
        Tag langTag = toolbox.getLanguageTag(systemState(input));
        if (langTag != null) {
            tags.add(langTag);
        }
        List<PlayList> recentPlayLists = playHelper.getRecentPlayed(systemState(input), NUM_RECENT_DAYS);
        PlayList candidateList = PlayListUtils.getListByTags(tags, recentPlayLists);

        String text = preSpeech + text("tag.response", candidateList.getListNumber(), candidateList.getText());
        return toolbox.resumePlayList(text, systemState(input), candidateList);
    }

    protected Optional<Response> playList(HandlerInput input, PlayList myList) {
        final String listDescription = myList.getListNumber() > 0 ?
            (myList.getListNumber() + ": " + myList.getText()) : myList.getText();
        return toolbox.resumePlayList(text("playlist.response", listDescription), systemState(input), myList);
    }
}
