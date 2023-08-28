package com.clover.hazelcast.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "merchant_hsn")
public class MerchantHsn implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "hsn", length = 25, unique = true, nullable = false)
    private String hsn;

    @Column(name = "merchant_id", length = 25, unique = true, nullable = false)
    private String merchantId;

    @Transient
    private String uuid;

}
