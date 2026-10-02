import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Task Tracker CLI - roadmap.sh project (Java, no external libraries).
 *
 * Run directly (Java 11+):
 *   java TaskCli.java add "Buy groceries"
 *
 * Or compile first:
 *   javac TaskCli.java
 *   java TaskCli add "Buy groceries"
 *
 * Commands:
 *   add "description"
 *   update <id> "new description"
 *   delete <id>
 *   mark-in-progress <id>
 *   mark-done <id>
 *   list [todo|in-progress|done]
 *
 * Tasks are stored in tasks.json in the current directory.
 */
public class TaskCli {

    private static final Path TASKS_FILE = Paths.get("tasks.json");

    // ---------- Task model ----------

    static class Task {
        int id;
        String description;
        String status;      // todo, in-progress, done
        String createdAt;
        String updatedAt;

        Task(int id, String description, String status, String createdAt, String updatedAt) {
            this.id = id;
            this.description = description;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    // ---------- Entry point ----------

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];

        switch (command) {
            case "add":
                requireArgs(args, 2, "add \"description\"");
                addTask(args[1]);
                break;
            case "update":
                requireArgs(args, 3, "update <id> \"new description\"");
                updateTask(parseId(args[1]), args[2]);
                break;
            case "delete":
                requireArgs(args, 2, "delete <id>");
                deleteTask(parseId(args[1]));
                break;
            case "mark-in-progress":
                requireArgs(args, 2, "mark-in-progress <id>");
                markTask(parseId(args[1]), "in-progress");
                break;
            case "mark-done":
                requireArgs(args, 2, "mark-done <id>");
                markTask(parseId(args[1]), "done");
                break;
            case "list":
                if (args.length == 1) {
                    listTasks(null);
                } else if (args.length == 2 && isValidStatus(args[1])) {
                    listTasks(args[1]);
                } else {
                    fail("Usage: task-cli list [todo|in-progress|done]");
                }
                break;
            default:
                System.out.println("Error: unknown command '" + command + "'.");
                printUsage();
                System.exit(1);
        }
    }

    // ---------- Commands ----------

    private static void addTask(String description) {
        description = description.trim();
        if (description.isEmpty()) {
            fail("Error: description cannot be empty.");
        }
        List<Task> tasks = loadTasks();
        int newId = 1;
        for (Task t : tasks) {
            if (t.id >= newId) newId = t.id + 1;
        }
        String now = now();
        tasks.add(new Task(newId, description, "todo", now, now));
        saveTasks(tasks);
        System.out.println("Task added successfully (ID: " + newId + ")");
    }

    private static void updateTask(int id, String description) {
        description = description.trim();
        if (description.isEmpty()) {
            fail("Error: description cannot be empty.");
        }
        List<Task> tasks = loadTasks();
        Task task = findTask(tasks, id);
        task.description = description;
        task.updatedAt = now();
        saveTasks(tasks);
        System.out.println("Task " + id + " updated successfully");
    }

    private static void deleteTask(int id) {
        List<Task> tasks = loadTasks();
        Task task = findTask(tasks, id);
        tasks.remove(task);
        saveTasks(tasks);
        System.out.println("Task " + id + " deleted successfully");
    }

    private static void markTask(int id, String status) {
        List<Task> tasks = loadTasks();
        Task task = findTask(tasks, id);
        task.status = status;
        task.updatedAt = now();
        saveTasks(tasks);
        System.out.println("Task " + id + " marked as " + status);
    }

    private static void listTasks(String statusFilter) {
        List<Task> tasks = loadTasks();
        boolean found = false;
        for (Task t : tasks) {
            if (statusFilter != null && !t.status.equals(statusFilter)) continue;
            found = true;
            System.out.println("[" + t.id + "] " + t.description);
            System.out.println("     status: " + t.status
                    + " | created: " + t.createdAt
                    + " | updated: " + t.updatedAt);
        }
        if (!found) {
            System.out.println("No tasks found.");
        }
    }

    // ---------- Helpers ----------

    private static String now() {
        return LocalDateTime.now().withNano(0).toString();
    }

    private static boolean isValidStatus(String s) {
        return s.equals("todo") || s.equals("in-progress") || s.equals("done");
    }

