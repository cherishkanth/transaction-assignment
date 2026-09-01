

package com.example.transactionstarter.transaction.service;

import com.example.transactionstarter.transaction.dto.CreateTransactionRequest;
import com.example.transactionstarter.transaction.entity.Transaction;
import com.example.transactionstarter.transaction.entity.TransactionStatus;
import com.example.transactionstarter.transaction.exception.DuplicateTransactionException;
import com.example.transactionstarter.transaction.exception.InvalidStatusTransitionException;
import com.example.transactionstarter.transaction.exception.TransactionNotFoundException;
import com.example.transactionstarter.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public Transaction createTransaction(CreateTransactionRequest request) {
        if (repository.existsById(request.getTransactionId())) {
            throw new DuplicateTransactionException("Transaction ID already exists: " + request.getTransactionId());
        }

        Transaction transaction = new Transaction(
                request.getTransactionId(),
                request.getCustomerId(),
                request.getAmount(),
                request.getCurrency().toUpperCase(),
                request.getTransactionType(),
                TransactionStatus.PENDING // Default state upon creation
        );

        return repository.save(transaction);
    }

    public Transaction getTransaction(String transactionId) {
        return repository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + transactionId));
    }

    public List<Transaction> getTransactionsByCustomer(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    public Transaction updateTransactionStatus(String transactionId, TransactionStatus newStatus) {
        Transaction transaction = getTransaction(transactionId);
        validateStatusTransition(transaction.getTransactionStatus(), newStatus);
        
        transaction.setTransactionStatus(newStatus);
        return repository.save(transaction);
    }

    private void validateStatusTransition(TransactionStatus current, TransactionStatus next) {
        if (current == next) return; // No change

        // Terminal states cannot transition further
        if (current == TransactionStatus.COMPLETED || current == TransactionStatus.CANCELLED || current == TransactionStatus.FAILED) {
            throw new InvalidStatusTransitionException("Cannot change status from terminal state: " + current);
        }

        // PENDING can move to COMPLETED, FAILED, or CANCELLED
        if (current == TransactionStatus.PENDING) {
            return; // Valid transitions
        }

        throw new InvalidStatusTransitionException("Invalid transition from " + current + " to " + next);
    }
}