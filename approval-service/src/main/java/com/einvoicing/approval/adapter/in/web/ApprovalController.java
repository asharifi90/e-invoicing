package com.einvoicing.approval.adapter.in.web;

import com.einvoicing.approval.application.port.in.ManualApprovalUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ManualApprovalUseCase manualApprovalUseCase;

    public ApprovalController(ManualApprovalUseCase manualApprovalUseCase) {
        this.manualApprovalUseCase = manualApprovalUseCase;
    }

    @PostMapping("/{invoiceId}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID invoiceId){
        manualApprovalUseCase.approveManual(invoiceId, "demo-approver");
        return ResponseEntity.ok().build();
    }
}
