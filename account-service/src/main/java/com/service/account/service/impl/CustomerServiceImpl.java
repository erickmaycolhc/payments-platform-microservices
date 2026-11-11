package com.service.account.service.impl;

import com.service.account.dto.customer.CustomerRequest;
import com.service.account.dto.customer.CustomerResponse;
import com.service.account.entity.CustomerEntity;
import com.service.account.enums.CustomerStatus;
import com.service.account.exception.custom.DuplicateResourceException;
import com.service.account.exception.custom.ResourceNotFoundException;
import com.service.account.repository.CustomerRepository;
import com.service.account.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request){
        // valid duplicated
        if(customerRepository.existsByDocumentTypeAndDocumentNumber(request.documentType(), request.documentNumber())){
            throw new DuplicateResourceException("The client with that document already exists " + request.documentType() + " " + request.documentNumber());
        }

        CustomerEntity entity = CustomerEntity.builder()
                .documentType(request.documentType())
                .documentNumber(request.documentNumber())
                .fullName(request.fullName())
                .email(request.email())
                .status(CustomerStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        return CustomerResponse.fromEntity(customerRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id){
        CustomerEntity entity = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));

        return CustomerResponse.fromEntity(entity);
    }
}
