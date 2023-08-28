package com.clover.hazelcast.controller;

import com.clover.hazelcast.entity.MerchantHsn;
import com.clover.hazelcast.model.HazelcastRequest;
import com.clover.hazelcast.queue.HazelcastQueueService;
import com.clover.hazelcast.repository.MerchantHsnRepository;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/")
@Slf4j
public class HazelcastController {

    @Qualifier("hazelcastInstance")
    @Autowired
    HazelcastInstance hazelcastInstance;
    @Autowired
    HazelcastQueueService queueService;
    @Autowired
    MerchantHsnRepository repository;

    @Value("${hazelcast.data.map.name}")
    String mapName;

    @GetMapping("/get_members")
    public ResponseEntity getMembers() {
        return new ResponseEntity<>(hazelcastInstance.getCluster().getMembers(), HttpStatus.OK);
    }

    @GetMapping("/get_all_hsn")
    public ResponseEntity getAllHsn() {
        List<MerchantHsn> merchantHsnList = repository.findAll();

        merchantHsnList.stream().forEach( t -> {
            t.setUuid((String) hazelcastInstance.getMap(mapName).get(t.getHsn()));
        });

        return new ResponseEntity<>(merchantHsnList, HttpStatus.OK);
    }

    @PutMapping("/add_hsn/{hsn}/{merchantId}")
    @Cacheable(value="hazelcast-cache",key="#hsn")
    public MerchantHsn addHsn(@PathVariable("hsn") String hsn, @PathVariable("merchantId") String merchantId) {
        MerchantHsn merchantHsn = MerchantHsn.builder().merchantId(merchantId).hsn(hsn).build();
        merchantHsn.setUuid(UUID.randomUUID().toString());
        repository.save(merchantHsn);

        if(hazelcastInstance.getMap(mapName).put(hsn, merchantHsn.getUuid()) == null) {
            queueService.registerRequestQueue(hsn);
        }
        return merchantHsn;
    }

    @GetMapping(value="/get_by_hsn/{hsn}")
    @Cacheable(value="hazelcast-cache",key="#hsn")
    public MerchantHsn getByHsn(@PathVariable("hsn") String hsn) {
        MerchantHsn merchantHsn = repository.findMerchantHsnByHsn(hsn);
        merchantHsn.setUuid((String) hazelcastInstance.getMap(mapName).get(hsn));
        return merchantHsn;
    }

    @DeleteMapping("/remove_hsn")
    @CacheEvict(value="hazelcast-cache",key="#hsn")
    public ResponseEntity removeHsn(String hsn) {
        if(repository.deleteByHsn(hsn) > 0) {
            if(hazelcastInstance.getMap(mapName).containsKey(hsn)) {
                queueService.unregisterRequestQueue(hsn);
                hazelcastInstance.getMap(mapName).remove(hsn);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/message/{hsn}")
    public ResponseEntity addMessageToRequestQueue(@PathVariable("hsn") String hsn) {
        HazelcastRequest request = new HazelcastRequest(queueService.getResponseQueueName(), UUID.randomUUID().toString());
        request.setHsn(hsn);
        if(queueService.addMessageToRequestQueue(request)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
