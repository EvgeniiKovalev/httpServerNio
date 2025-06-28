package http.server.metrics;

import http.server.HttpServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;


/**
 * server metrics, for server statistics
 */
public class ServerMetrics {
    private final Timer serverProcessingTime;
    private final PrometheusMeterRegistry metricsRegistry;
    private final Counter totalRequests;
    private final HttpServer httpServer;

    public ServerMetrics(PrometheusMeterRegistry metricsRegistry, HttpServer httpServer) {
        this.metricsRegistry = metricsRegistry;
        this.httpServer = httpServer;
        this.totalRequests = metricsRegistry.counter("http.requests.total");
        this.serverProcessingTime = metricsRegistry.timer("http.processing.time");
        Gauge.builder("server.active_threads", httpServer::activeCountThreads)
                .description("Current active threads")
                .register(metricsRegistry);
    }

    public void incTotalRequest() {
        totalRequests.increment();
    }

    public Timer.Sample startServerProcessingTimer() {
        return Timer.start();
    }

    public void stopServerProcessingTimer(Timer.Sample timer) {
        timer.stop(serverProcessingTime);
    }
}
