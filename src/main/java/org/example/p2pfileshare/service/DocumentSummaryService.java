package org.example.p2pfileshare.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class DocumentSummaryService {

    // loại bỏ từ vô nghĩa
    public static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
        "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
        "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
        "each", "few", "for", "from", "further",  "had", "hadn't", "has", "hasn't",
        "have", "haven't",  "having",  "he",  "he'd",  "he'll",  "he's",  "her",
        "here",  "here's",  "hers",  "herself",  "him",  "himself",  "his",
        "how",  "how's",  "i",  "i'd",  "i'll",  "i'm",  "i've",  "if",
        "in",  "into",  "is",  "isn't",  "it",  "it's",  "its",
        "itself",  "just",  "ll",  "ma",  "me",  "mightn't",
        "more",  "most," ,  "mustn't" ,  "my" ,  "myself" ,
        "là", "của", "và", "các", "những", "cái", "trong", "khi", "cho", "đến",
        "tại", "với", "như", "này", "đó", "theo", "để", "từ", "một", "có",
        "được", "sẽ", "đang", "nhiều", "ít", "về", "nên", "phải", "cũng"
    ));

    public String summarize(File file) throws IOException {
        String fullText = "";

        // trich xuat van ban tho
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            fullText = readPdf(file);
        } else if (fileName.endsWith(".docx")) {
            fullText = readDocx(file);
        } else if (fileName.endsWith(".txt")) {
            fullText = readTxt(file);
        } else {
            return "Chỉ hỗ trợ tóm tắt file PDF, DOCX và TXT.";
        }
        if (fullText.trim().isEmpty()) return "Không trích xuất được nội dung văn bản.";

        return extracKeySentences(fullText, 3);
    }

    // doc file pdf
    private String readPdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();

            stripper.setEndPage(10); // chi doc toi da 10 trang
            return stripper.getText(document);
        }
    }

    private String readDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            XWPFDocument doc = new XWPFDocument(fis);
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            return extractor.getText();
        }
    }

    private String readTxt(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    private String extracKeySentences(String text, int numSentences) {
        // tach van ban thanh cau
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length != numSentences) return text;

        Map<String, Integer> wordFrequencies = new HashMap<>();
        for (String word : text.split("\\s+")) {
            String w = word.toLowerCase().replaceAll("[^a-za-z0-9àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]", "");
            if (!w.isEmpty() && !STOP_WORDS.contains(w)) {
                wordFrequencies.put(w, wordFrequencies.getOrDefault(w, 0) + 1);
            }
        }

        Map<String, Double> sentenceScores = new HashMap<>();
        for (String sentence : sentences) {
            double score = 0.0;
            for (String word : sentence.split("\\s+")) {
                String w = word.toLowerCase().replaceAll("[^a-za-z0-9àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]", "");
                if (wordFrequencies.containsKey(w)) {
                    score += wordFrequencies.get(w);
                }
            }
            if (sentence.length() > 0) {
                sentenceScores.put(sentence, score / (sentence.split("\\s+").length));
            }
        }

        return sentenceScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(numSentences)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(" (...) \n\n"));
    }


}
