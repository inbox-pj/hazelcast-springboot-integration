package com.clover.hazelcast.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class HazelcastClusterCoordinator {
    private final HazelcastInstance hazelcastInstance;
    private final int clusterSize;

    @Autowired
    public HazelcastClusterCoordinator(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance,
                                       @Value("${hazelcast.cluster.size}") int clusterSize) {
        this.hazelcastInstance = hazelcastInstance;
        this.clusterSize = clusterSize;
    }

    // Returns true if the localMember is the first/oldest member in the cluster.
    public boolean isMaster() {
        try {
            Member localMember = hazelcastInstance.getCluster().getLocalMember();
            Set<Member> members = hazelcastInstance.getCluster().getMembers();
            return members.size() >= clusterSize && members.iterator().next() == localMember;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}
