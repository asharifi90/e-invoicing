package com.einvoicing.approval.application.port.in;

import java.util.UUID;

public interface ManualApprovalUseCase {

    void approveManual(UUID invoiceId, String approvedBy);
}
