package merits.funskills.pureland.utils;

import java.util.Locale;

import com.amazonaws.services.lambda.runtime.Context;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class AppConfig {

    private static boolean initialized = false;

    private static ThreadLocal<Locale> localeThreadLocal = new ThreadLocal<>();

    private static String playTable;

    public static void init(final Context context) {
        initTableName(context);
        initialized = true;
    }

    private static void initTableName(final Context context) {
        if (context.getFunctionName().endsWith("Beta")) {
            playTable = "pureLandTable-Beta";
        } else if (context.getFunctionName().endsWith("Alpha")) {
            playTable = "pureLandTable-Alpha";
        } else if (context.getFunctionName().endsWith("Prod")) {
            playTable = "pureLandTable-Prod";
        } else {
            throw new RuntimeException("Cannot get table name with function name " +
                context.getFunctionName());
        }
        log.info("Init playTable={}", playTable);
    }

    public static String getPlayTable() {
        checkInit();
        return playTable;
    }

    private static void checkInit() {
        if (!initialized) {
            throw new RuntimeException("AppConfig is not initialized!");
        }
    }

    public static void setLocale(final String locale) {
        localeThreadLocal.set(Locale.US);
    }

    public static Locale getLocale() {
        Locale locale = localeThreadLocal.get();
        return locale == null ? Locale.US : locale;
    }
}
