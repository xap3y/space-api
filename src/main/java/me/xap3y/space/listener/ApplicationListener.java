package me.xap3y.space.listener;

import me.xap3y.space.service.WebhookService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationListener {


    private final WebhookService webhookService;
    public ApplicationListener(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        webhookService.init();
        //webhookService.postMessage(serverInfo.getHost());
    }
}
