package weight_tracker_server2;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;

public class Handle {
    static String insertCalories = "INSERT INTO CALORIES (date, calories) VALUES (?, ?)";
    static String getCalories = "SELECT COALESCE(SUM(calories), 0) FROM CALORIES WHERE date = ?";
    
    static int responseCode;

    public static void main(String[] args) throws Exception {
        // Start an HTTP server on port 8000
    	HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8000), 0);
        server.createContext("/weight", exchange -> handleRequest(exchange));
        server.setExecutor(null); // Creates a default executor
        System.out.println("Server is running on http://localhost:8000");
        server.start();
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        // Allow CORS for all origins
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        // Handle preflight (OPTIONS) requests
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1); // No Content
            return;
        }
        
        responseCode = 200; //Assume OK unless something happens

        // Handle actual requests
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            requestBody.append(line);
        }
        br.close();

        // Decode form data
        HashMap<String, String> in_map = decodeInput(requestBody.toString());

        String response;
        try (Connection con = create_connection()) {
            if (in_map.get("request_type").equals("insert_calories")) {
                response = addCalories(con, in_map.get("date_eaten"), Integer.parseInt(in_map.get("calories")));
            } else if (in_map.get("request_type").equals("get_calories")) {
                response = getCalories(con, in_map.get("date_requested"));
            } else {
            	responseCode = 400;
                response = "Error: Unknown request type";
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseCode = 500;
            response = "Error processing the request";
        }

        // Send response
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(responseCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }


    private static String getCalories(Connection con, String date) throws SQLException {
        PreparedStatement pstmt = con.prepareStatement(getCalories);
        pstmt.setDate(1, Date.valueOf(date));
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            int calories = rs.getInt(1);
            return String.valueOf(calories);
        } else {
            return "0";
        }
    }

    private static String addCalories(Connection con, String date, int amount) throws SQLException {
        PreparedStatement pstmt = con.prepareStatement(insertCalories);
        pstmt.setDate(1, Date.valueOf(date));
        pstmt.setInt(2, amount);

        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0 ) {
        	responseCode = 200;
        	return "Calories added successfully!";
        }
        else {
        	responseCode = 500;
        	return "Failed to add calories.";
        }
    }

    private static HashMap<String, String> decodeInput(String in) {
        String[] requestArray = in.split("&");
        HashMap<String, String> out = new HashMap<>();

        for (String s : requestArray) {
            String[] keyVal = s.split("=", 2);
            out.put(keyVal[0], keyVal[1]);
        }

        out.replaceAll((k, v) -> URLDecoder.decode(v, StandardCharsets.UTF_8));
        return out;
    }

    private static Connection create_connection() {
    	InputStream in = Handle.class.getResourceAsStream("/password.conf");
        String password = null;

        // Read the password from the file
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            password = br.readLine();
        } catch (IOException e) {
            System.err.println("Failed to read the password file: " + e.getMessage());
            return null;
        }

        // Connect to the database
        try {
            return DriverManager.getConnection("jdbc:postgresql:Weight", "postgres", password);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
