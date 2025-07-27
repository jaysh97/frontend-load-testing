Frontend Performance Load Testing Project
This project exemplifies a basic approach to performance testing a web frontend using a custom Java-based traffic simulator. It simulates concurrent user traffic to a simple HTML page and collects key performance metrics like response times and throughput.

Project Structure
README.md: This file, providing an overview and instructions.

front-page.html: A simple static HTML file that serves as our "frontend" target for load testing.

src/main/java/TrafficSimulator.java: The core Java application responsible for generating HTTP traffic and logging performance metrics.

traffic_simulator_errors.log (generated on run): A log file that will record details of any failed HTTP requests during the simulation.

Prerequisites
To run this project, you'll need:

Java Development Kit (JDK) 11 or higher: Required to compile and run the TrafficSimulator.java.
*

Python 3.x: Used to easily serve the front-page.html locally for testing.
*



Licensed by Google
Setup and Running the Project
Follow these steps to get the performance test up and running:

1. Serve the Frontend HTML Page
Your TrafficSimulator needs a web server to send requests to. We'll use Python's built-in simple HTTP server for this.

Open your terminal or command prompt.

Navigate to the root directory of this project (where front-page.html is located).

Run the Python HTTP server:

python -m http.server 8000

This will make your front-page.html accessible at http://localhost:8000/front-page.html. Keep this terminal window open as long as you want the server to run.

2. Compile the Java Traffic Simulator
Next, compile the Java source code.

Open a new terminal or command prompt (separate from the one running the Python server).

Navigate to the root directory of this project.

Compile the Java file:

javac src/main/java/TrafficSimulator.java

This command compiles the TrafficSimulator.java file. If successful, it will generate a TrafficSimulator.class file in the src/main/java/ directory.

3. Run the Traffic Simulation
Now you can execute the compiled Java application to simulate traffic.

In the same terminal where you compiled the Java file, run the simulator. You can pass command-line arguments to configure the test:

java -cp src/main/java TrafficSimulator [TARGET_URL] [NUMBER_OF_REQUESTS] [CONCURRENT_USERS] [REQUEST_INTERVAL_MS]

[TARGET_URL] (e.g., http://localhost:8000/front-page.html): The URL of the page you want to test.

[NUMBER_OF_REQUESTS] (e.g., 500): The total number of HTTP requests to send.

[CONCURRENT_USERS] (e.g., 20): The number of concurrent threads (simulated users) sending requests.

[REQUEST_INTERVAL_MS] (e.g., 100): The delay in milliseconds between requests initiated by each individual concurrent user thread.

Example Run:

java -cp src/main/java TrafficSimulator http://localhost:8000/front-page.html 1000 50 20

This command will send 1000 requests to the specified URL, simulating 50 concurrent users, with each user making a request every 20 milliseconds on average.

If you run the command without any arguments, the simulator will use its default values:

java -cp src/main/java TrafficSimulator

(Default: http://localhost:8000/front-page.html, 100 requests, 10 concurrent users, 100ms interval).

Understanding the Performance Metrics
As the simulation runs, you'll see real-time output in your terminal, showing the status and response time for each request. Once the simulation completes, a summary of key performance metrics will be printed to the console:

Total requests attempted: The total number of HTTP requests the simulator tried to send.

Completed requests: The number of requests that finished (either successfully or with an error).

Successful requests (HTTP 2xx): Requests that received an HTTP status code in the 200 range (e.g., 200 OK). This indicates the server processed the request as expected.

Failed requests (non-2xx or exception): Requests that returned a non-2xx status code (e.g., 404 Not Found, 500 Internal Server Error) or resulted in a network/IO exception.

Total duration: The total time the entire simulation took to complete in milliseconds.

Average successful response time: The arithmetic mean of response times for all successful requests.

Median (P50) response time: The 50th percentile response time. Half of your successful requests were faster than this value.

90th Percentile (P90) response time: 90% of your successful requests were faster than this value. This is a crucial metric as it helps identify outliers and gives a better indication of user experience under load than just the average.

95th Percentile (P95) response time: 95% of your successful requests were faster than this value, providing an even stricter view of response time consistency.

Overall Throughput: The rate at which requests were completed, measured in requests per second. This indicates the capacity of your frontend/server under the simulated load.

Error Logging
Any failed requests (non-2xx status codes or exceptions) will be logged to the traffic_simulator_errors.log file in the project's root directory. Reviewing this file can help diagnose issues or identify specific failures during the load test.

This project provides a straightforward, self-contained way to demonstrate your understanding of performance testing principles using a programmatic approach in Java.