    private static int parseId(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            fail("Error: '" + value + "' is not a valid task ID (must be a number).");
            return -1; // unreachable
        }
    }

    private static Task findTask(List<Task> tasks, int id) {
        for (Task t : tasks) {
            if (t.id == id) return t;
        }
        fail("Error: no task found with ID " + id + ".");
        return null; // unreachable
    }

    private static void requireArgs(String[] args, int expected, String usage) {
        if (args.length != expected) {
            fail("Usage: task-cli " + usage);
        }
    }

    private static void fail(String message) {
        System.out.println(message);
        System.exit(1);
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  task-cli add \"description\"");
        System.out.println("  task-cli update <id> \"new description\"");
        System.out.println("  task-cli delete <id>");
        System.out.println("  task-cli mark-in-progress <id>");
        System.out.println("  task-cli mark-done <id>");
        System.out.println("  task-cli list [todo|in-progress|done]");
    }

    // ---------- File I/O ----------

    private static List<Task> loadTasks() {
        if (!Files.exists(TASKS_FILE)) {
            saveTasks(new ArrayList<>());
            return new ArrayList<>();
        }
        String content;
        try {
            content = new String(Files.readAllBytes(TASKS_FILE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("Error: could not read tasks.json: " + e.getMessage());
            return null; // unreachable
        }
        if (content.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return fromJson(content);
        } catch (RuntimeException e) {
            fail("Error: tasks.json is not valid. Fix or delete it and try again.");
            return null; // unreachable
        }
    }

    private static void saveTasks(List<Task> tasks) {
        try {
            Files.write(TASKS_FILE, toJson(tasks).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            fail("Error: could not write tasks.json: " + e.getMessage());
        }
    }

    // ---------- JSON writing ----------

    private static String toJson(List<Task> tasks) {
        if (tasks.isEmpty()) return "[]\n";
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": ").append(t.id).append(",\n");
            sb.append("    \"description\": ").append(quote(t.description)).append(",\n");
            sb.append("    \"status\": ").append(quote(t.status)).append(",\n");
            sb.append("    \"createdAt\": ").append(quote(t.createdAt)).append(",\n");
            sb.append("    \"updatedAt\": ").append(quote(t.updatedAt)).append("\n");
            sb.append("  }");
            if (i < tasks.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append("\"").toString();
    }

    // ---------- JSON reading (minimal parser for our own file format) ----------

    private static List<Task> fromJson(String json) {
        Parser parser = new Parser(json);
        Object root = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.atEnd() || !(root instanceof List)) {
            throw new IllegalArgumentException("Expected a JSON array");
        }
        List<Task> tasks = new ArrayList<>();
        for (Object item : (List<?>) root) {
            if (!(item instanceof Map)) throw new IllegalArgumentException("Expected an object");
            Map<?, ?> m = (Map<?, ?>) item;
            Object id = m.get("id");
            Object description = m.get("description");
            Object status = m.get("status");
            Object createdAt = m.get("createdAt");
            Object updatedAt = m.get("updatedAt");
            if (!(id instanceof Long) || !(description instanceof String)
                    || !(status instanceof String) || !isValidStatus((String) status)
                    || !(createdAt instanceof String) || !(updatedAt instanceof String)) {
                throw new IllegalArgumentException("Invalid task");
            }
            tasks.add(new Task(((Long) id).intValue(), (String) description,
                    (String) status, (String) createdAt, (String) updatedAt));
        }
        return tasks;
    }

    private static class Parser {
        private final String s;
        private int i = 0;

        Parser(String s) {
            this.s = s;
        }

        boolean atEnd() {
            return i >= s.length();
        }

        void skipWhitespace() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        Object parseValue() {
            skipWhitespace();
            if (atEnd()) throw new IllegalArgumentException("Unexpected end");
            char c = s.charAt(i);
            if (c == '[') return parseArray();
            if (c == '{') return parseObject();
            if (c == '"') return parseString();
            if (c == '-' || Character.isDigit(c)) return parseNumber();
            if (s.startsWith("true", i))  { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i))  { i += 4; return null; }
            throw new IllegalArgumentException("Unexpected character at " + i);
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            i++; // [
            skipWhitespace();
            if (peek() == ']') { i++; return list; }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char c = next();
                if (c == ']') return list;
                if (c != ',') throw new IllegalArgumentException("Expected , or ]");
            }
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            i++; // {
            skipWhitespace();
            if (peek() == '}') { i++; return map; }
            while (true) {
                skipWhitespace();
                if (peek() != '"') throw new IllegalArgumentException("Expected key");
                String key = parseString();
                skipWhitespace();
                if (next() != ':') throw new IllegalArgumentException("Expected :");
                map.put(key, parseValue());
                skipWhitespace();
                char c = next();
                if (c == '}') return map;
                if (c != ',') throw new IllegalArgumentException("Expected , or }");
            }
        }

        String parseString() {
            StringBuilder sb = new StringBuilder();
            i++; // opening quote
            while (true) {
                if (atEnd()) throw new IllegalArgumentException("Unterminated string");
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (atEnd()) throw new IllegalArgumentException("Bad escape");
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"':  sb.append('"');  break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/');  break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'u':
                            if (i + 4 > s.length()) throw new IllegalArgumentException("Bad unicode escape");
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                            break;
                        default:
                            throw new IllegalArgumentException("Bad escape");
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        Long parseNumber() {
            int start = i;
            if (s.charAt(i) == '-') i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            return Long.parseLong(s.substring(start, i));
        }

        char peek() {
            if (atEnd()) throw new IllegalArgumentException("Unexpected end");
            return s.charAt(i);
        }

        char next() {
            if (atEnd()) throw new IllegalArgumentException("Unexpected end");
            return s.charAt(i++);
        }
    }
}
