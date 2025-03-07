package org.manolin.ftpblost.logs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogsManager {
    private static final Logger logger = LogManager.getLogger(LogsManager.class);

    public static void logInfo(String message) {
        logger.info(message);
    }

    public static void logError(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public static void logDebug(String message) {
        logger.debug(message);
    }

    public static void logWarn(String message) {
        logger.warn(message);
    }
}
