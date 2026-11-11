package com.service.account.controller;

import com.service.account.dto.customer.CustomerResponse;
import com.service.account.enums.CustomerStatus;
import com.service.account.exception.custom.DuplicateResourceException;
import com.service.account.exception.custom.ResourceNotFoundException;
import com.service.account.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
public class CustomerControllerTest {

    /*
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private static final String VALID_REQUEST = """
            {
              "documentType": "DNI",
              "documentNumber": "12345678",
              "fullName": "Erick Pérez",
              "email": "erick@correo.com"
            }
            """;


    @Test
    void createdCustomer_validRequest_returns201() throws Exception{
        UUID id = UUID.randomUUID();

        CustomerResponse response = new CustomerResponse(
                id,
                "DNI",
                "12345678",
                "Erick Perez",
                "erick@correo.com",
                CustomerStatus.ACTIVE,
                LocalDateTime.now()
        );

        when(customerService.createCustomer(any())).thenReturn(response);

        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createCustomer_invalidEmail_returns400AndDoesNotCallService() throws Exception {
        String invalidRequest = """
                {
                  "documentType": "DNI",
                  "documentNumber": "12345678",
                  "fullName": "Erick Perez",
                  "email": "it-is-not-an-email"
                }
                """;

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    @Test
    void createCustomer_duplicateDocument_returns409() throws Exception {
        when(customerService.createCustomer(any()))
                .thenThrow(new DuplicateResourceException("A client with that document already exists."));

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isConflict());
    }

    @Test
    void findById_existingId_returns200() throws Exception {
        UUID id = UUID.randomUUID();

        CustomerResponse response = new CustomerResponse(
                id, "DNI", "12345678", "Erick Perez", "erick@correo.com",
                CustomerStatus.ACTIVE, LocalDateTime.now());

        when(customerService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.fullName").value("Erick Perez"));
    }

    @Test
    void findById_nonExistentId_returns404() throws Exception {
        UUID id = UUID.randomUUID();

        when(customerService.getById(id))
                .thenThrow(new ResourceNotFoundException("Customer not found: " + id));

        mockMvc.perform(get("/customers/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/customers/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }



     */
}
