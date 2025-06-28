package http.server.metrics;


import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class JvmMetrics {
    private final MemoryMXBean memoryMXBean;

    public JvmMetrics(PrometheusMeterRegistry registry) {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();

        Gauge.builder("jvm.memory.used", this,  metrics -> metrics.getUsedMemory().doubleValue())
                .description("Used JVM memory in bytes")
                .baseUnit("bytes")
                .register(registry);
    }

    private Number getUsedMemory() {
        return memoryMXBean.getHeapMemoryUsage().getUsed() +
                memoryMXBean.getNonHeapMemoryUsage().getUsed();
    }
}
