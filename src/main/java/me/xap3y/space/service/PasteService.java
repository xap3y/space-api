package me.xap3y.space.service;

import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.entity.Paste;
import me.xap3y.space.entity.User;
import me.xap3y.space.exception.ResourceNotFoundException;
import me.xap3y.space.mapper.PasteMapper;
import me.xap3y.space.repository.PasteRepository;
import me.xap3y.space.util.Utils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
}
