package com.alex.project.service.workers;

import com.alex.project.service.PresignedUrlService;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class ScheduledPresignedMapCleaner implements Runnable {

    private ExecutorService executorService;
    @Inject
    PresignedUrlService service;

    @PostConstruct
    void init() {
        executorService = Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory());
    }

    @PreDestroy
    void destroy() {
        executorService.shutdown();
    }


    @Scheduled(every = "1h")
    public void update() {
        executorService.execute(this);
    }

    @Override
    public void run() {
        service.getCache().entrySet().removeIf(
                entry
                        -> entry.getValue().isExpired()
        );
    }
}

