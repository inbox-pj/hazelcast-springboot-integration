package com.clover.hazelcast.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.map.AbstractEntryProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

import static com.clover.hazelcast.queue.HazelcastQueueService.getRequestQueueAttributeName;

@Slf4j
public class ClusterMembershipListener implements MembershipListener {

    private final HazelcastInstance hazelcastInstance;
    private final String requestQueueMapName;

    public ClusterMembershipListener(HazelcastInstance hazelcastInstance, String requestQueueMapName) {
        this.hazelcastInstance = hazelcastInstance;
        this.requestQueueMapName = requestQueueMapName;
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        if (isFirstNodeInCluster()) {
            String queue = membershipEvent.getMember().getStringAttribute(getRequestQueueAttributeName());
            log.info("Member leaving, queue: " + queue);

            hazelcastInstance.getMap(requestQueueMapName).executeOnEntries(
                    new AbstractEntryProcessor<String, String>() {
                        @Override
                        public Object process(Map.Entry<String, String> entry) {
                            log.info("removing entry {}", entry.getKey());
                            entry.setValue(null);
                            return null;
                        }
                    },
                    mapEntry -> mapEntry.getValue().equals(queue));
        }
    }

    private boolean isFirstNodeInCluster() {
        Set<Member> members = hazelcastInstance.getCluster().getMembers();
        Member localMember = hazelcastInstance.getCluster().getLocalMember();
        return members.iterator().next().equals(localMember);
    }

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        log.info("Member {}, event {}", membershipEvent.getMember(), membershipEvent);
    }

    @Override
    public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
        log.info("Attribute {} {} for Member {}", memberAttributeEvent.getKey(), memberAttributeEvent.getOperationType().toString(), memberAttributeEvent.getMember());
    }
}