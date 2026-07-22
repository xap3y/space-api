package me.xap3y.space.listener;

import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LokiStatusListener extends OnConsoleStatusListener {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss,SSS");

    @Override
    public void addStatusEvent(Status status) {
        String msg = status.getMessage();
        if (msg != null && msg.contains("Error while sending Batch") && msg.contains("to Loki")) {
            String time = dateFormat.format(new Date(status.getDate()));
            String originName = status.getOrigin() != null ? status.getOrigin().getClass().getSimpleName() : "LokiAppender";
            System.out.println(time + " |-WARN in " + originName + " - " + msg + " (Loki is unreachable/error)");
            return;
        }
        super.addStatusEvent(status);
    }
}
