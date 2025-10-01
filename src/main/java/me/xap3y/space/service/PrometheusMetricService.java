package me.xap3y.space.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import me.xap3y.space.api.enums.MetricRecordType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class PrometheusMetricService {

    private final Map<MetricRecordType, Counter> counters = new EnumMap<>(MetricRecordType.class);

    public PrometheusMetricService(MeterRegistry meterRegistry) {
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
}