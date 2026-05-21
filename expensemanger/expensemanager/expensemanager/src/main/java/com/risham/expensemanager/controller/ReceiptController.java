package com.risham.expensemanager.controller;

import com.risham.expensemanager.dto.request.ConfirmReceiptRequest;
import com.risham.expensemanager.dto.response.OcrResultResponse;
import com.risham.expensemanager.dto.response.ReceiptConfirmResponse;
import com.risham.expensemanager.dto.response.ReceiptResponse;
import com.risham.expensemanager.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
// ✅ removed @CrossOrigin
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReceiptResponse> uploadReceipt(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(receiptService.uploadReceipt(file));
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ReceiptResponse>> bulkUploadReceipts(
            @RequestParam("files") MultipartFile[] files
    ) {
        return ResponseEntity.ok(receiptService.bulkUploadReceipts(files));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<OcrResultResponse> processReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.processReceipt(id));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ReceiptConfirmResponse> confirmReceipt(
            @PathVariable Long id,
            @RequestBody ConfirmReceiptRequest request
    ) {
        return ResponseEntity.ok(receiptService.confirmReceipt(id, request));
    }

    @GetMapping
    public ResponseEntity<List<ReceiptResponse>> getAllReceipts() {
        return ResponseEntity.ok(receiptService.getAllReceipts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> getReceiptById(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.getReceiptById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.deleteReceipt(id));
    }
}