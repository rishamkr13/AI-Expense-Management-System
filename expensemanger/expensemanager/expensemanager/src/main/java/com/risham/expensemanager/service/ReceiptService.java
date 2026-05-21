package com.risham.expensemanager.service;

import com.risham.expensemanager.dto.request.ConfirmReceiptRequest;
import com.risham.expensemanager.dto.response.GeminiReceiptResult;
import com.risham.expensemanager.dto.response.OcrResultResponse;
import com.risham.expensemanager.dto.response.ReceiptConfirmResponse;
import com.risham.expensemanager.dto.response.ReceiptResponse;
import com.risham.expensemanager.entity.Expense;
import com.risham.expensemanager.entity.Receipt;
import com.risham.expensemanager.entity.User;
import com.risham.expensemanager.enums.ExpenseCategory;
import com.risham.expensemanager.enums.PaymentMode;
import com.risham.expensemanager.enums.ReceiptStatus;
import com.risham.expensemanager.repository.ExpenseRepository;
import com.risham.expensemanager.repository.ReceiptRepository;
import com.risham.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final OcrService ocrService;
    private final GroqReceiptService groqReceiptService;

    @Value("${app.receipt.upload-dir}")
    private String uploadDir;

    // =========================
    // UPLOAD SINGLE RECEIPT
    // =========================
    public ReceiptResponse uploadReceipt(MultipartFile file) {

        User currentUser = getCurrentUser();

        validateFile(file);

        try {
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String storedFileName = UUID.randomUUID() + fileExtension;

            Path filePath = uploadPath.resolve(storedFileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Receipt receipt = Receipt.builder()
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .filePath(filePath.toString())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .status(ReceiptStatus.UPLOADED)
                    .user(currentUser)
                    .build();

            Receipt savedReceipt = receiptRepository.save(receipt);

            return mapToResponse(savedReceipt);

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload receipt: " + e.getMessage());
        }
    }

    // =========================
    // BULK UPLOAD RECEIPTS
    // =========================
    public List<ReceiptResponse> bulkUploadReceipts(MultipartFile[] files) {

        List<ReceiptResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            responses.add(uploadReceipt(file));
        }

        return responses;
    }

    // =========================
    // GET ALL RECEIPTS
    // =========================
    public List<ReceiptResponse> getAllReceipts() {

        User currentUser = getCurrentUser();

        return receiptRepository.findByUserOrderByUploadedAtDesc(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================
    // GET RECEIPT BY ID
    // =========================
    public ReceiptResponse getReceiptById(Long id) {

        User currentUser = getCurrentUser();

        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Receipt not found with id: " + id));

        if (!receipt.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to access this receipt");
        }

        return mapToResponse(receipt);
    }

    // =========================
    // DELETE RECEIPT
    // =========================
    public String deleteReceipt(Long id) {

        User currentUser = getCurrentUser();

        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Receipt not found with id: " + id));

        if (!receipt.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to delete this receipt");
        }

        try {
            Path path = Paths.get(receipt.getFilePath());

            if (Files.exists(path)) {
                Files.delete(path);
            }

            receiptRepository.delete(receipt);

            return "Receipt deleted successfully";

        } catch (IOException e) {
            throw new RuntimeException("Failed to delete receipt file: " + e.getMessage());
        }
    }

    // ===========================================================
    // PROCESS RECEIPT
    //
    // Strategy (most accurate → least accurate):
    //   1. Groq Vision — send image directly, no Tesseract needed.
    //      Works great for UPI screenshots, printed receipts, photos.
    //   2. Tesseract + Groq text — extract text first, then ask Groq
    //      to parse it. Used when vision fails (e.g. API quota).
    //   3. Rule-based fallback — regex patterns on OCR text.
    //      Last resort, no AI involved.
    // ===========================================================
    public OcrResultResponse processReceipt(Long receiptId) {

        User currentUser = getCurrentUser();

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Receipt not found with id: " + receiptId));

        if (!receipt.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to process this receipt");
        }

        try {
            receipt.setStatus(ReceiptStatus.PROCESSING);
            receiptRepository.save(receipt);

            GeminiReceiptResult result = null;
            String processingSource = null;

            // ── Step 1: Try Groq Vision directly on the image ──────────────
            try {
                System.out.println("========== STRATEGY 1: GROQ VISION ==========");

                String mimeType = receipt.getFileType() != null ? receipt.getFileType() : "image/jpeg";

                result = groqReceiptService.extractReceiptDataFromImage(
                        receipt.getFilePath(), mimeType
                );

                if (result != null
                        && result.getAmount() != null
                        && result.getAmount().compareTo(BigDecimal.ZERO) > 0) {

                    processingSource = "GROQ_VISION";
                    System.out.println("========== GROQ VISION SUCCEEDED ==========");

                } else {
                    System.out.println("Groq Vision returned zero/null amount — trying text fallback.");
                    result = null;
                }

            } catch (Exception visionEx) {
                System.out.println("========== GROQ VISION FAILED ==========");
                visionEx.printStackTrace();
                result = null;
            }

            // ── Step 2: Tesseract OCR → Groq text parsing ──────────────────
            if (result == null) {
                try {
                    System.out.println("========== STRATEGY 2: TESSERACT + GROQ TEXT ==========");

                    String extractedText = ocrService.extractText(receipt.getFilePath());

                    System.out.println("OCR Text:\n" + extractedText);

                    if (extractedText != null && !extractedText.isBlank()) {
                        result = groqReceiptService.extractReceiptDataFromText(extractedText);

                        if (result != null
                                && result.getAmount() != null
                                && result.getAmount().compareTo(BigDecimal.ZERO) > 0) {

                            processingSource = "TESSERACT_PLUS_GROQ_AI";
                            System.out.println("========== TESSERACT+GROQ SUCCEEDED ==========");

                        } else {
                            System.out.println("Groq text returned zero/null amount — using rule-based fallback.");
                            result = buildRuleBasedResult(extractedText);
                            processingSource = "RULE_BASED_FALLBACK";
                        }

                    } else {
                        System.out.println("Tesseract returned blank text — using rule-based fallback on empty string.");
                        result = buildRuleBasedResult("");
                        processingSource = "RULE_BASED_FALLBACK";
                    }

                } catch (Exception textEx) {
                    System.out.println("========== TESSERACT+GROQ FAILED ==========");
                    textEx.printStackTrace();

                    // ── Step 3: Rule-based on whatever Tesseract got ──────────
                    try {
                        System.out.println("========== STRATEGY 3: RULE-BASED FALLBACK ==========");
                        String fallbackText = ocrService.extractText(receipt.getFilePath());
                        result = buildRuleBasedResult(fallbackText != null ? fallbackText : "");
                        processingSource = "RULE_BASED_FALLBACK";
                    } catch (Exception ruleEx) {
                        System.out.println("Rule-based fallback also failed:");
                        ruleEx.printStackTrace();
                        throw new RuntimeException("All OCR strategies failed: " + ruleEx.getMessage());
                    }
                }
            }

            // ── Save result as expense ──────────────────────────────────────
            Expense expense;

            if (receipt.getExpense() != null) {
                expense = receipt.getExpense();
                expense.setAmount(result.getAmount());
                expense.setCategory(result.getCategory());
                expense.setMerchantName(result.getMerchantName());
                expense.setPaymentMode(PaymentMode.OTHER);
                expense.setDescription("Auto-created from " + processingSource);
                expense.setExpenseDate(result.getExpenseDate());
            } else {
                expense = Expense.builder()
                        .amount(result.getAmount())
                        .category(result.getCategory())
                        .merchantName(result.getMerchantName())
                        .paymentMode(PaymentMode.OTHER)
                        .description("Auto-created from " + processingSource)
                        .expenseDate(result.getExpenseDate())
                        .user(currentUser)
                        .build();
            }

            Expense savedExpense = expenseRepository.save(expense);

            receipt.setExtractedText(result.getRawResponse());
            receipt.setStatus(ReceiptStatus.PROCESSED);
            receipt.setProcessedAt(LocalDateTime.now());
            receipt.setExpense(savedExpense);

            Receipt updatedReceipt = receiptRepository.save(receipt);

            System.out.println("========== RECEIPT PROCESSED via " + processingSource + " ==========");
            System.out.println("Amount:   " + result.getAmount());
            System.out.println("Merchant: " + result.getMerchantName());
            System.out.println("Date:     " + result.getExpenseDate());
            System.out.println("Category: " + result.getCategory());

            return OcrResultResponse.builder()
                    .receiptId(updatedReceipt.getId())
                    .expenseId(savedExpense.getId())
                    .extractedText(result.getRawResponse())
                    .extractedAmount(result.getAmount())
                    .extractedMerchantName(result.getMerchantName())
                    .extractedDate(result.getExpenseDate())
                    .category(result.getCategory())
                    .message("Receipt processed using " + processingSource)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();

            receipt.setStatus(ReceiptStatus.FAILED);
            receipt.setProcessedAt(LocalDateTime.now());
            receiptRepository.save(receipt);

            throw new RuntimeException("Receipt processing failed: " + e.getMessage());
        }
    }

    // =========================
    // CONFIRM / CORRECT OCR RESULT
    // =========================
    public ReceiptConfirmResponse confirmReceipt(Long receiptId, ConfirmReceiptRequest request) {

        User currentUser = getCurrentUser();

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Receipt not found with id: " + receiptId));

        if (!receipt.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to confirm this receipt");
        }

        Expense expense;

        if (receipt.getExpense() != null) {

            expense = receipt.getExpense();

            if (!expense.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You are not allowed to update this expense");
            }

            expense.setAmount(request.getAmount());
            expense.setCategory(request.getCategory());
            expense.setMerchantName(
                    request.getMerchantName() != null ? request.getMerchantName() : "Unknown Merchant"
            );
            expense.setPaymentMode(PaymentMode.OTHER);
            expense.setDescription("Confirmed from receipt");
            expense.setExpenseDate(
                    request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now()
            );

        } else {

            expense = Expense.builder()
                    .amount(request.getAmount())
                    .category(request.getCategory())
                    .merchantName(
                            request.getMerchantName() != null ? request.getMerchantName() : "Unknown Merchant"
                    )
                    .paymentMode(PaymentMode.OTHER)
                    .description("Confirmed from receipt")
                    .expenseDate(
                            request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now()
                    )
                    .user(currentUser)
                    .build();
        }

        Expense savedExpense = expenseRepository.save(expense);

        receipt.setExpense(savedExpense);
        receipt.setStatus(ReceiptStatus.PROCESSED);
        receipt.setProcessedAt(LocalDateTime.now());

        Receipt updatedReceipt = receiptRepository.save(receipt);

        return ReceiptConfirmResponse.builder()
                .receiptId(updatedReceipt.getId())
                .expenseId(savedExpense.getId())
                .amount(savedExpense.getAmount())
                .category(savedExpense.getCategory())
                .merchantName(savedExpense.getMerchantName())
                .paymentMode(savedExpense.getPaymentMode())
                .description(savedExpense.getDescription())
                .expenseDate(savedExpense.getExpenseDate())
                .receiptStatus(updatedReceipt.getStatus())
                .message("Receipt confirmed and expense saved successfully")
                .build();
    }

    // =========================
    // RULE BASED FALLBACK
    // =========================
    private GeminiReceiptResult buildRuleBasedResult(String extractedText) {

        BigDecimal amount;
        try {
            amount = extractAmount(extractedText);
        } catch (Exception e) {
            amount = BigDecimal.ZERO;
        }

        String merchantName = extractMerchantName(extractedText);
        LocalDate expenseDate = extractDate(extractedText);
        ExpenseCategory category = predictCategory(extractedText, merchantName);

        return GeminiReceiptResult.builder()
                .amount(amount)
                .merchantName(merchantName)
                .expenseDate(expenseDate)
                .category(category)
                .rawResponse(extractedText)
                .build();
    }

    private BigDecimal extractAmount(String text) {

        if (text == null || text.isBlank()) {
            throw new RuntimeException("OCR text is empty");
        }

        String normalizedText = text
                .replace(",", "")
                .replace("₹", "Rs ")
                .replace("INR", "Rs ")
                .replace("inr", "Rs ");

        // Priority 1: explicit currency prefix (Rs 500, ₹500, INR 500)
        Pattern currencyPattern = Pattern.compile(
                "(?i)(?:rs\\.?|inr)\\s*([0-9]{1,6}(?:\\.[0-9]{1,2})?)"
        );
        Matcher currencyMatcher = currencyPattern.matcher(normalizedText);
        while (currencyMatcher.find()) {
            BigDecimal value = new BigDecimal(currencyMatcher.group(1));
            if (isValidAmount(value)) return value;
        }

        // Priority 2: keyword lines (Total, Amount, Paid, Debited, etc.)
        List<String> keywords = List.of(
                "amount paid", "amount debited", "amount", "grand total",
                "net amount", "total", "paid", "payment", "completed", "debited"
        );
        String[] lines = normalizedText.split("\\R");
        for (String keyword : keywords) {
            for (String line : lines) {
                if (line.toLowerCase().contains(keyword)) {
                    BigDecimal value = findAmountInLine(line);
                    if (isValidAmount(value)) return value;
                }
            }
        }

        // Priority 3: first 10 lines (UPI screenshots show amount near top)
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            BigDecimal value = findAmountInLine(lines[i]);
            if (isValidAmount(value)) return value;
        }

        throw new RuntimeException("Could not extract amount from receipt");
    }

    private BigDecimal findAmountInLine(String line) {

        if (line == null || line.isBlank()) return BigDecimal.ZERO;

        String cleanedLine = line
                .replace(",", "")
                .replaceAll("(?i)[₹]", "")
                .replaceAll("(?i)rs\\.?\\s*", "");

        Pattern pattern = Pattern.compile("\\b[0-9]{1,6}(?:\\.[0-9]{1,2})?\\b");
        Matcher matcher = pattern.matcher(cleanedLine);

        while (matcher.find()) {
            BigDecimal value = new BigDecimal(matcher.group());
            if (isValidAmount(value)) return value;
        }

        return BigDecimal.ZERO;
    }

    /**
     * Fixed: minimum lowered from 10 to 1 so small valid amounts (e.g. ₹5 chai)
     * are not wrongly rejected. Year filter kept (1900-2099).
     */
    private boolean isValidAmount(BigDecimal value) {

        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) return false;

        int intValue = value.intValue();

        // Reject years
        if (intValue >= 1900 && intValue <= 2099) return false;

        // Reject unrealistically large amounts
        if (value.compareTo(new BigDecimal("500000")) > 0) return false;

        // Reject tiny noise values (0.01 etc.) but allow ₹1+
        if (value.compareTo(BigDecimal.ONE) < 0) return false;

        return true;
    }

    private LocalDate extractDate(String text) {

        if (text == null || text.isBlank()) return LocalDate.now();

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yy"),
                DateTimeFormatter.ofPattern("dd-MM-yy")
        );

        List<String> datePatterns = List.of(
                "\\b\\d{2}/\\d{2}/\\d{4}\\b",
                "\\b\\d{2}-\\d{2}-\\d{4}\\b",
                "\\b\\d{4}-\\d{2}-\\d{2}\\b",
                "\\b\\d{2}/\\d{2}/\\d{2}\\b",
                "\\b\\d{2}-\\d{2}-\\d{2}\\b"
        );

        for (String patternText : datePatterns) {
            Pattern pattern = Pattern.compile(patternText);
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                String dateText = matcher.group();
                for (DateTimeFormatter formatter : formatters) {
                    try {
                        return LocalDate.parse(dateText, formatter);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return LocalDate.now();
    }

    private String extractMerchantName(String text) {

        if (text == null || text.isBlank()) return "Unknown Merchant";

        // Skip generic UPI/payment header lines
        List<String> skipKeywords = List.of(
                "invoice", "receipt", "gst", "total", "payment successful",
                "payment", "transaction", "upi", "success", "completed",
                "paid", "debited", "amount", "balance"
        );

        String[] lines = text.split("\\R");

        for (String line : lines) {
            String cleaned = line.trim();
            String lower = cleaned.toLowerCase();

            boolean shouldSkip = skipKeywords.stream().anyMatch(lower::contains)
                    || cleaned.length() < 3
                    || cleaned.matches(".*\\d{5,}.*");   // skip lines that are mostly numbers

            if (!shouldSkip) {
                return cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
            }
        }

        return "Unknown Merchant";
    }

    private ExpenseCategory predictCategory(String text, String merchantName) {

        String value = ((text == null ? "" : text) + " " + (merchantName == null ? "" : merchantName)).toLowerCase();

        if (value.contains("zomato") || value.contains("swiggy") || value.contains("restaurant") ||
                value.contains("food") || value.contains("cafe") || value.contains("pizza") ||
                value.contains("burger") || value.contains("kfc") || value.contains("domino") ||
                value.contains("blinkit") || value.contains("zepto")) {
            return ExpenseCategory.FOOD;
        }

        if (value.contains("uber") || value.contains("ola") || value.contains("metro") ||
                value.contains("railway") || value.contains("irctc") || value.contains("bus") ||
                value.contains("fuel") || value.contains("petrol") || value.contains("diesel") ||
                value.contains("rapido") || value.contains("namma yatri")) {
            return ExpenseCategory.TRAVEL;
        }

        if (value.contains("amazon") || value.contains("flipkart") || value.contains("myntra") ||
                value.contains("meesho") || value.contains("shopping") || value.contains("mall") ||
                value.contains("ajio") || value.contains("nykaa")) {
            return ExpenseCategory.SHOPPING;
        }

        if (value.contains("electricity") || value.contains("water bill") || value.contains("wifi") ||
                value.contains("internet") || value.contains("recharge") || value.contains("mobile bill") ||
                value.contains("broadband") || value.contains("jio") || value.contains("airtel") ||
                value.contains("bsnl") || value.contains("vi ")) {
            return ExpenseCategory.BILLS;
        }

        if (value.contains("hospital") || value.contains("clinic") || value.contains("medicine") ||
                value.contains("pharmacy") || value.contains("doctor") || value.contains("apollo") ||
                value.contains("medplus") || value.contains("netmeds") || value.contains("1mg")) {
            return ExpenseCategory.HEALTH;
        }

        if (value.contains("school") || value.contains("college") || value.contains("course") ||
                value.contains("tuition") || value.contains("book") || value.contains("udemy") ||
                value.contains("coursera") || value.contains("byju") || value.contains("unacademy")) {
            return ExpenseCategory.EDUCATION;
        }

        if (value.contains("movie") || value.contains("netflix") || value.contains("prime") ||
                value.contains("spotify") || value.contains("game") || value.contains("hotstar") ||
                value.contains("zee5") || value.contains("bookmyshow") || value.contains("pvr")) {
            return ExpenseCategory.ENTERTAINMENT;
        }

        return ExpenseCategory.OTHER;
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Receipt file is required");
        }

        String contentType = file.getContentType();

        if (contentType == null || !(
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/jpg")
        )) {
            throw new RuntimeException("Only JPG and PNG image files are allowed for OCR");
        }
    }

    private String getFileExtension(String fileName) {

        if (fileName == null || !fileName.contains(".")) return "";

        return fileName.substring(fileName.lastIndexOf("."));
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    private ReceiptResponse mapToResponse(Receipt receipt) {

        Expense expense = receipt.getExpense();

        return ReceiptResponse.builder()
                .id(receipt.getId())
                .originalFileName(receipt.getOriginalFileName())
                .storedFileName(receipt.getStoredFileName())
                .fileType(receipt.getFileType())
                .fileSize(receipt.getFileSize())
                .status(receipt.getStatus())
                .extractedText(receipt.getExtractedText())
                .expenseId(expense != null ? expense.getId() : null)
                .amount(expense != null ? expense.getAmount() : BigDecimal.ZERO)
                .merchantName(expense != null ? expense.getMerchantName() : null)
                .category(expense != null ? expense.getCategory() : null)
                .paymentMode(expense != null ? expense.getPaymentMode() : null)
                .expenseDate(expense != null ? expense.getExpenseDate() : null)
                .uploadedAt(receipt.getUploadedAt())
                .processedAt(receipt.getProcessedAt())
                .build();
    }
}