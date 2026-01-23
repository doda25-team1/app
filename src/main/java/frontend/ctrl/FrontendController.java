package frontend.ctrl;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import frontend.data.Sms;
import frontend.metrics.MetricsCollector;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/sms")
public class FrontendController {

    private static final Logger log = LoggerFactory.getLogger(FrontendController.class);

    private String modelHost;

    private RestTemplateBuilder rest;

    private MetricsCollector metricsCollector;

    private String appVersion;

    public FrontendController(RestTemplateBuilder rest, Environment env, MetricsCollector metricsCollector) {
        this.rest = rest;
        this.modelHost = env.getProperty("MODEL_HOST");
        this.metricsCollector = metricsCollector;
        this.appVersion = env.getProperty("APP_VERSION", "v1").trim();
        this.metricsCollector.setVariantLabel(this.appVersion);
        assertModelHost();
    }

    private void assertModelHost() {
        if (modelHost == null || modelHost.strip().isEmpty()) {
            log.error("ENV variable MODEL_HOST is null or empty");
            System.exit(1);
        }
        modelHost = modelHost.strip();
        if (modelHost.indexOf("://") == -1) {
            log.error("ENV variable MODEL_HOST is missing protocol, like \"http://...\" (was: \"{}\")", modelHost);
            System.exit(1);
        } else {
            log.info("Working with MODEL_HOST=\"{}\"", modelHost);
        }
    }

    @GetMapping("")
    public String redirectToSlash(HttpServletRequest request) {
        // relative REST requests in JS will end up on / and not on /sms
        return "redirect:" + request.getRequestURI() + "/";
    }

    @GetMapping(path = "/metrics", produces = "text/plain; version=0.0.4")
    @ResponseBody
    public String metrics() {
        long startTime = System.currentTimeMillis();
        metricsCollector.incrementActiveRequests();

        try {
            String metricsOutput = metricsCollector.generateMetrics();
            metricsCollector.incrementRequestCounter("/sms/metrics", 200);
            return metricsOutput;
        } finally {
            metricsCollector.decrementActiveRequests();
            long duration = System.currentTimeMillis() - startTime;
            metricsCollector.recordResponseDuration(duration);
        }
    }

    @GetMapping("/")
    public String index(Model m) {
        long startTime = System.currentTimeMillis();
        metricsCollector.incrementActiveRequests();

        try {
            m.addAttribute("hostname", modelHost);
            m.addAttribute("appVersion", appVersion);
            System.out.printf("App version: %s\n", appVersion);
            metricsCollector.incrementRequestCounter("/sms/", 200);
            return "sms/index";
        } finally {
            metricsCollector.decrementActiveRequests();
            long duration = System.currentTimeMillis() - startTime;
            metricsCollector.recordResponseDuration(duration);
        }
    }

    @PostMapping({ "", "/" })
    @ResponseBody
    public Sms predict(@RequestBody Sms sms) {
        long startTime = System.currentTimeMillis();
        metricsCollector.incrementActiveRequests();
        int statusCode = 200;

        try {
            log.info("Requesting prediction for \"{}\" ...", sms.sms);
            sms.result = getPrediction(sms);
            log.info("Prediction: {}", sms.result);
            return sms;
        } catch (Exception e) {
            statusCode = 500;
            throw e;
        } finally {
            metricsCollector.decrementActiveRequests();
            metricsCollector.incrementRequestCounter("/sms/predict", statusCode);
            metricsCollector.incrementPredictCounter(statusCode);
            long duration = System.currentTimeMillis() - startTime;
            metricsCollector.recordResponseDuration(duration);
        }
    }

    private String getPrediction(Sms sms) {
        try {
            System.out.printf("App version: %s\n", appVersion);
            var url = new URI(modelHost + "/predict");
            var c = rest.build().postForEntity(url, sms, Sms.class);
            return c.getBody().result.trim();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
