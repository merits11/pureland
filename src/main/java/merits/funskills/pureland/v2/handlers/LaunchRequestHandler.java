package merits.funskills.pureland.v2.handlers;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.system.SystemState;
import com.amazon.ask.request.Predicates;

import merits.funskills.pureland.model.PlayList;
import merits.funskills.pureland.model.UpdateLog;
import merits.funskills.pureland.model.UserSetting;

import static merits.funskills.pureland.model.UpdateLog.getLatestUpdate;
import static merits.funskills.pureland.model.UpdateLog.shouldPlayLatest;

public class LaunchRequestHandler extends BaseRequestHandler {

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.requestType(LaunchRequest.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        SystemState systemState = input.getRequestEnvelope().getContext().getSystem();
        List<PlayList> recentPlayed = this.playHelper.getRecentPlayed(systemState, 180);
        StringBuffer sb = new StringBuffer();
        sb.append(text("app.welcome"));
        UserSetting setting = toolbox.getUserSettings(systemState);
        if (setting == null || StringUtils.isEmpty(setting.getLanguage())) {
            sb.append(text("lang.prompt"));
            return askResponse(sb.toString(), text("lang.reprompt"));
        } else if (shouldPlayLatest(setting)) {
            UpdateLog.Update latestUpdate = getLatestUpdate();
            sb.append(latestUpdate.getUpdate());
            playHelper.incrementHeardCount(setting, latestUpdate);

        } else if (recentPlayed == null || recentPlayed.size() < 3) {
            sb.append(text("play.introduction") + " ");
        }
        sb.append(text("play.prompt"));
        return askResponse(sb.toString(), text("play.reprompt"));
    }
}
