package weight_tracker_server2;

import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Handle {
    static String insertCalories = "INSERT INTO CALORIES (date, calories) VALUES (?, ?)";
    static String getCalories = "SELECT COALESCE(SUM(calories), 0) FROM CALORIES WHERE date = ?";
    static String getRecentEntries = "SELECT date, calories FROM CALORIES ORDER BY date DESC LIMIT ?";
    static String getCurrentWeight = "SELECT weight FROM WEIGHT ORDER BY date DESC LIMIT 1";
    
    static int responseCode;
    static Connection con;
    
    

    public static void main(String[] args) throws Exception {
    	
    	con = create_connection(); // Have the connection be a static global variable created once
    	
        // Start an HTTP server on port 8000
    	HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8000), 0);
        server.createContext("/weight", exchange -> handleRequest(exchange));
        server.setExecutor(null); // Creates a default executor
        System.out.println("Server is running on http://localhost:8000");
        server.start();
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
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
        try {
            if (in_map.get("request_type").equals("insert_calories")) {
                response = addCalories(con, in_map.get("date_eaten"), Integer.valueOf(in_map.get("calories")));
            } 
            else if (in_map.get("request_type").equals("get_calories")) {
                response = getCalories(con, in_map.get("date_requested"));
            } 
            else if (in_map.get("request_type").equals("get_recent_entries")) {
            	response = getRecentEntries(con, Integer.valueOf(in_map.get("num")));
            }
            else if (in_map.get("request_type").equals("get_weight")) {
            	response = getCurrentWeight(con);
            }
            else {
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
    
    public static String getCurrentWeight(Connection con) throws SQLException {
    	PreparedStatement statement = con.prepareStatement(getCurrentWeight);
    	ResultSet rs = statement.executeQuery();
    	
    	if (rs.next()) {
    		int weight = rs.getInt(1);
    		return String.valueOf(weight);
    	}
    	else {
    		return "Error - No weight on file";
    	}
    }
    
    public static String getRecentEntries(Connection con, int num) throws SQLException {
    	
    	if (num > 10) {
    		num = 10; //LIMIT TO TEN
    	}
    	
    	PreparedStatement statement = con.prepareStatement(getRecentEntries);
    	statement.setInt(1, num);
    	ResultSet rs = statement.executeQuery();
    	
    	Gson gson = new Gson();
    	ArrayList<HashMap<String, String>> entries = new ArrayList<HashMap<String,String>>();
    	
    	while (rs.next()) {
    		HashMap<String, String> map = new HashMap<String, String>();
    		map.put("date", rs.getString("date"));
    		map.put("calories", String.valueOf(rs.getInt("calories")));
    		entries.add(map);
    	}
    	
    	String json = gson.toJson(entries);
    	return json;
    	
    }


    private static String getCalories(Connection con, String date) throws SQLException {
        PreparedStatement statement = con.prepareStatement(getCalories);
        statement.setDate(1, Date.valueOf(date));
        ResultSet rs = statement.executeQuery();

        if (rs.next()) {
            int calories = rs.getInt(1);
            return String.valueOf(calories);
        } else {
            return "0";
        }
    }

    private static String addCalories(Connection con, String date, int amount) throws SQLException {
        PreparedStatement statement = con.prepareStatement(insertCalories);
        statement.setDate(1, Date.valueOf(date));
        statement.setInt(2, amount);

        int rowsAffected = statement.executeUpdate();
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
