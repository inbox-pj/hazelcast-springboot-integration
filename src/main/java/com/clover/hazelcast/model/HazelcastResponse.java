package com.clover.hazelcast.model;

import com.clover.hazelcast.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HazelcastResponse {
    private String hsn;
    private String merchantId;

    private Map<String, String> responseProperties;

    public HazelcastResponse(String correlationId) {
        this.responseProperties = new HashMap<>();

        responseProperties.put(ResponseProperty.CORRELATION_ID.name(), correlationId);
    }

    @Override
    public String toString() {
        try {
            return JsonUtils.marshal(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public enum ResponseProperty {
        CORRELATION_ID,
        QUEUE_SEND_TIME;
    }
}
