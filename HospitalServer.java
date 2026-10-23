import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Educational Smart Hospital Patient Management System.
 * Uses Java collections to demonstrate ArrayList, HashMap, Queue,
 * PriorityQueue, LinkedList, searching and sorting.
 */
public class HospitalServer {
    private static final int PORT = 8080;
    private static final List<Map<String, String>> patients = new ArrayList<>();
    private static final Map<String, Map<String, String>> patientsById = new HashMap<>();
    private static final Queue<Map<String, String>> appointments = new LinkedList<>();
    private static final PriorityQueue<Map<String, String>> emergencies =
        new PriorityQueue<>((a, b) -> Integer.compare(
            Integer.parseInt(b.get("severity")), Integer.parseInt(a.get("severity"))));
    private static final Map<String, LinkedList<String>> medicalHistory = new HashMap<>();
    private static int patientCounter = 1001;
    private static int appointmentCounter = 1;
    private static int emergencyCounter = 1;
    private static final Path PUBLIC_DIR = Paths.get("public").toAbsolutePath().normalize();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/", HospitalServer::handleApi);
        server.createContext("/", HospitalServer::serveStatic);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Smart Hospital is running at http://localhost:" + PORT);
        System.out.println("Press Ctrl+C to stop the server.");
    }

    private static void handleApi(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        Map<String, String> data = readForm(ex);
        synchronized (HospitalServer.class) {
            try {
                if (path.equals("/api/patients") && method.equals("GET")) {
                    sendJson(ex, 200, jsonList(patients));
                } else if (path.equals("/api/patients") && method.equals("POST")) {
                    String name = required(data, "name");
                    String age = required(data, "age");
                    String gender = required(data, "gender");
                    String phone = data.getOrDefault("phone", "");
                    String condition = data.getOrDefault("condition", "Not specified");
                    String id = "P" + patientCounter++;
                    Map<String, String> p = map("id", id, "name", name, "age", age,
                        "gender", gender, "phone", phone, "condition", condition,
                        "createdAt", LocalDateTime.now().toString().replace('T', ' '));
                    patients.add(p);
                    patientsById.put(id, p);
                    medicalHistory.put(id, new LinkedList<>());
                    sendJson(ex, 201, json(p));
                } else if (path.equals("/api/appointments") && method.equals("GET")) {
                    sendJson(ex, 200, jsonList(new ArrayList<>(appointments)));
                } else if (path.equals("/api/appointments") && method.equals("POST")) {
                    String patientId = required(data, "patientId");
                    if (!patientsById.containsKey(patientId)) throw new IllegalArgumentException("Patient ID not found.");
                    Map<String, String> a = map("id", "A" + appointmentCounter++,
                        "patientId", patientId, "patientName", patientsById.get(patientId).get("name"),
                        "doctor", required(data, "doctor"), "date", required(data, "date"),
                        "time", required(data, "time"), "reason", data.getOrDefault("reason", ""));
                    appointments.add(a);
                    addHistory(patientId, "Appointment booked with " + a.get("doctor") + " on " + a.get("date") + " at " + a.get("time"));
                    sendJson(ex, 201, json(a));
                } else if (path.equals("/api/emergencies") && method.equals("GET")) {
                    List<Map<String, String>> list = new ArrayList<>(emergencies);
                    list.sort((a,b) -> Integer.compare(Integer.parseInt(b.get("severity")), Integer.parseInt(a.get("severity"))));
                    sendJson(ex, 200, jsonList(list));
                } else if (path.equals("/api/emergencies") && method.equals("POST")) {
                    String patientId = required(data, "patientId");
                    if (!patientsById.containsKey(patientId)) throw new IllegalArgumentException("Patient ID not found.");
                    String severity = required(data, "severity");
                    int level = Integer.parseInt(severity);
                    if (level < 1 || level > 5) throw new IllegalArgumentException("Severity must be from 1 to 5.");
                    Map<String, String> e = map("id", "E" + emergencyCounter++, "patientId", patientId,
                        "patientName", patientsById.get(patientId).get("name"), "severity", severity,
                        "description", data.getOrDefault("description", ""), "status", "Waiting");
                    emergencies.add(e);
                    addHistory(patientId, "Emergency case registered (severity " + severity + ")");
                    sendJson(ex, 201, json(e));
                } else if (path.equals("/api/history") && method.equals("GET")) {
                    String patientId = queryParam(ex.getRequestURI().getRawQuery(), "patientId");
                    if (patientId == null || patientId.isBlank()) throw new IllegalArgumentException("Provide a patientId.");
                    if (!patientsById.containsKey(patientId)) throw new IllegalArgumentException("Patient ID not found.");
                    List<Map<String, String>> out = new ArrayList<>();
                    for (String item : medicalHistory.getOrDefault(patientId, new LinkedList<>())) out.add(map("entry", item));
                    sendJson(ex, 200, jsonList(out));
                } else if (path.equals("/api/stats") && method.equals("GET")) {
                    sendJson(ex, 200, "{\"patients\":" + patients.size() + ",\"appointments\":" +
                        appointments.size() + ",\"emergencies\":" + emergencies.size() + "}");
                } else {
                    sendJson(ex, 404, "{\"error\":\"Endpoint not found.\"}");
                }
            } catch (IllegalArgumentException err) {
                sendJson(ex, 400, "{\"error\":" + quote(err.getMessage()) + "}");
            } catch (Exception err) {
                sendJson(ex, 500, "{\"error\":\"Server error. Please try again.\"}");
                err.printStackTrace();
            }
        }
    }

    private static void addHistory(String id, String entry) {
        medicalHistory.computeIfAbsent(id, k -> new LinkedList<>()).addFirst(
            LocalDateTime.now().toString().replace('T', ' ') + " — " + entry);
    }

    private static void serveStatic(HttpExchange ex) throws IOException {
        String requested = ex.getRequestURI().getPath();
        if (requested.equals("/")) requested = "/index.html";
        Path file = PUBLIC_DIR.resolve(requested.substring(1)).normalize();
        if (!file.startsWith(PUBLIC_DIR) || !Files.exists(file) || Files.isDirectory(file)) {
            byte[] b = "Not found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
            return;
        }
        String type = file.toString().endsWith(".html") ? "text/html; charset=utf-8" :
            file.toString().endsWith(".css") ? "text/css; charset=utf-8" :
            file.toString().endsWith(".js") ? "application/javascript; charset=utf-8" : "text/plain";
        ex.getResponseHeaders().set("Content-Type", type);
        byte[] bytes = Files.readAllBytes(file);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static Map<String, String> readForm(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> result = new HashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) continue;
            String[] parts = pair.split("=", 2);
            result.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                URLDecoder.decode(parts.length > 1 ? parts[1] : "", StandardCharsets.UTF_8));
        }
        return result;
    }

    private static String queryParam(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] p = pair.split("=", 2);
            if (URLDecoder.decode(p[0], StandardCharsets.UTF_8).equals(key))
                return URLDecoder.decode(p.length > 1 ? p[1] : "", StandardCharsets.UTF_8);
        }
        return null;
    }

    private static String required(Map<String, String> data, String key) {
        String value = data.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Please fill in " + key + ".");
        return value.trim();
    }

    private static Map<String, String> map(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put(pairs[i], pairs[i + 1]);
        return m;
    }

    private static String jsonList(List<Map<String, String>> list) {
        StringJoiner j = new StringJoiner(",", "[", "]");
        for (Map<String, String> m : list) j.add(json(m));
        return j.toString();
    }

    private static String json(Map<String, String> m) {
        StringJoiner j = new StringJoiner(",", "{", "}");
        for (Map.Entry<String, String> e : m.entrySet()) j.add(quote(e.getKey()) + ":" + quote(e.getValue()));
        return j.toString();
    }

    private static String quote(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    private static void sendJson(HttpExchange ex, int status, String body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
