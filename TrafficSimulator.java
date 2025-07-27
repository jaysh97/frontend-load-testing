import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TrafficSimulator {

    // Default configuration values
    private static int NUMBER_OF_REQUESTS = 100;
    private static int CONCURRENT_USERS = 10;
    private static int REQUEST_INTERVAL_MS = 100; // Delay between requests from a single "user" thread (ms)
    private static final String ERROR_LOG_FILE = "traffic_simulator_errors.log";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final AtomicInteger completedRequests = new AtomicInteger(0);
    private static final AtomicInteger successfulRequests = new AtomicInteger(0);
    private static final AtomicInteger failedRequests = new AtomicInteger(0);
    private static final List<Long> successfulResponseTimes = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, List<Long>> endpointResponseTimes = new ConcurrentHashMap<>(); // Track response times per endpoint
    private static PrintWriter errorLogger;

    // --- NEW: Request Configuration and Scenario Definition ---
    public static class RequestConfig {
        String url;
        String method; // GET, POST, PUT, DELETE
        Map<String, String> headers;
        String requestBody; // For POST/PUT requests

        public RequestConfig(String url, String method) {
            this.url = url;
            this.method = method;
            this.headers = new HashMap<>();
        }

        public RequestConfig(String url, String method, Map<String, String> headers, String requestBody) {
            this.url = url;
            this.method = method;
            this.headers = headers != null ? headers : new HashMap<>();
            this.requestBody = requestBody;
        }

        // Builder pattern for easier configuration
        public static RequestConfig get(String url) {
            return new RequestConfig(url, "GET");
        }
        public static RequestConfig post(String url, String body) {
            return new RequestConfig(url, "POST", Map.of("Content-Type", "application/json"), body);
        }
        public static RequestConfig put(String url, String body) {
            return new RequestConfig(url, "PUT", Map.of("Content-Type", "application/json"), body);
        }
        public static RequestConfig delete(String url) {
            return new RequestConfig(url, "DELETE");
        }
        public RequestConfig withHeader(String name, String value) {
            this.headers.put(name, value);
            return this;
        }
        public RequestConfig withBody(String body) {
            this.requestBody = body;
            return this;
        }
    }

    // Define a simple scenario as a list of RequestConfig objects
    // Each simulated user will execute this sequence of requests.
    private static final List<RequestConfig> TEST_SCENARIO = List.of(
            RequestConfig.get("http://localhost:8000/front-page.html"),
            RequestConfig.post("http://localhost:8000/api/login", "{\"username\": \"testuser\", \"password\": \"password123\"}")
                        .withHeader("Accept", "application/json"),
            RequestConfig.get("http://localhost:8000/api/products?category=electronics")
    );
    // You'll need a backend (e.g., Node.js or Spring Boot) to respond to /api/login and /api/products
    // For a simple front-page only test, keep only the first GET request.
    // Example for front-page only:
    // private static final List<RequestConfig> TEST_SCENARIO = List.of(
    //         RequestConfig.get("http://localhost:8000/front-page.html")
    // );
    // --- END NEW ---


    public static void main(String[] args) {
        // Parse command-line arguments (NUMBER_OF_REQUESTS, CONCURRENT_USERS, REQUEST_INTERVAL_MS)
        if (args.length == 3) {
            NUMBER_OF_REQUESTS = Integer.parseInt(args[0]);
            CONCURRENT_USERS = Integer.parseInt(args[1]);
            REQUEST_INTERVAL_MS = Integer.parseInt(args[2]);
        } else if (args.length != 0) {
            System.out.println("Usage: java TrafficSimulator [NUMBER_OF_REQUESTS] [CONCURRENT_USERS] [REQUEST_INTERVAL_MS]");
            System.out.println("Using default values or scenario if no arguments or incorrect number of arguments provided.");
        }

        try {
            errorLogger = new PrintWriter(new FileWriter(ERROR_LOG_FILE, true)); // Append to file
        } catch (IOException e) {
            System.err.println("Could not open error log file: " + e.getMessage());
            return; // Exit if logging fails
        }

        System.out.println("Starting Java Traffic Simulator...");
        System.out.println("Concurrent Users: " + CONCURRENT_USERS);
        System.out.println("Total Requests to Send: " + NUMBER_OF_REQUESTS);
        System.out.println("Request Interval per user: " + REQUEST_INTERVAL_MS + " ms");
        System.out.println("Error logs will be written to: " + ERROR_LOG_FILE);
        System.out.println("Defined Scenario Steps:");
        TEST_SCENARIO.forEach(config ->
            System.out.println("  - " + config.method + " " + config.url +
                               (config.requestBody != null ? " (with body)" : "") +
                               (config.headers.isEmpty() ? "" : " (headers: " + config.headers.keySet() + ")"))
        );
        System.out.println("--------------------------------------------------");

        long startTime = System.currentTimeMillis();

        // Use a scheduled thread pool for concurrent users, each running the scenario
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(CONCURRENT_USERS);

        // Schedule 'NUMBER_OF_REQUESTS' total executions of the scenario.
        // The effective total number of requests will be NUMBER_OF_REQUESTS * TEST_SCENARIO.size()
        for (int i = 0; i < NUMBER_OF_REQUESTS; i++) {
            final int userSessionId = i + 1; // Unique ID for each simulated user session
            scheduler.schedule(() -> {
                runUserScenario(userSessionId);
            }, (long) (i * REQUEST_INTERVAL_MS / (double)CONCURRENT_USERS), TimeUnit.MILLISECONDS);
        }

        scheduler.shutdown();

        try {
            boolean finished = scheduler.awaitTermination(300, TimeUnit.SECONDS); // Increased max wait time for complex scenarios
            if (!finished) {
                System.err.println("Warning: Not all scenarios completed within the timeout period.");
                errorLogger.println("Warning: Not all scenarios completed within the timeout period.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Traffic simulation was interrupted: " + e.getMessage());
            errorLogger.println("Traffic simulation was interrupted: " + e.getMessage());
        } finally {
            errorLogger.close(); // Ensure the error log file is closed
        }

        long endTime = System.currentTimeMillis();
        long totalDurationMs = endTime - startTime;

        System.out.println("\n--------------------------------------------------");
        System.out.println("Traffic Simulation Finished!");
        System.out.println("Total user sessions simulated: " + NUMBER_OF_REQUESTS);
        System.out.println("Total HTTP requests attempted: " + (NUMBER_OF_REQUESTS * TEST_SCENARIO.size()));
        System.out.println("Completed HTTP requests: " + completedRequests.get());
        System.out.println("Successful HTTP requests (HTTP 2xx): " + successfulRequests.get());
        System.out.println("Failed HTTP requests (non-2xx or exception): " + failedRequests.get());
        System.out.println("Total simulation duration: " + totalDurationMs + " ms");

        if (successfulRequests.get() > 0) {
            // Overall metrics
            Collections.sort(successfulResponseTimes);
            double averageResponseTime = (double) successfulResponseTimes.stream().mapToLong(Long::longValue).sum() / successfulRequests.get();
            System.out.printf("Overall Average successful response time: %.2f ms\n", averageResponseTime);
            System.out.printf("Overall Median (P50) response time: %.2f ms\n", calculatePercentile(successfulResponseTimes, 50.0));
            System.out.printf("Overall 90th Percentile (P90) response time: %.2f ms\n", calculatePercentile(successfulResponseTimes, 90.0));
            System.out.printf("Overall 95th Percentile (P95) response time: %.2f ms\n", calculatePercentile(successfulResponseTimes, 95.0));
        } else {
            System.out.println("No successful HTTP requests to calculate overall average and percentile response times.");
        }

        // Metrics per endpoint
        System.out.println("\n--- Metrics Per Endpoint ---");
        endpointResponseTimes.forEach((endpoint, times) -> {
            if (!times.isEmpty()) {
                Collections.sort(times);
                double avg = (double) times.stream().mapToLong(Long::longValue).sum() / times.size();
                System.out.printf("  %s (Count: %d)\n", endpoint, times.size());
                System.out.printf("    Avg: %.2f ms, P50: %.2f ms, P90: %.2f ms, P95: %.2f ms\n",
                        avg, calculatePercentile(times, 50.0), calculatePercentile(times, 90.0), calculatePercentile(times, 95.0));
            }
        });


        double requestsPerSecond = (double) completedRequests.get() / (totalDurationMs / 1000.0);
        System.out.printf("Overall Throughput: %.2f requests/second\n", requestsPerSecond);
        System.out.println("--------------------------------------------------");
    }

    private static void runUserScenario(int userSessionId) {
        for (int i = 0; i < TEST_SCENARIO.size(); i++) {
            RequestConfig config = TEST_SCENARIO.get(i);
            sendRequest(userSessionId, i + 1, config, 0); // 0 retries initially
        }
    }

    // --- NEW: Send Request with Retries ---
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 100; // 100ms

    private static void sendRequest(int userSessionId, int requestInScenarioNum, RequestConfig config, int retryCount) {
        long startTime = System.nanoTime();
        String uniqueRequestId = "User-" + userSessionId + "-Req-" + requestInScenarioNum + "-Retry-" + retryCount;

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(config.url))
                    .timeout(Duration.ofSeconds(15)); // Increased timeout for potentially slower operations

            // Set HTTP Method and Body
            switch (config.method.toUpperCase()) {
                case "POST":
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(config.requestBody != null ? config.requestBody : ""));
                    break;
                case "PUT":
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(config.requestBody != null ? config.requestBody : ""));
                    break;
                case "DELETE":
                    requestBuilder.DELETE();
                    break;
                case "GET":
                default:
                    requestBuilder.GET();
                    break;
            }

            // Add Headers
            config.headers.forEach(requestBuilder::header);

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            long endTime = System.nanoTime();
            long responseTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            int statusCode = response.statusCode();
            String statusMessage = (statusCode >= 200 && statusCode < 300) ? "SUCCESS" : "FAILED";

            System.out.printf("[%s] %s %s - Status: %d (%s), Time: %d ms%s\n",
                    uniqueRequestId, config.method, config.url, statusCode, statusMessage, responseTimeMs,
                    (retryCount > 0 ? " (Retried)" : ""));

            completedRequests.incrementAndGet();
            if (statusCode >= 200 && statusCode < 300) {
                successfulRequests.incrementAndGet();
                successfulResponseTimes.add(responseTimeMs);
                endpointResponseTimes.computeIfAbsent(config.url, k -> Collections.synchronizedList(new ArrayList<>())).add(responseTimeMs);
            } else {
                // If not successful and retries left, try again with exponential backoff
                if (retryCount < MAX_RETRIES) {
                    long delay = INITIAL_RETRY_DELAY_MS * (1L << retryCount); // Exponential backoff
                    System.out.printf("[%s] Retrying in %d ms...\n", uniqueRequestId, delay);
                    try {
                        Thread.sleep(delay); // Introduce a delay before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.err.printf("[%s] Retry sleep interrupted.\n", uniqueRequestId);
                    }
                    sendRequest(userSessionId, requestInScenarioNum, config, retryCount + 1);
                } else {
                    failedRequests.incrementAndGet();
                    errorLogger.printf("[%s] FAILED (Status: %d, Max Retries Reached): %s\n",
                            uniqueRequestId, statusCode, response.body());
                }
            }

        } catch (IOException | InterruptedException e) {
            long endTime = System.nanoTime();
            long responseTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            if (retryCount < MAX_RETRIES) {
                long delay = INITIAL_RETRY_DELAY_MS * (1L << retryCount); // Exponential backoff
                System.err.printf("[%s] Error: %s, Time: %d ms. Retrying in %d ms...\n",
                        uniqueRequestId, e.getMessage(), responseTimeMs, delay);
                errorLogger.printf("[%s] ERROR: %s, Time: %d ms. Retrying in %d ms...\n",
                        uniqueRequestId, e.getMessage(), responseTimeMs, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.err.printf("[%s] Retry sleep interrupted.\n", uniqueRequestId);
                }
                sendRequest(userSessionId, requestInScenarioNum, config, retryCount + 1);
            } else {
                System.err.printf("[%s] Error: %s, Time: %d ms (Max Retries Reached)\n",
                        uniqueRequestId, e.getMessage(), responseTimeMs);
                errorLogger.printf("[%s] ERROR: %s, Time: %d ms (Max Retries Reached)\n",
                        uniqueRequestId, e.getMessage(), responseTimeMs);
                completedRequests.incrementAndGet();
                failedRequests.incrementAndGet();
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
            }
        }
    }
    // --- END NEW ---


    /**
     * Calculates the specified percentile from a list of sorted latencies.
     *
     * @param latencies The list of response times in milliseconds, assumed to be sorted.
     * @param percentile The percentile to calculate (e.g., 90.0 for 90th percentile).
     * @return The calculated percentile value.
     */
    private static double calculatePercentile(List<Long> latencies, double percentile) {
        if (latencies.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * latencies.size()) - 1;
        if (index < 0) { // Handle case where percentile is very small and rounds down
            index = 0;
        } else if (index >= latencies.size()) { // Handle case where percentile is 100 or higher
            index = latencies.size() - 1;
        }
        return latencies.get(index);
    }
}
