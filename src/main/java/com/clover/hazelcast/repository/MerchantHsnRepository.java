package com.clover.hazelcast.repository;

import com.clover.hazelcast.entity.MerchantHsn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableJpaRepositories
@Transactional
public interface MerchantHsnRepository extends JpaRepository<MerchantHsn, Integer> {

    long deleteByHsn(String hsn);

    MerchantHsn findMerchantHsnByHsn(String hsn);
}
