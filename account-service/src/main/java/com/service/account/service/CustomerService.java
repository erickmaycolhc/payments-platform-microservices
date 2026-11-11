package com.service.account.service;

import com.service.account.dto.customer.CustomerRequest;
import com.service.account.dto.customer.CustomerResponse;

import java.util.UUID;

public interface CustomerService {

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse getById(UUID id);

}
