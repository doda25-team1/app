package frontend.ctrl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Health check endpoints for Kubernetes liveness and readiness probes.
 *
 * /health - Basic liveness check (always returns 200 if app is running)
 * /ready  - Readiness check (verifies MODEL_HOST is reachable)
 */
@RestController
public class HealthController {

    private String modelHost;
    private RestTemplate restTemplate;

    public HealthController(RestTemplateBuilder rest, Environment env) {
        this.restTemplate = rest.build();
        this.modelHost = env.getProperty("MODEL_HOST");
    }

    /**
     * Liveness probe endpoint.
     * Returns 200 OK if the application is running.
     * This is a simple check that doesn't validate dependencies.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "app-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Readiness probe endpoint.
     * Returns 200 OK if the application is ready to serve traffic.
     * Validates that MODEL_HOST (dependency) is reachable.
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        Map<String, String> response = new HashMap<>();
        response.put("service", "app-service");

        // Check if MODEL_HOST is configured
        if (modelHost == null || modelHost.strip().isEmpty()) {
            response.put("status", "NOT_READY");
            response.put("reason", "MODEL_HOST not configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        // Try to reach the model service health endpoint
        try {
            String healthUrl = modelHost + "/health";
            ResponseEntity<String> modelResponse = restTemplate.getForEntity(healthUrl, String.class);

            if (modelResponse.getStatusCode().is2xxSuccessful()) {
                response.put("status", "READY");
                response.put("modelService", "reachable");
                response.put("modelHost", modelHost);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_READY");
                response.put("modelService", "unhealthy");
                response.put("modelHost", modelHost);
                response.put("modelStatus", modelResponse.getStatusCode().toString());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
        } catch (Exception e) {
            response.put("status", "NOT_READY");
            response.put("modelService", "unreachable");
            response.put("modelHost", modelHost);
            response.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}
