# SMS Checker / Frontend

The frontend allows users to interact with the model in the backend through a web-based UI.

The frontend is implemented with Spring Boot and only consists of a website and one REST endpoint.
It **requires Java 25+** to run (tested with 25.0.1).
Any classification requests will be delegated to the `backend` service that serves the model.
You must specify the environment variable `MODEL_HOST` to define where the backend is running.
Set up a `.env` file based on `.env.example` so the application can pick up these variables.

The frontend service can be started through running the `Main` class (e.g., in your IDE) or through Maven (recommended):

    MODEL_HOST="http://localhost:8081" mvn spring-boot:run

The server runs on port 8080. Once its startup has finished, you can access [localhost:8080/sms](http://localhost:8080/sms) in your browser to interact with the application.

To run locally with Java and Maven (without Docker):

```
mvn -s settings.xml -U clean install
set MODEL_HOST=http://localhost:8081 && mvn spring-boot:run
```

To build and test locally with Docker (user your GitHub username and PAT):

```
docker build --build-arg GITHUB_ACTOR=MyUserName --build-arg GITHUB_TOKEN=3t8rj13912r129r192rr2 -t my-app .
```

## Metrics

Implemented a custom `/metrics` endpoint for the SMS Spam Checker application that exposes metrics formatted for Prometheus (without external libraries).

### 1. Counter: `app_http_requests_total`
Tracks total number of HTTP requests with labels:
- `endpoint`: The request path (e.g., "/sms/", "/sms/predict", "/sms/metrics")
- `status`: HTTP status code (e.g., "200", "500")

**Example:**
```
# HELP app_http_requests_total Total number of HTTP requests by endpoint and status code
# TYPE app_http_requests_total counter
app_http_requests_total{endpoint="/sms/",status="200"} 42
app_http_requests_total{endpoint="/sms/predict",status="200"} 38
app_http_requests_total{endpoint="/sms/metrics",status="200"} 5
```

### 2. Gauge: `app_active_requests`
Tracks current number of requests being processed concurrently.

**Example:**
```
# HELP app_active_requests Current number of requests being processed
# TYPE app_active_requests gauge
app_active_requests 3
```

### 3. Histogram: `app_http_request_duration_seconds`
Tracks request latency distribution with buckets: 0.005s, 0.01s, 0.025s, 0.05s, 0.1s, 0.25s, 0.5s, 1s, 2.5s, 5s, +Inf

**Example:**
```
# HELP app_http_request_duration_seconds HTTP request latency in seconds
# TYPE app_http_request_duration_seconds histogram
app_http_request_duration_seconds_bucket{le="0.005"} 10
app_http_request_duration_seconds_bucket{le="0.01"} 25
app_http_request_duration_seconds_bucket{le="0.025"} 35
app_http_request_duration_seconds_bucket{le="0.05"} 40
app_http_request_duration_seconds_bucket{le="0.1"} 42
app_http_request_duration_seconds_bucket{le="0.25"} 42
app_http_request_duration_seconds_bucket{le="0.5"} 42
app_http_request_duration_seconds_bucket{le="1.0"} 42
app_http_request_duration_seconds_bucket{le="2.5"} 42
app_http_request_duration_seconds_bucket{le="5.0"} 42
app_http_request_duration_seconds_bucket{le="+Inf"} 42
app_http_request_duration_seconds_sum 1.234
app_http_request_duration_seconds_count 42
```

## How to Test (locally)

### 1. Build the Application
```bash
cd app/
mvn -s settings.xml clean install
```

### 2. Run Locally (requires model-service running)
```bash
MODEL_HOST="http://localhost:8081" mvn spring-boot:run
```

### 3. Access the Metrics Endpoint
```bash
curl http://localhost:8080/sms/metrics
```

### 4. View Updated Metrics
```bash
curl http://localhost:8080/sms/metrics
```

