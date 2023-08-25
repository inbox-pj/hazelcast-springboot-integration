package com.clover.hazelcast.model;

import com.clover.hazelcast.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HazelcastRequest implements Serializable {
    private String hsn;

    private Map<String, String> requestProperties;

    public HazelcastRequest(String responseQueueName, String correlationId) {
        this.requestProperties = new HashMap<>();

        requestProperties.put(RequestProperty.RESPONSE_QUEUE_NAME.name(), responseQueueName);
        requestProperties.put(RequestProperty.CORRELATION_ID.name(), correlationId);

    }

    @Override
    public String toString() {
        try {
            return JsonUtils.marshal(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public enum RequestProperty {
        RESPONSE_QUEUE_NAME,
        CORRELATION_ID,
        QUEUE_SEND_TIME;

    }
}
