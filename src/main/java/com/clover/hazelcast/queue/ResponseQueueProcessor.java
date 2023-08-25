package com.clover.hazelcast.queue;

import com.clover.hazelcast.model.HazelcastResponse;
import com.clover.hazelcast.utils.JsonUtils;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class ResponseQueueProcessor extends HazelcastQueueProcessor<String> {

    public ResponseQueueProcessor(HazelcastInstance hazelcastInstance, HazelcastQueueService queueService,
                                  @Value("${hazelcast.response.queue.processor.threads}") int threadCount) {
        super(hazelcastInstance, queueService.getResponseQueueName(), threadCount);
    }

    @Override
    protected void onMessage(String message) {
        HazelcastResponse response;
        try {
            response = JsonUtils.unmarshal(message, HazelcastResponse.class);
            log.info("hazelcast response received {} ", response);
        } catch (IOException e) {
            log.error("could not unmarshal response: " + message, e);
        }
    }
}
