package me.xap3y.space.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class SystemMetricsService {

    private final MeterRegistry meterRegistry;
    private final ImageService imageService;

    public SystemMetricsService(MeterRegistry meterRegistry, ImageService imageService) {
        this.meterRegistry = meterRegistry;
        this.imageService = imageService;
    }

    private Double getGaugeValue(String name) {
        Gauge g = meterRegistry.find(name).gauge();
        return g != null ? g.value() : null;
    }

    // =========================
    // 🔥 CPU Metrics
    // =========================
    public Double getSystemCpuUsage() {
        return getGaugeValue("system.cpu.usage");
    }

    public Double getProcessCpuUsage() {
        return getGaugeValue("process.cpu.usage");
    }

    public Double getSystemCpuCount() {
        return getGaugeValue("system.cpu.count");
    }

    public Double getProcessCpuTime() {
        return getGaugeValue("process.cpu.time"); // seconds
    }

    // =========================
    // 🔥 Memory Metrics (JVM)
    // =========================
    public Double getJvmMemoryUsed() {
        return getGaugeValue("jvm.memory.used");
    }

    public Double getJvmMemoryCommitted() {
        return getGaugeValue("jvm.memory.committed");
    }

    public Double getJvmMemoryMax() {
        return getGaugeValue("jvm.memory.max");
    }

    public Double getJvmBufferMemoryUsed() {
        return getGaugeValue("jvm.buffer.memory.used");
    }

    public Double getJvmBufferCount() {
        return getGaugeValue("jvm.buffer.count");
    }

    // =========================
    // 🔥 Memory Metrics (System RAM)
    // =========================
    public Double getSystemTotalMemory() {
        return getGaugeValue("system.memory.total");
    }

    public Double getSystemFreeMemory() {
        return getGaugeValue("system.memory.free");
    }

    public Double getProcessMemoryUsed() {
        return getGaugeValue("process.memory.usage");
    }

    public Double getProcessMemoryVirtual() {
        return getGaugeValue("process.memory.virtual");
    }

    // =========================
    // 🔥 GC Metrics
    // =========================
    public Double getGcPauseCount() {
        return getGaugeValue("jvm.gc.pause.count");
    }

    public Double getGcPauseTotalTime() {
        return getGaugeValue("jvm.gc.pause.totalTime");
    }

    // =========================
    // 🔥 Thread Metrics
    // =========================
    public Double getThreadLive() {
        return getGaugeValue("jvm.threads.live");
    }

    public Double getThreadDaemon() {
        return getGaugeValue("jvm.threads.daemon");
    }

    public Double getThreadPeak() {
        return getGaugeValue("jvm.threads.peak");
    }

    // =========================
    // 🔥 Disk Metrics
    // =========================
    public Double getDiskTotal() {
        return getGaugeValue("disk.total");
    }

    public Double getDiskFree() {
        return getGaugeValue("disk.free");
    }

    // =========================
    // 🔥 I/O Metrics (Spring Boot)
    // =========================
    public Double getProcessFilesOpen() {
        return getGaugeValue("process.files.open");
    }

    public Double getProcessFilesMax() {
        return getGaugeValue("process.files.max");
    }

    // =========================
    // 🔥 Uptime Metrics
    // =========================
    public Double getProcessStartTime() {
        return getGaugeValue("process.start.time");
    }

    public Double getProcessUpTime() {
        return getGaugeValue("process.uptime");
    }

    // =========================
    // 🔥 Executor & Pool Metrics
    // =========================
    public Double getExecutorPoolSize() {
        return getGaugeValue("executor.pool.size");
    }

    public Double getExecutorActive() {
        return getGaugeValue("executor.active");
    }

    public Double getExecutorCompleted() {
        return getGaugeValue("executor.completed");
    }

    public Double getExecutorQueued() {
        return getGaugeValue("executor.queued");
    }

    // =========================
    // 🔥 HTTP Metrics
    // =========================
    public Double getHttpServerRequests() {
        return getGaugeValue("http.server.requests");
    }

    public Double getHttpClientRequests() {
        return getGaugeValue("http.client.requests");
    }

    // =========================
    // 🔥 Database Metrics (HikariCP)
    // =========================
    public Double getHikariConnectionsActive() {
        return getGaugeValue("hikaricp.connections.active");
    }

    public Double getHikariConnectionsIdle() {
        return getGaugeValue("hikaricp.connections.idle");
    }

    public Double getHikariConnectionsPending() {
        return getGaugeValue("hikaricp.connections.pending");
    }

    public Double getHikariConnectionsTotal() {
        return getGaugeValue("hikaricp.connections");
    }

    public Map<String, Object> getAllMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        String[] metricNames = {
                // CPU
                "system.cpu.usage",
                "system.cpu.count",
                "process.cpu.usage",
                "process.cpu.time",

                // System Memory
                "system.memory.total",
                "system.memory.free",

                // JVM Memory
                "jvm.memory.used",
                "jvm.memory.committed",
                "jvm.memory.max",
                "jvm.buffer.memory.used",
                "jvm.buffer.count",

                // Process Memory
                "process.memory.usage",
                "process.memory.virtual",

                // Threads
                "jvm.threads.live",
                "jvm.threads.daemon",
                "jvm.threads.peak",

                // GC
                "jvm.gc.pause.count",
                "jvm.gc.pause.totalTime",

                // Disk
                "disk.total",
                "disk.free",

                // IO / File Handles
                "process.files.open",
                "process.files.max",

                // Uptime
                "process.start.time",
                "process.uptime",

                // Executor (if available)
                "executor.pool.size",
                "executor.active",
                "executor.completed",
                "executor.queued",

                // HTTP metrics
                "http.server.requests",

                // Database metrics (HikariCP)
                "hikaricp.connections",
                "hikaricp.connections.active",
                "hikaricp.connections.idle",
                "hikaricp.connections.pending"
        };

        for (String name : metricNames) {
            Double value = getGaugeValue(name);
            if (value != null) {
                metrics.put(name, value);
            }
        }

        metrics.put("uploads.images.today", imageService.getTodayImageCount());

        return metrics;
    }
}
