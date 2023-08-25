package com.clover.hazelcast.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T unmarshal(String string, Class<T> c) throws IOException {
        return objectMapper.readValue(string, c);
    }

    public static String marshal(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }

}