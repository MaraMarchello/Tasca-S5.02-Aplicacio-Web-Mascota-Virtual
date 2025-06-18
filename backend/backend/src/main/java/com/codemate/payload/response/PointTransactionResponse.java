package com.codemate.payload.response;

import com.codemate.model.TransactionType;
import com.codemate.model.PointSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PointTransactionResponse {
    private Long id;
    private TransactionType type;
    private PointSource source;
    private Long amount;
    private String description;
    private String referenceId;
    private Date createdAt;
} 