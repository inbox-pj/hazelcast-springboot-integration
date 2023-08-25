package com.clover.hazelcast.queue;

public interface QueueService<T, U> {

    String getRequestQueueName();

    String getResponseQueueName();

    boolean hasRequestQueue(String hsn);

    boolean doesQueueExist(String queueName);

    void registerRequestQueue(String hsn);

    void unregisterRequestQueue(String hsn);

    boolean addMessageToRequestQueue(T message) throws Exception;

    boolean addMessageToResponseQueue(String queueName, U message);

}