package com.skcraft.launcher.util;

import java.util.logging.Logger;
import java.util.logging.Level;

public class LogglyIntegration {

    public static void main(String[] args) {
        Logger logger = Logger.getLogger(LogglyIntegration.class.getName());
        logger.setLevel(Level.SEVERE);

        String logglyEndpoint = "https://logs-01.loggly.com/bulk/00cb8265-0d42-41c6-88aa-1226e3f936fd/tag/foxford-launcher";
        LogglyHandler logglyHandler = new LogglyHandler(logglyEndpoint);
        logger.addHandler(logglyHandler);

        logger.info("This is an example log message sent to Loggly.");
    }
}
