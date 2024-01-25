package top.lixetron.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
public class GameScheduler extends ThreadPoolTaskScheduler {

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    public void scheduleWithFixedDelayAndUid(Runnable task, Duration delay, String id) {
        ScheduledFuture<?> future = super.scheduleWithFixedDelay(task, delay);
        scheduledTasks.put(id, future);
    }

    public void cancelScheduledTask(String requestId) {
        ScheduledFuture<?> future = scheduledTasks.get(requestId);
        if (Objects.nonNull(future)) {
            future.cancel(true);
            log.warn("GAME WITH UID: {} stopped", requestId);
        }
    }
}
