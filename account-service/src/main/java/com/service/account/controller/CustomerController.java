package com.service.account.controller;

import com.service.account.dto.customer.CustomerRequest;
import com.service.account.dto.customer.CustomerResponse;
import com.service.account.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerRequest request){
        return customerService.createCustomer(request);
    }

    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable UUID id){
        return customerService.getById(id);
    }

}




/*
@PutMapping("/{id}")
public CustomerResponse updateCustomer(@PathVariable UUID id,
                                       @Valid @RequestBody CustomerUpdateRequest request) {
    return customerService.updateCustomer(id, request);
}

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteCustomer(@PathVariable UUID id) {
    customerService.deactivateCustomer(id);
}
*/

