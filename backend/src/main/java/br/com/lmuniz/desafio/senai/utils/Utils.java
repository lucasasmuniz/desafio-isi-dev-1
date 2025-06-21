package br.com.lmuniz.desafio.senai.utils;

public class Utils {
    public static String normalizeName(String name) {
        String normalized = null;
        if (name != null) {
            normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD);
            normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            normalized = normalized.toLowerCase().trim().replaceAll("\\s+", " ");
        }
        return normalized;
    }
}
