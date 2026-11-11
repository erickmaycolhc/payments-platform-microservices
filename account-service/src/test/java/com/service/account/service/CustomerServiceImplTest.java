package com.service.account.service;

import com.service.account.dto.customer.CustomerRequest;
import com.service.account.dto.customer.CustomerResponse;
import com.service.account.entity.CustomerEntity;
import com.service.account.enums.CustomerStatus;
import com.service.account.exception.custom.DuplicateResourceException;
import com.service.account.exception.custom.ResourceNotFoundException;
import com.service.account.repository.CustomerRepository;
import com.service.account.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void createCustomer_validRequest_savesCustomerAsActive(){

        CustomerRequest request = new CustomerRequest(
                "DNI",
                "12345678",
                "Erick Perez",
                "erick@correo.com");

        when(customerRepository.existsByDocumentTypeAndDocumentNumber("DNI", "12345678")).thenReturn(false);
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(response.documentNumber()).isEqualTo("12345678");
        verify(customerRepository).save(any(CustomerEntity.class));
    }

    @Test
    void createCustomer_duplicateDocument_throwsExceptionAndDoesNotSave(){

        CustomerRequest request = new CustomerRequest(
                "DNI",
                "12345678",
                "Erick Perez",
                "erick@correo.com");

        when(customerRepository.existsByDocumentTypeAndDocumentNumber("DNI", "12345678")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void getById_existingId_returnsCustomer(){

        UUID id = UUID.randomUUID();
        CustomerEntity entity = CustomerEntity.builder()
                .id(id)
                .documentType("DNI")
                .documentNumber("12345678")
                .fullName("Erick Perez")
                .email("erick@correo.com")
                .status(CustomerStatus.ACTIVE).build();

        when(customerRepository.findById(id)).thenReturn(Optional.of(entity));

        CustomerResponse response = customerService.getById(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void getById_nonExistentId_throwsNotFound(){

        UUID id = UUID.randomUUID();

        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
