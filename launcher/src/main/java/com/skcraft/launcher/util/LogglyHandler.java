package com.skcraft.launcher.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LogglyHandler extends Handler {
    private final String logglyEndpoint;

    public LogglyHandler(String logglyEndpoint) {
        this.logglyEndpoint = logglyEndpoint;
    }

    @Override
    public void publish(LogRecord record) {
        try {
            URL url = new URL(logglyEndpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);

            String logMessage = formatLogRecord(record);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = logMessage.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Loggly log send failed with response code: " + responseCode);
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

    private String formatLogRecord(LogRecord record) {
        return record.getMessage();
    }
}
