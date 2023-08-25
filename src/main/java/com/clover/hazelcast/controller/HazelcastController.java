package com.clover.hazelcast.controller;

import com.clover.hazelcast.model.HazelcastRequest;
import com.clover.hazelcast.queue.HazelcastQueueService;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/")
@Slf4j
public class HazelcastController {

    @Autowired
    HazelcastInstance hazelcastInstance;
    @Autowired
    HazelcastQueueService queueService;

    @Value("${hazelcast.data.map.name}")
    String mapName;

    @GetMapping("/get_members")
    public ResponseEntity getMembers() {
        return new ResponseEntity<>(hazelcastInstance.getCluster().getMembers(), HttpStatus.OK);
    }

    @PutMapping("/add_hsn")
    public ResponseEntity addHsn(String hsn, String merchantId) {
        if(hazelcastInstance.getMap(mapName).putIfAbsent(hsn, merchantId) == null) {
            queueService.registerRequestQueue(hsn);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/remove_hsn")
    public ResponseEntity addHsn(String hsn) {
        if(hazelcastInstance.getMap(mapName).containsKey(hsn)) {
            queueService.unregisterRequestQueue(hsn);
            hazelcastInstance.getMap(mapName).remove(hsn);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/get_all_hsn")
    public ResponseEntity getAllHsn() {
        List<String> responseList = new ArrayList<>();
        hazelcastInstance.getMap(mapName).entrySet().stream().forEach(s -> responseList.add(s.getKey().toString() + "/" + s.getValue().toString()));
        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }

    @PutMapping("/message")
    public ResponseEntity addMessageToRequestQueue(String hsn) {
        HazelcastRequest request = new HazelcastRequest(queueService.getResponseQueueName(), UUID.randomUUID().toString());
        request.setHsn(hsn);
        if(queueService.addMessageToRequestQueue(request)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
