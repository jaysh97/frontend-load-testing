import java.util.HashMap;
import java.util.Map;

/**
 * Represents the configuration for a single HTTP request in the performance test scenario.
 * This class allows defining the URL, HTTP method, headers, and request body for each step
 * of a simulated user journey.
 */
public class RequestConfig {
    public String url;
    public String method; // GET, POST, PUT, DELETE
    public Map<String, String> headers;
    public String requestBody; // For POST/PUT requests

    /**
     * Constructs a RequestConfig with a URL and method. Headers are initialized as an empty HashMap.
     * @param url The target URL for the request.
     * @param method The HTTP method (e.g., "GET", "POST").
     */
    public RequestConfig(String url, String method) {
        this.url = url;
        this.method = method;
        this.headers = new HashMap<>();
    }

    /**
     * Constructs a RequestConfig with a URL, method, initial headers, and a request body.
     * @param url The target URL for the request.
     * @param method The HTTP method.
     * @param headers A map of HTTP headers.
     * @param requestBody The request body string (e.g., JSON payload).
     */
    public RequestConfig(String url, String method, Map<String, String> headers, String requestBody) {
        this.url = url;
        this.method = method;
        this.headers = headers != null ? headers : new HashMap<>();
        this.requestBody = requestBody;
    }

    // --- Builder pattern for easier configuration ---

    /**
     * Creates a RequestConfig for a GET request.
     * @param url The target URL.
     * @return A new RequestConfig instance.
     */
    public static RequestConfig get(String url) {
        return new RequestConfig(url, "GET");
    }

    /**
     * Creates a RequestConfig for a POST request with a JSON body.
     * Automatically sets "Content-Type" to "application/json".
     * @param url The target URL.
     * @param body The request body string.
     * @return A new RequestConfig instance.
     */
    public static RequestConfig post(String url, String body) {
        return new RequestConfig(url, "POST", Map.of("Content-Type", "application/json"), body);
    }

    /**
     * Creates a RequestConfig for a PUT request with a JSON body.
     * Automatically sets "Content-Type" to "application/json".
     * @param url The target URL.
     * @param body The request body string.
     * @return A new RequestConfig instance.
     */
    public static RequestConfig put(String url, String body) {
        return new RequestConfig(url, "PUT", Map.of("Content-Type", "application/json"), body);
    }

    /**
     * Creates a RequestConfig for a DELETE request.
     * @param url The target URL.
     * @return A new RequestConfig instance.
     */
    public static RequestConfig delete(String url) {
        return new RequestConfig(url, "DELETE");
    }

    /**
     * Adds an HTTP header to the request.
     * @param name The header name.
     * @param value The header value.
     * @return The current RequestConfig instance for chaining.
     */
    public RequestConfig withHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    /**
     * Sets the request body.
     * @param body The request body string.
     * @return The current RequestConfig instance for chaining.
     */
    public RequestConfig withBody(String body) {
        this.requestBody = body;
        return this;
    }
}
