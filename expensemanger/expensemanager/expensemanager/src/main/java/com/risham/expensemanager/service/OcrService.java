package com.risham.expensemanager.service;

import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class OcrService {

    @Value("${app.tesseract.data-path}")
    private String tessDataPath;

    @Value("${app.tesseract.language}")
    private String language;

    public String extractText(String filePath) {

        try {
            ITesseract tesseract = new Tesseract();

            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage(language);

            // Better OCR settings for payment screenshots
            tesseract.setPageSegMode(6);
            tesseract.setOcrEngineMode(1);

            File imageFile = new File(filePath);

            String text = tesseract.doOCR(imageFile);

            System.out.println("========== OCR TEXT START ==========");
            System.out.println(text);
            System.out.println("========== OCR TEXT END ==========");

            return text;

        } catch (TesseractException e) {
            throw new RuntimeException("OCR failed: " + e.getMessage());
        }
    }
}