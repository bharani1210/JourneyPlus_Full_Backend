package com.journeyplus.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public class ExpenseClaimRequest {
    private String claimTitle;
    private LocalDate submittedDate;
    private BigDecimal totalAmount;
    private String originalCurrency;
    private List<ExpenseLineRequest> expenseLines = new ArrayList<>();

    public String getClaimTitle() {
        return claimTitle;
    }

    public void setClaimTitle(String claimTitle) {
        this.claimTitle = claimTitle;
    }

    public LocalDate getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(LocalDate submittedDate) {
        this.submittedDate = submittedDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public List<ExpenseLineRequest> getExpenseLines() {
        if (expenseLines == null) {
            expenseLines = new ArrayList<>();
        }
        return expenseLines;
    }

    public void setExpenseLines(List<ExpenseLineRequest> expenseLines) {
        this.expenseLines = expenseLines;
    }
}
