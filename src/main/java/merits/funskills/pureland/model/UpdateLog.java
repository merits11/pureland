package merits.funskills.pureland.model;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class UpdateLog {

    private static final int PLAY_TIMES = 1;

    private static Map<String, String> updates = new HashMap<String, String>() {
        {
            put("2018.0720.1142",
                " Latest update: I can now play by list name. Try saying: 'heart sutra'. Check pure land Alexa "
                    + "skills page for more. ");

            put("000.0000.0001", "dummy");
        }
    };

    private static Update latestUpdate = getLatest();

    private static Update getLatest() {
        String latestVersion = updates.keySet().stream().reduce((a, b) -> a.compareTo(b) >= 0 ? a : b).get();
        return new Update(latestVersion, updates.get(latestVersion));
    }

    public static Update getLatestUpdate() {
        return latestUpdate;
    }

    public static boolean shouldPlayLatest(final UserSetting userSetting) {
        if (userSetting == null) {
            return true;
        }
        if (userSetting.getLastHeardVersion() == null) {
            return true;
        }
        if (latestUpdate.version.compareTo(userSetting.getLastHeardVersion()) > 0) {
            return true;
        }
        if (userSetting.getHeardTimes() == null || userSetting.getHeardTimes() < PLAY_TIMES) {
            return true;
        }
        return false;
    }

    @AllArgsConstructor
    public static class Update {
        @Getter
        private String version;

        @Getter
        private String update;

    }

}
