package com.clover.hazelcast.queue;

import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class HazelcastQueueProcessor<T> {

    private final HazelcastInstance hazelcastInstance;
    private final String queueName;
    private final int threadCount;
    private final ScheduledExecutorService executor;

    public HazelcastQueueProcessor(HazelcastInstance hazelcastInstance, String queueName, int threadCount) {
        this.hazelcastInstance = hazelcastInstance;
        this.queueName = queueName;
        this.threadCount = threadCount;
        executor = Executors.newScheduledThreadPool(threadCount);
        executor.schedule(createPollerTask(queueName), 1, TimeUnit.SECONDS);
    }

    private Runnable createPollerTask(String queueName) {
        return () -> {
            log.info("starting hz queue polling task on {} queue", queueName);
            BlockingQueue<T> queue = hazelcastInstance.getQueue(queueName);
            try {
                T message = queue.take();
                if (message != null) {
                    log.info("received message on {} queue, message {}", queueName, message.toString());
                    onMessage(message);
                }
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
            }
        };
    }

    protected abstract void onMessage(T message);
}
