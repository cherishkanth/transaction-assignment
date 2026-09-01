package com.example.transactionstarter.transaction;

import com.example.transactionstarter.transaction.dto.CreateTransactionRequest;
import com.example.transactionstarter.transaction.dto.UpdateStatusRequest;
import com.example.transactionstarter.transaction.entity.TransactionStatus;
import com.example.transactionstarter.transaction.entity.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest 
{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    // 1. Successful creation
    @Test
    void testCreateTransaction_Success() throws Exception 
    {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setTransactionId("TXN-101");
        req.setCustomerId("CUST-1");
        req.setAmount(new BigDecimal("150.00"));
        req.setCurrency("USD");
        req.setTransactionType(TransactionType.PAYMENT);

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN-101"))
                .andExpect(jsonPath("$.transactionStatus").value("PENDING"));
    }    

    // 2. Rejected because validation fails
    @Test
    void testCreateTransaction_ValidationFailure() throws Exception
    {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setTransactionId(""); // Invalid empty ID
        req.setCustomerId("CUST-1");
        req.setAmount(new BigDecimal("-10.00")); // Invalid negative amount

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.transactionId").exists())
                .andExpect(jsonPath("$.errors.amount").exists());
    }
    

    // 3. Duplicate Transaction ID rejected
    @Test
    void testCreateTransaction_DuplicateId() throws Exception 
    {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setTransactionId("TXN-DUP");
        req.setCustomerId("CUST-1");
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("EUR");
        req.setTransactionType(TransactionType.DEPOSIT);

        // First creation succeeds
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Duplicate creation fails
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
        
    }

    // 4. Non-existent transaction lookup
    @Test
    void testGetTransaction_NotFound() throws Exception 
    {
        mockMvc.perform(get("/api/transactions/NON-EXISTENT-ID"))
                .andExpect(status().isNotFound());
    }
    
 // 5. Successfully update transaction status from PENDING to COMPLETED
    @Test
    void testUpdateStatus_Success() throws Exception {
        // First, create a pending transaction
        CreateTransactionRequest createReq = new CreateTransactionRequest();
        createReq.setTransactionId("TXN-STATUS-1");
        createReq.setCustomerId("CUST-100");
        createReq.setAmount(new BigDecimal("250.00"));
        createReq.setCurrency("USD");
        createReq.setTransactionType(TransactionType.PAYMENT);

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        // Now, update its status to COMPLETED
        UpdateStatusRequest statusReq = new UpdateStatusRequest();
        statusReq.setStatus(TransactionStatus.COMPLETED);

        mockMvc.perform(patch("/api/transactions/TXN-STATUS-1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));
    }

    // 6. Reject status change from a terminal state (COMPLETED -> FAILED)
    @Test
    void testUpdateStatus_InvalidTransitionFromTerminalState() throws Exception {
        // Step 1: Create transaction
        CreateTransactionRequest createReq = new CreateTransactionRequest();
        createReq.setTransactionId("TXN-STATUS-2");
        createReq.setCustomerId("CUST-100");
        createReq.setAmount(new BigDecimal("100.00"));
        createReq.setCurrency("USD");
        createReq.setTransactionType(TransactionType.PAYMENT);

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        // Step 2: Move to COMPLETED (Terminal State)
        UpdateStatusRequest completeReq = new UpdateStatusRequest();
        completeReq.setStatus(TransactionStatus.COMPLETED);

        mockMvc.perform(patch("/api/transactions/TXN-STATUS-2/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk());

        // Step 3: Attempt to move from COMPLETED -> FAILED
        UpdateStatusRequest invalidReq = new UpdateStatusRequest();
        invalidReq.setStatus(TransactionStatus.FAILED);

        mockMvc.perform(patch("/api/transactions/TXN-STATUS-2/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot change status from terminal state: COMPLETED"));
    }
    
    
}




