package com.service.account.repository;

import com.service.account.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    boolean existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);
}
