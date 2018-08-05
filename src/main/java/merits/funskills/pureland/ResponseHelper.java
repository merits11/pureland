package merits.funskills.pureland;

import com.amazon.speech.speechlet.Directive;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.dialog.directives.DelegateDirective;
import com.amazon.speech.speechlet.dialog.directives.DialogDirective;
import com.amazon.speech.speechlet.dialog.directives.DialogIntent;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioItem;
import com.amazon.speech.speechlet.interfaces.audioplayer.PlayBehavior;
import com.amazon.speech.speechlet.interfaces.audioplayer.Stream;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.StopDirective;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ResponseHelper {

    public SpeechletResponse newPlayResponse(
            final String speechText,
            final Stream stream,
            final PlayBehavior playBehavior) {
        return newPlayResponse(
                speechText,
                stream,
                playBehavior,
                null,
                null
        );
    }

    public SpeechletResponse newDialogDelegateResponse(final DialogIntent dialogIntent) {
        SpeechletResponse response = new SpeechletResponse();
        DialogDirective dialogDirective = new DelegateDirective();
        dialogDirective.setUpdatedIntent(dialogIntent);
        List<Directive> directives = new ArrayList<>();
        directives.add(dialogDirective);
        response.setNullableShouldEndSession(false);
        response.setDirectives(directives);
        return response;
    }

    public SpeechletResponse newPlayResponse(
            final String speechText,
            final Stream stream,
            final PlayBehavior playBehavior,
            final String cardTitle,
            final String cardText) {
        SpeechletResponse response = new SpeechletResponse();
        if (StringUtils.isNotBlank(speechText)) {
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            speech.setText(speechText);
            response.setOutputSpeech(speech);
        }
        AudioItem audioItem = new AudioItem();
        audioItem.setStream(stream);
        PlayDirective playDirective = new PlayDirective();
        playDirective.setAudioItem(audioItem);
        playDirective.setPlayBehavior(playBehavior);


        List<Directive> directives = new ArrayList<>();
        directives.add(playDirective);
        response.setDirectives(directives);
        response.setNullableShouldEndSession(true);

        response.setCard(newCard(cardTitle, cardText));
        return response;
    }

    public SpeechletResponse newStopResponse(final String speechText) {
        SpeechletResponse response = new SpeechletResponse();
        if (StringUtils.isNotBlank(speechText)) {
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            speech.setText(speechText);
            response.setOutputSpeech(speech);
        }
        StopDirective stopDirective = new StopDirective();
        List<Directive> directives = new ArrayList<>();
        directives.add(stopDirective);
        response.setDirectives(directives);
        response.setNullableShouldEndSession(true);
        return response;
    }

    /*
    public SpeechletResponse clearQueueResponse(final String speechText) {
        SpeechletResponse response = new SpeechletResponse();
        if (StringUtils.isNotBlank(speechText)) {
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            speech.setText(speechText);
            response.setOutputSpeech(speech);
        }
        ClearQueueDirective clearQueueDirective = new ClearQueueDirective();
        clearQueueDirective.setClearBehavior(ClearBehavior.CLEAR_ENQUEUED);
        List<Directive> directives = new ArrayList<>();
        directives.add(clearQueueDirective);
        response.setDirectives(directives);
        response.setNullableShouldEndSession(true);
        return response;
    }*/


    public SpeechletResponse newTellResponse(final String txt) {
        return newTellResponse(txt, null, null);
    }

    public SpeechletResponse newTellResponse(final String txt, final String cardTitle, final String cardText) {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(txt);
        SpeechletResponse response = SpeechletResponse.newTellResponse(outputSpeech);
        response.setCard(newCard(cardTitle, cardText));
        return response;
    }

    public SpeechletResponse newAskResponse(
            final String preText,
            final String hint) {
        return newAskResponse(preText, hint,
                null, null);
    }

    public SpeechletResponse newAskResponse(
            final String preText,
            final String hint,
            final String cardTitle,
            final String cardText) {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(preText);
        repromptSpeech.setText(hint);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptSpeech);
        SimpleCard card = newCard(cardTitle, cardText);
        if (card == null) {
            return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
        } else {

            return SpeechletResponse.newAskResponse(outputSpeech, reprompt, card);
        }
    }

    private SimpleCard newCard(final String cardTitle, final String cardText) {
        if (StringUtils.isBlank(cardText)) {
            return null;
        }
        SimpleCard card = new SimpleCard();
        card.setTitle(cardTitle);
        card.setContent(cardText);
        return card;
    }
}
