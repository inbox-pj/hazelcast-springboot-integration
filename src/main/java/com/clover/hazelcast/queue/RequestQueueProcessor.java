package com.clover.hazelcast.queue;

import com.clover.hazelcast.model.HazelcastRequest;
import com.clover.hazelcast.model.HazelcastResponse;
import com.clover.hazelcast.repository.MerchantHsnRepository;
import com.clover.hazelcast.utils.JsonUtils;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class RequestQueueProcessor extends HazelcastQueueProcessor<String> {

    @Autowired
    private HazelcastQueueService queueService;
    @Qualifier("hazelcastInstance")
    @Autowired
    HazelcastInstance hazelcastInstance;
    @Autowired
    MerchantHsnRepository repository;

    @Value("${hazelcast.data.map.name}")
    String mapName;

    @Autowired
    public RequestQueueProcessor(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance, HazelcastQueueService queueService, @Value("${hazelcast.request.queue.processor.threads}") int threadCount) {
        super(hazelcastInstance, queueService.getRequestQueueName(), threadCount);
        this.queueService = queueService;
    }


    @Override
    protected void onMessage(String message) {
        try {
            HazelcastRequest request = JsonUtils.unmarshal(message, HazelcastRequest.class);
            log.info("hazelcast request received {} ", request);

            doResponse(request);
        } catch (IOException e) {
            log.error("cannot process request: " + message, e);
        }
    }

    private void doResponse(HazelcastRequest request) {
        HazelcastResponse response = new HazelcastResponse(request.getRequestProperties().get(HazelcastRequest.RequestProperty.CORRELATION_ID.name()));
        response.setHsn(request.getHsn());
        response.setMerchantId(repository.findMerchantHsnByHsn(response.getHsn()).getMerchantId());

        if(hazelcastInstance.getMap(mapName).containsKey(request.getHsn())) {
            response.setUniqueDisplayId((String) hazelcastInstance.getMap(mapName).get(request.getHsn()));
        }

        if(queueService.addMessageToResponseQueue(queueService.getResponseQueueName(), response)) {
            log.info("Response successfully sent with, correlation-id - {}" ,
                    response.getResponseProperties().get(HazelcastResponse.ResponseProperty.CORRELATION_ID.name()));
        } else {
            log.error("Error while posting response with correlation-id - {}" ,
                    response.getResponseProperties().get(HazelcastResponse.ResponseProperty.CORRELATION_ID.name()));
        }
    }
}
