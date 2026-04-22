package me.xap3y.space.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import me.xap3y.space.api.enums.MetricRecordType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PrometheusMetricService {

    private final Map<MetricRecordType, Counter> counters = new EnumMap<>(MetricRecordType.class);
    private final Map<String, Counter> imageViewCounters = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;

    public PrometheusMetricService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        for (MetricRecordType type : MetricRecordType.values()) {
            counters.put(type, Counter.builder("app_events_total")
                    .tag("type", type.name().toLowerCase())
                    .description("Count of " + type.name().toLowerCase() + " events")
                    .register(meterRegistry));
        }
    }

    public void recordEvent(MetricRecordType type) {
        counters.get(type).increment();
    }

    public Map<MetricRecordType, Double> getAllMetrics() {
        Map<MetricRecordType, Double> result = new EnumMap<>(MetricRecordType.class);
        counters.forEach((k, v) -> result.put(k, v.count()));
        return result;
    }

    public void recordImageView(String uniqueId) {
        recordImageView(uniqueId, "GET");
    }

    public void recordImageView(String uniqueId, String type) {
        imageViewCounters
                .computeIfAbsent(uniqueId, id -> Counter.builder("spring_image_stats")
                        .tag("application", "space")
                        .tag("method", type)
                        .tag("uniqueId", id)
                        .register(meterRegistry))
                .increment();
    }

    public void recordIpAccess(String ip, String path) {
        Counter.builder("app_ip_access_total")
                .tag("ip", ip)
                .tag("path", path)
                .description("Tracking unique IP accesses per path")
                .register(meterRegistry)
                .increment();
    }
}