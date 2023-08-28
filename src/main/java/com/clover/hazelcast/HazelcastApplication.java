package com.clover.hazelcast;

import com.clover.hazelcast.cluster.ClusterMembershipListener;
import com.clover.hazelcast.queue.HazelcastQueueService;
import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.ManagementCenterConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

@SpringBootApplication
@Slf4j
@EnableCaching
public class HazelcastApplication {

	public static void main(String[] args) {
		SpringApplication.run(HazelcastApplication.class, args);
	}

	@Bean
	public HazelcastInstance hazelcastInstance(
			@Value("${hazelcast.group.name}") String groupName,
			@Value("${hazelcast.group.password}") String groupPassword,
			@Value("${hazelcast.network.port}") int port,
			@Value("${hazelcast.network.port.autoincrement}") boolean autoincrementPort,
			@Value("${hazelcast.network.join.multicast.enabled}") boolean multicastEnabled,
			@Value("${hazelcast.network.join.multicast.group}") String multicastGroup,
			@Value("${hazelcast.network.join.multicast.port}") int multicastPort,
			@Value("${hazelcast.network.join.multicast.timeout.seconds}") int multicastTimeout,
			@Qualifier("instanceId") String instanceId,
			@Value("${hazelcast.request.queue.map.name}") String requestQueueMapName,
			@Value("${hazelcast.request.queue.max.size}") int requestQueueMaxSize,
			@Value("${hazelcast.response.queue.max.size}") int responseQueueMaxSize,
			@Value("${hazelcast.merge.first.run.delay.seconds}") String mergeFirstRunDelay,
			@Value("${hazelcast.merge.next.run.delay.seconds}") String mergeNextRunDelay,
			@Value("${hazelcast.max.no.heartbeat.seconds}") String maxNoHeartbeatSeconds,
			@Value("${hazelcast.data.map.name}") String dataMapName) {

		GroupConfig groupConfig = new GroupConfig();
		groupConfig.setName(groupName);
		groupConfig.setPassword(groupPassword);

		Properties hazelcastProperties = new Properties();
		hazelcastProperties.setProperty("hazelcast.logging.type", "slf4j");
		hazelcastProperties.setProperty("hazelcast.jmx", "true");
		hazelcastProperties.setProperty("hazelcast.merge.first.run.delay.seconds", mergeFirstRunDelay);
		hazelcastProperties.setProperty("hazelcast.merge.next.run.delay.seconds", mergeNextRunDelay);
		hazelcastProperties.setProperty("hazelcast.max.no.heartbeat.seconds", maxNoHeartbeatSeconds);
		hazelcastProperties.setProperty("hazelcast.shutdownhook.enabled", "false");

		MemberAttributeConfig memberAttributeConfig = new MemberAttributeConfig();
		memberAttributeConfig.setStringAttribute(
				HazelcastQueueService.getRequestQueueAttributeName(),
				HazelcastQueueService.getRequestQueueName(instanceId));

		NetworkConfig networkConfig = new NetworkConfig();
		networkConfig.setPort(port);
		networkConfig.setPortAutoIncrement(autoincrementPort);
		networkConfig.setJoin(new JoinConfig());
		networkConfig.getJoin().setMulticastConfig(new MulticastConfig());
		networkConfig.getJoin().getMulticastConfig().setEnabled(multicastEnabled);

		if (multicastEnabled) {
			networkConfig.getJoin().getMulticastConfig().setMulticastGroup(multicastGroup);
			networkConfig.getJoin().getMulticastConfig().setMulticastPort(multicastPort);
			networkConfig.getJoin().getMulticastConfig().setMulticastTimeoutSeconds(multicastTimeout);
		}

		MapConfig requestQueueMapConfig = new MapConfig();
		requestQueueMapConfig.setName(requestQueueMapName);
		requestQueueMapConfig.setMergePolicy("com.hazelcast.map.merge.LatestUpdateMapMergePolicy");

		MapConfig dataMapConfig = new MapConfig();
		dataMapConfig.setName(dataMapName);
		dataMapConfig.setMergePolicy("com.hazelcast.map.merge.LatestUpdateMapMergePolicy");

		QueueConfig requestQueueConfig = new QueueConfig();
		requestQueueConfig.setName(HazelcastQueueService.getRequestQueueName(instanceId));
		requestQueueConfig.setMaxSize(requestQueueMaxSize);

		QueueConfig responseQueueConfig = new QueueConfig();
		responseQueueConfig.setName(HazelcastQueueService.getResponseQueueName(instanceId));
		responseQueueConfig.setMaxSize(responseQueueMaxSize);

		MapConfig cacheConfig = new MapConfig();
		cacheConfig.setName("hazelcast-cache")
				.setTimeToLiveSeconds(5000)
				.setMaxSizeConfig(new MaxSizeConfig(200,com.hazelcast.config.MaxSizeConfig.MaxSizePolicy.FREE_HEAP_SIZE))
				.setEvictionPolicy(EvictionPolicy.LRU);

		Config cfg = new Config();
		cfg.setGroupConfig(groupConfig);
		cfg.setProperties(hazelcastProperties);
		// members
		cfg.setMemberAttributeConfig(memberAttributeConfig);
		cfg.setNetworkConfig(networkConfig);
		// map
		cfg.addMapConfig(requestQueueMapConfig);
		cfg.addMapConfig(dataMapConfig);
		// queue
		cfg.addQueueConfig(requestQueueConfig);
		cfg.addQueueConfig(responseQueueConfig);

		// cache config
		cfg.addMapConfig(cacheConfig);

		HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);
		hazelcastInstance.getCluster().addMembershipListener(
				new ClusterMembershipListener(hazelcastInstance, requestQueueMapName));

		return hazelcastInstance;
	}

	@Bean(name = "instanceId")
	public String instanceId(@Value("${instance.id}") String instanceId) {
		log.info("Using '{}' as the application instance id", instanceId);
		return instanceId;
	}
}
