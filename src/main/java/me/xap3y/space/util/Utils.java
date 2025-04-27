package me.xap3y.space.util;

import me.xap3y.space.model.UserStats;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    public static String generateRandomId() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 8) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();
    }

    public static boolean containsText(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                for (int i = 0; i < read; i++) {
                    char c = buffer[i];
                    if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static String generateApiKey() {
        SecureRandom random = new SecureRandom();
        StringBuilder apiKey = new StringBuilder(8);
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < 8; i++) {
            apiKey.append(characters.charAt(random.nextInt(characters.length())));
        }

        return apiKey.toString();
    }

    @Nullable
    public static String sha256hex(String text) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        byte[] encodedhash = digest.digest(
                text.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
        for (byte b : encodedhash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static List<Pair<LocalDate, Long>> convertToPairList(LocalDateTime startDate, LocalDateTime endDate, List<Object[]> results, boolean fillMissingDates) {
        Map<LocalDate, Long> resultMap = new HashMap<>();
        for (Object[] result : results) {
            LocalDate date = ((Date) result[0]).toLocalDate();
            Long count = ((Number) result[1]).longValue();
            resultMap.put(date, count);
        }

        if (fillMissingDates) {
            List<Pair<LocalDate, Long>> filledResults = new ArrayList<>();
            LocalDate currentDate = startDate.toLocalDate();
            while (!currentDate.isAfter(endDate.toLocalDate())) {
                Long count = resultMap.getOrDefault(currentDate, 0L);
                filledResults.add(Pair.of(currentDate, count));
                currentDate = currentDate.plusDays(1);
            }
            return filledResults;
        } else {
            return results.stream()
                    .map(result -> Pair.of(((Date) result[0]).toLocalDate(), ((Number) result[1]).longValue()))
                    .collect(Collectors.toList());
        }
    }
}
