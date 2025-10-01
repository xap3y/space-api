package me.xap3y.space.util;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import me.xap3y.space.api.wrapper.FileMultipartFile;
import me.xap3y.space.api.enums.ArchiveType;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.FoundImageDto;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class Utils {

    private final ServerInfo serverInfo;

    public Utils(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    public static String generateRandomId() {
        return generateRandomId(8);
    }

    public static String generateRandomId(int length) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < length) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();
    }

    public static String generateRandom6DigitNumber() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
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

    public static String extractQueryParam(String query, String name) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
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

    public static Optional<Pair<Long, Long>> parseBestUploader(List<Object[]> data) {
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = data.get(0);
        Long uid = (Long) row[0];
        Long uploadCount = (Long) row[1];

        return Optional.of(Pair.of(uid, uploadCount));
    }

    @Contract(pure = true)
    public static String structDiscordLink(String text) {
        return structDiscordLink(text, text);
    }

    @Contract(pure = true)
    public static String structDiscordLink(String title, String url) {
        return "[" + title + "](" + url + ")";
    }

    public String structDiscordProfileLink(String username) {
        String url = serverInfo.getFrontEndUrl() + "/user/" + username;
        return "[" + username + "](" + url + ")";
    }

    @Contract(pure = true)
    public static String structDiscordUserTag(String userId) {
        return "<@" + userId + ">";
    }

    @Contract(pure = true)
    public static String structDiscordChannelTag(String channelId) {
        return "<#" + channelId + ">";
    }

    /*public static List<FoundImageDto> extractFoundImages(@NotNull MultipartFile archive) throws IOException {
        List<FoundImageDto> foundImages = new ArrayList<>();

        try (InputStream inputStream = archive.getInputStream();
             ZipInputStream zis = new ZipInputStream(inputStream)) {

            var entry = zis.getNextEntry();
            while (entry != null) {
                String name = entry.getName().toLowerCase();
                if (!entry.isDirectory() && isImageFile(name)) {

                    String fileType = name.substring(name.lastIndexOf('.') + 1);
                    int size = (int) (entry.getSize() / 1024);
                    foundImages.add(new FoundImageDto(name, fileType, size));
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        }
        return foundImages;
    }*/

    public static List<FoundImageDto> extractFoundImages(
            @NotNull MultipartFile archive,
            @NotNull ArchiveType type
    ) throws IOException {
        List<FoundImageDto> foundImages = new ArrayList<>();

        switch (type) {
            case ZIP:
                try (InputStream inputStream = archive.getInputStream();
                     ZipInputStream zis = new ZipInputStream(inputStream)) {
                    ZipEntry entry = zis.getNextEntry();
                    while (entry != null) {
                        String name = entry.getName().toLowerCase();
                        if (!entry.isDirectory() && isImageFile(name)) {
                            String fileType = name.substring(name.lastIndexOf('.') + 1);
                            int size = (int) (entry.getSize() / 1024);
                            foundImages.add(new FoundImageDto(name, fileType, size));
                        }
                        zis.closeEntry();
                        entry = zis.getNextEntry();
                    }
                }
                break;
            case TAR_GZ:
                try (InputStream inputStream = archive.getInputStream();
                     GZIPInputStream gzis = new GZIPInputStream(inputStream);
                     TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
                    TarArchiveEntry entry;
                    while ((entry = tais.getNextTarEntry()) != null) {
                        String name = entry.getName().toLowerCase();
                        if (entry.isFile() && isImageFile(name)) {
                            String fileType = name.substring(name.lastIndexOf('.') + 1);
                            int size = (int) (entry.getSize() / 1024);
                            foundImages.add(new FoundImageDto(name, fileType, size));
                        }
                    }
                }
                break;
            case RAR:
                try (InputStream is = archive.getInputStream()) {
                    Archive rarArchive = null;
                    try {
                        rarArchive = new Archive(is);
                    } catch (RarException e) {
                        throw new IOException("Failed to open RAR archive: " + e.getMessage(), e);
                    }
                    List<FileHeader> headers = rarArchive.getFileHeaders();
                    for (FileHeader header : headers) {
                        String name = header.getFileNameString().toLowerCase();
                        if (!header.isDirectory() && isImageFile(name)) {
                            String fileType = name.substring(name.lastIndexOf('.') + 1);
                            int size = (int) (header.getFullUnpackSize() / 1024);
                            foundImages.add(new FoundImageDto(name, fileType, size));
                        }
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported archive type: " + type);
        }

        return foundImages;
    }

    public static MultipartFile createMultipartFileFromUrl(String url, String filename) {
        File file = Paths.get("temp/" + filename).toFile(); // TODO: temp
        MultipartFile multipartFile;
        try (InputStream in = new URL(url).openStream(); FileOutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            multipartFile = new FileMultipartFile(file);
        } catch (IOException exception) {
            multipartFile = null;
        }

        return multipartFile;
    }

    private static boolean isImageFile(String filename) {
        for (String ext : ConfigDb.SUPPORTED_EXTENSIONS) {
            if (filename.endsWith(ext)) return true;
        }
        return false;
    }
}
