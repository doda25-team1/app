package frontend.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/**
 * Thread-safe (I hope :/ ) metrics collector for Prometheus metrics.
 * Keys are formatted as Prometheus labels (endpoint="/api/test", status="200").
 */
@Component
public class MetricsCollector {

    // Counter: Total HTTP requests by endpoint and status
    private final Map<String, AtomicLong> requestCounter = new ConcurrentHashMap<>();

    // Counter: Total predict requests by status (CTA experiment)
    private final Map<String, AtomicLong> predictCounter = new ConcurrentHashMap<>();

    // Gauge: Current number of active requests being processed
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    // Histogram: Response time duration buckets (in seconds)
    private final Map<String, AtomicLong> responseDurationBuckets = new ConcurrentHashMap<>();
    private final AtomicLong responseDurationSum = new AtomicLong(0);
    private final AtomicLong responseDurationCount = new AtomicLong(0);

    // Variant label (e.g., v1, v2) set by the controller; defaults to v1
    private volatile String variantLabel = "v1";

    // Histogram buckets in seconds: 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, +Inf
    // example: 0.06s will add one into the buckets 1.0, 2.5, 5.0, +Inf
    private static final double[] HISTOGRAM_BUCKETS = {0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, Double.POSITIVE_INFINITY};

    public MetricsCollector() {
        // initialize buckets with sizes 0
        for (double bucket : HISTOGRAM_BUCKETS) {
            String key = formatBucket(bucket);
            responseDurationBuckets.put(key, new AtomicLong(0));
        }
    }

    /**
     * Set the variant label (e.g., v1, v2) so metrics are comparable across releases.
     */
    public void setVariantLabel(String variantLabel) {
        if (variantLabel != null && !variantLabel.isBlank()) {
            this.variantLabel = variantLabel.trim();
        }
    }

    /**
     * Increment request counter for a specific endpoint and status code.
     */
    public void incrementRequestCounter(String endpoint, int statusCode) {
        String key = String.format("endpoint=\"%s\",status=\"%d\"", endpoint, statusCode);
        requestCounter.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Increment predict counter for a specific status code (CTA experiment metric).
     */
    public void incrementPredictCounter(int statusCode) {
        String key = String.format("status=\"%d\"", statusCode);
        predictCounter.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Increment active requests gauge (called at request start).
     */
    public void incrementActiveRequests() {
        activeRequests.incrementAndGet();
    }

    /**
     * Decrement active requests gauge (called at request end).
     */
    public void decrementActiveRequests() {
        activeRequests.decrementAndGet();
    }

    /**
     * Record response duration in milliseconds for histogram.
     */
    public void recordResponseDuration(long durationMillis) {
        double durationSeconds = durationMillis / 1000.0;

        // update sum and count
        responseDurationSum.addAndGet(durationMillis);
        responseDurationCount.incrementAndGet();

        // update buckets
        for (double bucket : HISTOGRAM_BUCKETS) {
            if (durationSeconds <= bucket) {
                String key = formatBucket(bucket);
                responseDurationBuckets.get(key).incrementAndGet();
            }
        }
    }

    /**
     * Generate formatted metrics output for Prometheus.
     */
    public String generateMetrics() {
        StringBuilder sb = new StringBuilder();
        String variantLabelFormatted = String.format("variant=\"%s\"", variantLabel);

        // Counter: app_http_requests_total
        sb.append("# HELP app_http_requests_total Total number of HTTP requests by endpoint and status code\n");
        sb.append("# TYPE app_http_requests_total counter\n");
        if (requestCounter.isEmpty()) {
            sb.append(String.format("app_http_requests_total{endpoint=\"none\",status=\"0\",%s} 0\n", variantLabelFormatted));
        } else {
            for (Map.Entry<String, AtomicLong> entry : requestCounter.entrySet()) {
                sb.append(String.format("app_http_requests_total{%s,%s} %d\n", entry.getKey(), variantLabelFormatted, entry.getValue().get()));
            }
        }
        sb.append("\n");

        // Counter: sms_app_requests_total (focused on predict endpoint)
        sb.append("# HELP sms_app_requests_total Total number of SMS predict requests by status code\n");
        sb.append("# TYPE sms_app_requests_total counter\n");
        if (predictCounter.isEmpty()) {
            sb.append(String.format("sms_app_requests_total{status=\"0\",%s} 0\n", variantLabelFormatted));
        } else {
            for (Map.Entry<String, AtomicLong> entry : predictCounter.entrySet()) {
                sb.append(String.format("sms_app_requests_total{%s,%s} %d\n", entry.getKey(), variantLabelFormatted, entry.getValue().get()));
            }
        }
        sb.append("\n");

        // Gauge: app_active_requests
        sb.append("# HELP app_active_requests Current number of requests being processed\n");
        sb.append("# TYPE app_active_requests gauge\n");
        sb.append(String.format("app_active_requests{%s} %d\n", variantLabelFormatted, activeRequests.get()));
        sb.append("\n");

        // Histogram: app_http_request_duration_seconds
        sb.append("# HELP app_http_request_duration_seconds HTTP request latency in seconds\n");
        sb.append("# TYPE app_http_request_duration_seconds histogram\n");
        for (double bucket : HISTOGRAM_BUCKETS) {
            String key = formatBucket(bucket);
            long count = responseDurationBuckets.get(key).get();
            sb.append(String.format("app_http_request_duration_seconds_bucket{le=\"%s\",%s} %d\n", key, variantLabelFormatted, count));
        }
        // Sum and count for histogram
        double sumSeconds = responseDurationSum.get() / 1000.0;
        sb.append(String.format("app_http_request_duration_seconds_sum{%s} %.3f\n", variantLabelFormatted, sumSeconds));
        sb.append(String.format("app_http_request_duration_seconds_count{%s} %d\n", variantLabelFormatted, responseDurationCount.get()));

        return sb.toString();
    }

    private String formatBucket(double bucket) {
        return bucket == Double.POSITIVE_INFINITY ? "inf" : String.valueOf(bucket);
    }
}
