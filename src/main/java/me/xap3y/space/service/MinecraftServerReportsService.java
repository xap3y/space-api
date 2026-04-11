package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.entity.MinecraftServerReports;
import me.xap3y.space.repository.MinecraftServerReportsRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class MinecraftServerReportsService {

    private final MinecraftServerReportsRepository minecraftServerReportsRepository;

    public Optional<MinecraftServerReports> getReportById(Long id) {
        return minecraftServerReportsRepository.findById(id);
    }

    public Optional<MinecraftServerReports> getServerByApiKey(String apiKey) {
        return minecraftServerReportsRepository.findByApiKey(apiKey);
    }

    public @NonNull List<MinecraftServerReports> findAll() {
        return minecraftServerReportsRepository.findAll();
    }

    public MinecraftServerReports getServerByApiKeyStrict(String apiKey) {
        return minecraftServerReportsRepository.findByApiKey(apiKey).orElseThrow(() -> new ResourceNotFoundException("Minecraft server not found with API key: " + apiKey));
    }

    public Optional<MinecraftServerReports> findByOwnerEmail(String ownerEmail) {
        return minecraftServerReportsRepository.findByOwnerEmail(ownerEmail);
    }

    public Optional<MinecraftServerReports> findByServerName(String serverName) {
        return minecraftServerReportsRepository.findByServerName(serverName);
    }

    public boolean existsByServerName(String serverName) {
        return minecraftServerReportsRepository.existsByServerName(serverName);
    }

    public boolean existsByOwnerEmail(String ownerEmail) {
        return minecraftServerReportsRepository.existsByOwnerEmail(ownerEmail);
    }

    public MinecraftServerReports save(MinecraftServerReports report) {
        return minecraftServerReportsRepository.save(report);
    }

    public void deleteById(Long id) {
        minecraftServerReportsRepository.deleteById(id);
    }
}
