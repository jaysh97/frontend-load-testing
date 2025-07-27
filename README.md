Frontend Performance Load Testing Project
This project exemplifies a sophisticated approach to performance testing a web frontend using a custom Java-based traffic simulator. It simulates concurrent user traffic following defined scenarios to web pages and APIs, collecting key performance metrics like response times, throughput, and error rates, even including retry mechanisms.

Project Structure
README.md: This file, providing an overview and instructions.

front-page.html: A simple static HTML file that serves as our "frontend" target for load testing.

src/main/java/TrafficSimulator.java: The core Java application responsible for generating HTTP traffic and logging detailed performance metrics.

traffic_simulator_errors.log (generated on run): A log file that will record details of any failed HTTP requests during the simulation.

Prerequisites
To run this project, you'll need:

Java Development Kit (JDK) 11 or higher: This is essential to compile and run the TrafficSimulator.java.

Python 3.x: We'll use Python's built-in HTTP server to easily serve front-page.html locally for testing.



Setup and Running the Project
Follow these steps to get your performance test configured and running:

1. Serve the Frontend HTML Page
Your TrafficSimulator needs a live web server to send requests to. For local testing, Python's simple HTTP server is perfect.

Open your terminal or command prompt.

Navigate to the root directory of this project (where front-page.html is located).

Run the Python HTTP server:

python -m http.server 8000

This command makes your front-page.html accessible at http://localhost:8000/front-page.html. Keep this terminal window open for the duration of your testing.

2. Compile the Java Traffic Simulator
Next, you'll compile the Java source code for your simulator.

Open a new terminal or command prompt (this should be separate from the one running the Python server).

Navigate to the root directory of this project.

Compile the Java file:

javac src/main/java/TrafficSimulator.java

Upon successful compilation, TrafficSimulator.class will be generated in the src/main/java/ directory.

3. Run the Traffic Simulation
Now you can execute the compiled Java application to generate and simulate traffic to your frontend.

In the same terminal where you compiled the Java file, run the simulator. You can configure the test by passing command-line arguments:

java -cp src/main/java TrafficSimulator [NUMBER_OF_REQUESTS] [CONCURRENT_USERS] [REQUEST_INTERVAL_MS]

[NUMBER_OF_REQUESTS] (e.g., 500): The total number of user sessions or scenarios to execute. Each session will run through the entire TEST_SCENARIO once.

[CONCURRENT_USERS] (e.g., 20): The number of concurrent threads (simulated users) that will be active simultaneously, executing the TEST_SCENARIO.

[REQUEST_INTERVAL_MS] (e.g., 100): The approximate delay in milliseconds between the start of consecutive user sessions. This helps control the injection rate of new users.

Example Run:

java -cp src/main/java TrafficSimulator 100 10 500

This command will simulate 100 complete user sessions, with 10 concurrent users actively sending requests, and new user sessions starting roughly every 500 milliseconds.

If you run the command without any arguments, the simulator will use its default values:

java -cp src/main/java TrafficSimulator

(Default: 100 user sessions, 10 concurrent users, 100ms interval).

Advanced Performance Testing Dimensions
The TrafficSimulator.java has been significantly enhanced to support more complex and realistic performance testing scenarios:

1. Configurable Request Scenarios (TEST_SCENARIO)
Instead of just hammering a single URL, you can now define a full sequence of requests that mimic a real user's journey through your application. This user journey is configured directly within the TrafficSimulator.java file in the TEST_SCENARIO list.

Each item in TEST_SCENARIO is a RequestConfig object, offering granular control over each step:

URL: The target endpoint for the request (e.g., a web page, an API endpoint).

Method: The HTTP method (GET, POST, PUT, DELETE).

Headers: Custom HTTP headers can be added (e.g., Content-Type, Authorization for authenticated requests).

Request Body: A string representing the request body (e.g., a JSON payload for POST or PUT requests).

Example Scenario (as defined in TrafficSimulator.java):

private static final List<RequestConfig> TEST_SCENARIO = List.of(
    RequestConfig.get("http://localhost:8000/front-page.html"), // Simulate visiting the homepage
    RequestConfig.post("http://localhost:8000/api/login", "{\"username\": \"testuser\", \"password\": \"password123\"}")
                 .withHeader("Accept", "application/json"), // Simulate a user login API call
    RequestConfig.get("http://localhost:8000/api/products?category=electronics") // Simulate Browse products
);

If you use this scenario, you'll need a simple backend server (e.g., built with Node.js, Spring Boot, or a mock API service) to respond to the /api/login and /api/products endpoints. For a project focused solely on the static front-page.html, you might simplify TEST_SCENARIO to only include RequestConfig.get("http://localhost:8000/front-page.html").

