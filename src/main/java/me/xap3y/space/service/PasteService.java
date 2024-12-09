package me.xap3y.space.service;

import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.entity.Paste;
import me.xap3y.space.entity.User;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.mapper.PasteMapper;
import me.xap3y.space.repository.PasteRepository;
import me.xap3y.space.util.Utils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PasteService {

    private final PasteRepository pasteRepository;
    private final PasteMapper pasteMapper;

    public PasteService(PasteRepository pasteRepository, PasteMapper pasteMapper) {
        this.pasteRepository = pasteRepository;
        this.pasteMapper = pasteMapper;
    }

    public Optional<Paste> getPasteByUniqueId(String id) {
        return pasteRepository.findByUniqueId(id);
    }

    public Optional<List<Paste>> getPastesByUser(User user) {
        return pasteRepository.findByCreatedBy(user);
    }

    public List<PasteDto> getPastesByUserId(User user) {
        return pasteRepository.findByCreatedBy(user)
                .orElseThrow(() -> new ResourceNotFoundException("No pastes found for user"))
                .stream()
                .map(pasteMapper)
                .collect(Collectors.toList());
    }

    public PasteDto savePaste(String text, User uploader) throws IllegalArgumentException, OptimisticLockingFailureException {
        Paste pasteDto = new Paste();
        pasteDto.setContent(text);
        pasteDto.setCreatedBy(uploader);
        pasteDto.setPublic(true);
        pasteDto.setCreatedAt(LocalDateTime.now());
        pasteDto.setUniqueId(Utils.generateRandomId());

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
}
