package me.xap3y.space.service;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.dto.PasteSummary;
import me.xap3y.space.entity.Paste;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.PasteMapper;
import me.xap3y.space.repository.PasteRepository;
import me.xap3y.space.util.HuffmanEncoder;
import me.xap3y.space.util.Utils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PasteService {

    private final PasteRepository pasteRepository;
    private final PasteMapper pasteMapper;
    private final HuffmanEncoder huffmanEncoder;

    public PasteService(PasteRepository pasteRepository, PasteMapper pasteMapper, HuffmanEncoder huffmanEncoder) {
        this.pasteRepository = pasteRepository;
        this.pasteMapper = pasteMapper;
        this.huffmanEncoder = huffmanEncoder;
    }

    public Optional<Paste> getPasteByUniqueId(String id) {
        return pasteRepository.findByUniqueId(id);
    }

    public void deleteByUniqueId(String uniqueId) {
        this.pasteRepository.deleteByUniqueId(uniqueId);
    }

    public List<Paste> getPastesByUser(User user) {
        return pasteRepository.findByCreatedBy(user);
    }

    public int countPastesByUserId(Long uid) {
        return pasteRepository.countAllByCreatedById(uid);
    }

    public List<PasteDto> getPastesByUserId(User user) {
        return pasteRepository.findByCreatedBy(user).stream()
                .map(pasteMapper)
                .collect(Collectors.toList());
    }

    public PasteDto savePaste(String text, User uploader) {
        return savePaste("Untitled", text, uploader);
    }

    public PasteDto savePaste(String title, String text, User uploader) throws IllegalArgumentException, OptimisticLockingFailureException {
        log.info("ENCODING TEXT: {}", text);
        //byte[] encodedText =  huffmanEncoder.encode(text);
        //String encodedText =  huffmanEncoder.encode(text);
        log.info("ENCODING TEXT: {}", text);
        Paste pasteDto = new Paste();
        pasteDto.setTitle(title);
        pasteDto.setContent(text);
        pasteDto.setCreatedBy(uploader);
        pasteDto.setPublic(true);
        pasteDto.setCreatedAt(LocalDateTime.now());
        pasteDto.setUniqueId(Utils.generateRandomId());

        log.info("APPLYING MAPPER: {}", pasteDto);

        return pasteMapper.apply(pasteRepository.save(pasteDto));
    }

    public Map<String, ?> getStats(LocalDateTime fromTime, LocalDateTime toTime) {
        List<Object[]> stats = pasteRepository.findBiggestCreatorInRange(fromTime, toTime);
        Object[] biggest;
        if (!stats.isEmpty()) biggest = stats.get(0);
        else biggest = null;

        Map<String, ?> map = new HashMap<>() {{
            put("fromDate", fromTime);
            put("toDate", toTime);
            put("biggest_creator", biggest != null ? Map.of(
                    "uid", biggest[0],
                    "username", biggest[1],
                    "avatar", biggest[2],
                    "uploads", biggest[3]
            ) : null);
            put("total_pastes", pasteRepository.count());
        }};
        return map;
    }

    public List<PasteDto> getAllPastesByUserId(Long uid, boolean includeContent) {
        return pasteRepository.findByCreatedById(uid)
                .stream()
                .map((p) -> pasteMapper.apply(p, includeContent))
                .toList();
    }

    public List<Pair<LocalDate, Long>> findTotalImagesPerDayByUser(LocalDateTime startDate, LocalDateTime endDate, Long uploaderId, boolean fillMissingDates) {
        List<Object[]> results = pasteRepository.findTotalPastesPerDayByUser(startDate.with(LocalTime.MIN), endDate.with(LocalTime.MAX), uploaderId);
        return Utils.convertToPairList(startDate, endDate, results, fillMissingDates);
    }

    public Optional<Pair<Long, Long>> findBestUploader(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> result = pasteRepository.findBiggestCreatorInRangeWithId(startDate, endDate).orElse(null);
        return Utils.parseBestUploader(result);
    }

    public long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return pasteRepository.countByCreatedAtBetween(startDate, endDate);
    }
}