2. Robust Error Handling with Exponential Backoff
The simulator now includes a robust retry mechanism. For failed requests (those with non-2xx HTTP status codes or network/IO exceptions), it will automatically attempt to retry the request up to MAX_RETRIES (default is 3 times). Between each retry, it introduces an exponential backoff delay. This significantly enhances the realism of your simulation, as it mimics how a resilient client application would handle transient network issues or temporary server unavailability. It also helps differentiate between intermittent glitches and persistent failures.

3. Detailed Metrics Per Endpoint
Beyond overall performance metrics, the simulator now intelligently collects and reports the average, 50th percentile (median), 90th percentile (P90), and 95th percentile (P95) response times for each unique URL (endpoint) hit during the simulation. This granular breakdown is invaluable for pinpointing performance bottlenecks in specific API calls or pages within your application, helping you to identify exactly where optimization is needed.

4. Enhanced Error Logging
Failed requests, including their HTTP status codes, error messages, and whether they exhausted all retry attempts, are meticulously logged to the traffic_simulator_errors.log file. This detailed error log is essential for post-test analysis, debugging, and understanding the nature of failures under load.

Understanding the Performance Metrics
As the simulation runs, you'll see real-time output in your terminal, providing status and response time for each request. Each log entry includes a unique ID (User-X-Req-Y-Retry-Z). Upon completion, a comprehensive summary of key performance metrics is printed to the console:

Total user sessions simulated: The total number of times the entire TEST_SCENARIO was executed.

Total HTTP requests attempted: The aggregate count of all individual HTTP requests sent across all scenario executions.

Completed HTTP requests: The number of requests that completed their transaction, whether successfully or with an error (including retries).

Successful HTTP requests (HTTP 2xx): Requests that received an HTTP status code in the 200 range (e.g., 200 OK, 201 Created). This signifies the server processed the request as expected.

Failed HTTP requests (non-2xx or exception): Requests that returned a non-2xx status code (e.g., 404 Not Found, 500 Internal Server Error) or encountered a network/IO exception after all retry attempts were exhausted.

Total simulation duration: The total time the entire load test took to complete, measured in milliseconds.

Overall Average successful response time: The arithmetic mean of response times for all successful HTTP requests across all endpoints.

Overall Percentiles (P50, P90, P95) response time: These statistical measures provide insight into response time consistency. For example, the P95 (95th percentile) means 95% of your successful requests completed within that specified time. This is a critical metric for understanding user experience, as it highlights potential "long tail" performance issues.

Metrics Per Endpoint: This section provides granular average and percentile response times for each distinct URL/endpoint hit in your TEST_SCENARIO. This helps you pinpoint performance issues to specific parts of your application.

Overall Throughput: The rate at which requests were completed, measured in requests per second. This directly indicates the capacity of your frontend and its underlying server infrastructure under the simulated load.

Potential Enhancements and Next Steps
To push this project further and showcase even more advanced performance engineering skills, consider these additions:

1. Deeper TrafficSimulator.java Enhancements
Parameterized Test Data: Move test data (like usernames, passwords, product IDs) out of the code and into external files (e.g., CSV). Implement logic to read and inject this dynamic data into request bodies and URLs for highly realistic and varied simulations.

Correlation: Implement advanced logic to extract specific data from one HTTP response (e.g., a session token, an item ID generated by the server) and dynamically use it in subsequent requests within the same user session. This is vital for testing multi-step workflows like e-commerce checkouts or user registrations.

Think Time/Pacing: Introduce realistic "think time" delays between consecutive requests within a user's scenario. This simulates human interaction patterns (e.g., a user pausing to read content before clicking a link) and prevents an artificial "machine-gun" load.

Ramp-up/Ramp-down Phases: Instead of an immediate burst of concurrent users, implement a gradual increase (ramp-up) in the number of concurrent users over time, followed by a gradual decrease (ramp-down). This helps in understanding how your system behaves as load incrementally increases and decreases.

Response Content Validation (Assertions): Add assertions to verify that the content of HTTP responses is correct. For example, check if a specific text string is present on a page, or parse JSON responses to confirm data integrity. This validates not just speed, but also correctness under load.

Advanced Reporting: Implement a more robust reporting mechanism. This could involve writing results to a structured format (like JSON or XML) that can be easily consumed by other tools, or even generating basic HTML reports directly from the Java code.

2. Broader Project Additions
Containerization of the Full Stack: Develop a docker-compose.yml file that orchestrates the entire test environment. This would spin up your frontend web server (e.g., Nginx serving front-page.html, or a simple Node.js/Spring Boot backend), and your Java TrafficSimulator in separate, linked Docker containers. This ensures a fully portable, isolated, and reproducible testing environment for anyone.

Performance Dashboard Integration: Integrate your collected performance metrics with a dedicated visualization tool like Grafana. You could use a time-series database like Prometheus or InfluxDB to store your TrafficSimulator's output, allowing you to create dynamic, interactive dashboards for real-time monitoring and post-test analysis.

