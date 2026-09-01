
package com.example.transactionstarter.transaction.controller;

import com.example.transactionstarter.transaction.dto.CreateTransactionRequest;
import com.example.transactionstarter.transaction.dto.UpdateStatusRequest;
import com.example.transactionstarter.transaction.entity.Transaction;
import com.example.transactionstarter.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController
{

    private final TransactionService service;

    public TransactionController(TransactionService service)
    {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody CreateTransactionRequest request)
    {
        Transaction created = service.createTransaction(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.getTransaction(id));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Transaction>> getCustomerTransactions(@PathVariable("customerId") String customerId) {
        return ResponseEntity.ok(service.getTransactionsByCustomer(customerId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Transaction> updateStatus(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateStatusRequest request) {
        Transaction updated = service.updateTransactionStatus(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}