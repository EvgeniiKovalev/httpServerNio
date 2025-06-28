package http.server.metrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.prometheus.PrometheusMeterRegistry;

/**
 * router metrics, for query statistics
 */
public class RouterMetrics {
    private final Counter routedRequests;
    private final DistributionSummary requestSize;
    private final PrometheusMeterRegistry registry;

    public RouterMetrics(PrometheusMeterRegistry registry) {
        this.registry = registry;
        this.routedRequests = registry.counter("router.requests.total");
        this.requestSize = registry.summary("router.request.size");
    }

    /**
     * count all requests
     */
    public void countRoutedRequest() {
        routedRequests.increment();
    }

    /**
     * count requests on a specific path (tagged)
     * @param path  - specific path
     */
    public void countRoutedRequest(String path) {
        Counter.builder("router.requests.by.path")
                .tag("path", path)
                .register(registry)
                .increment();
    }

    public void recordRequestSize(int bytes) {
        requestSize.record(bytes);
    }
}