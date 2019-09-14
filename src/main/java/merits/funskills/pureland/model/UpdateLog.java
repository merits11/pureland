package merits.funskills.pureland.model;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class UpdateLog {

    private static final int PLAY_TIMES = 2;

    private static Map<String, String> updates = new HashMap<String, String>() {
        {

            put("2019.0824.0810",
                   "<voice name=\"Salli\">We have added a new list!\n" +
                           "Now you can play the full <prosody rate=\"slow\">80</prosody> scrolls of Avatamsaka Sutra" +
                           ".\n" +
                           "Also known as: </voice>\n" +
                           "<voice name=\"Matthew\">Hua Yan Jin</voice>\n" +
                           "<voice name=\"Salli\"> or, Flower Garland Sutra.</voice>\n" +
                           "To play this list, try list number <prosody rate=\"slow\">80.</prosody> ");
            put("2018.1214.0816",
                "Latest update! Now you can ask Pure Land to jump to any piece in a list. While a stream is playing,"
                    + " try something like: ask pure land to go to sequence 5. Check pure land skills page for more "
                    + "details.");

            put("2018.0915.1451",
                "Latest update: volumes for all audio pieces have been adjusted. You might experience significant "
                    + "improvements for some lists. Enjoy!");

            put("2018.0720.1142",
                "Latest update: I can now play by list name. Try saying: 'heart sutra'. Check pure land Alexa "
                    + "skills page for more.");

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
