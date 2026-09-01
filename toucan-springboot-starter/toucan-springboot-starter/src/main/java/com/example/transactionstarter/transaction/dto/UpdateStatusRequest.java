
package com.example.transactionstarter.transaction.dto;

import com.example.transactionstarter.transaction.entity.TransactionStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull(message = "New status is required")
    private TransactionStatus status;

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
}