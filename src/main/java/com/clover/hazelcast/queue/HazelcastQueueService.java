package com.clover.hazelcast.queue;

import com.clover.hazelcast.model.HazelcastRequest;
import com.clover.hazelcast.model.HazelcastResponse;
import com.clover.hazelcast.utils.JsonUtils;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.AbstractEntryProcessor;
import com.hazelcast.query.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.stripEnd;

@Component
@Slf4j
public class HazelcastQueueService implements QueueService<HazelcastRequest, HazelcastResponse> {

    private final HazelcastInstance hazelcastInstance;
    private final String instanceId;
    private final String requestQueueMapName;
    private ConcurrentMap<String, String> requestQueueMap;
    private static final String REQ_Q_PREFIX = "request-queue.";
    private static final String RES_Q_PREFIX = "response-queue.";

    @Autowired
    public HazelcastQueueService(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance,
                                 @Qualifier("instanceId") String instanceId,
                                 @Value("${hazelcast.request.queue.map.name}") String requestQueueMapName) {

        this.hazelcastInstance = hazelcastInstance;
        this.instanceId = instanceId;
        this.requestQueueMapName = requestQueueMapName;
        requestQueueMap = hazelcastInstance.getMap(requestQueueMapName);
    }

    @Override
    public String getRequestQueueName() {
        return getRequestQueueName(instanceId);
    }

    @Override
    public String getResponseQueueName() {
        return getResponseQueueName(instanceId);
    }

    // register the request queue with hsn
    @Override
    public void registerRequestQueue(String hsn) {
        String requestQueueName = getRequestQueueName();
        if (!requestQueueName.equals(requestQueueMap.get(hsn))) {
            log.info("registering {} with queue {}", hsn, requestQueueName);
            requestQueueMap.put(hsn, requestQueueName);
        }
    }

    @Override
    public void unregisterRequestQueue(String hsn) {
        if (hazelcastInstance.getLifecycleService().isRunning()) {
            String requestQueueName = getRequestQueueName();
            // only remove the hsn entry if it is for this instance's queue.
            hazelcastInstance.getMap(requestQueueMapName).executeOnEntries(
                    new EntryProcessor() {
                    }, new UnregisterPredicate(hsn, requestQueueName));

        } else {
            log.info("hazelcastInstance is not active, could not remove hsn {} from requestQueueMap", hsn);
        }
    }

    @Override
    public boolean hasRequestQueue(String hsn) {
        return requestQueueMap.containsKey(hsn);
    }

    @Override
    public boolean doesQueueExist(String queueName) {
        return hazelcastInstance.getDistributedObjects().stream()
                .map(DistributedObject::getName)
                .anyMatch(name -> name.equals(queueName));
    }


    @Override
    public boolean addMessageToRequestQueue(HazelcastRequest message) {
        String queueName = requestQueueMap.get(message.getHsn());
        if (StringUtils.isNotEmpty(queueName)) {
            try {
                message.getRequestProperties().put(HazelcastRequest.RequestProperty.QUEUE_SEND_TIME.name(), String.valueOf(System.currentTimeMillis()));
                log.info("sending request to queue {}:\n{}", queueName, message);
                boolean isMessageSent =  hazelcastInstance.getQueue(queueName).offer(JsonUtils.marshal(message), 60
                        , TimeUnit.SECONDS);
                log.info("Request sent -> "+ isMessageSent);
                return isMessageSent;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            return false;
        } else {
            throw new HazelcastException("No request queue registered for hsn " + message.getHsn());
        }
    }

    @Override
    public boolean addMessageToResponseQueue(String queueName, HazelcastResponse message) {
        try {
            message.getResponseProperties().put(HazelcastResponse.ResponseProperty.QUEUE_SEND_TIME.name(), String.valueOf(System.currentTimeMillis()));
            log.info("sending response to queue {}:\n{}", queueName, message);
            return hazelcastInstance.getQueue(queueName).offer(JsonUtils.marshal(message), 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    public static String getRequestQueueAttributeName() {
        return stripEnd(REQ_Q_PREFIX, ".");
    }

    public static String getRequestQueueName(String instanceId) {
        return REQ_Q_PREFIX + instanceId;
    }

    public static String getResponseQueueName(String instanceId) {
        return RES_Q_PREFIX + instanceId;
    }

    public static class EntryProcessor extends AbstractEntryProcessor<String, String> {
        @Override
        public Object process(Map.Entry<String, String> entry) {
            log.info("removing requestQueueMap entry for hsn {}", entry.getKey());
            entry.setValue(null);
            return null;
        }
    }

    public static class UnregisterPredicate implements Predicate<String, String> {
        private String hsn;
        private String requestQueueName;

        public UnregisterPredicate(String hsn, String requestQueueName) {
            this.hsn = hsn;
            this.requestQueueName = requestQueueName;
        }

        @Override
        public boolean apply(Map.Entry<String, String> mapEntry) {
            return mapEntry.getKey().equals(hsn) && mapEntry.getValue().equals(requestQueueName);
        }
    }
}
