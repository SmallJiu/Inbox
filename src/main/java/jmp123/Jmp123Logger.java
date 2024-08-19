package jmp123;

/**
 * Hook jmp123 normal logger system.
 */
public class Jmp123Logger {
    public static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("jmp123");
    private static boolean enable;
    public static void enable(boolean enable){
        Jmp123Logger.enable = enable;
    }
    public static void info(String s, Object... args) {
        if (enable) {
            LOGGER.info(String.format(s, args));
        }
    }
    public static void error(String s, Object... args) {
        if (enable) {
            LOGGER.error(String.format(s, args));
        }
    }
}
