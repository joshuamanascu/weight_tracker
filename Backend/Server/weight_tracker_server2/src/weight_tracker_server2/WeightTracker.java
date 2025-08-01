package weight_tracker_server2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class WeightTracker {
	
	public DatabaseManager DBManager;
	public int responseCode;
	
	public WeightTracker() throws IOException {
		DBManager = new DatabaseManager();
		
		// Start an HTTP server on port 8000
    	HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8000), 0);
        server.createContext("/weight", exchange -> handleRequest(exchange));
        server.setExecutor(null); // Creates a default executor
        System.out.println("Server is running on http://localhost:8000");
        server.start();
	}
	
	public String handleRequest(HttpExchange exchange) throws IOException {
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
                responseCode = DBManager.addCalories(in_map.get("date_eaten"), Integer.valueOf(in_map.get("calories")));
                
                if (responseCode == 200) response = "Calories added successfully!";
                else response = "Failed to add calories.";
            } 
            else if (in_map.get("request_type").equals("get_calories")) {
                response = DBManager.getCalories(in_map.get("date_requested"));
            } 
            else if (in_map.get("request_type").equals("get_recent_entries")) {
            	response = DBManager.getRecentEntries(Integer.valueOf(in_map.get("num")));
            }
            else if (in_map.get("request_type").equals("get_weight")) {
            	response = DBManager.getCurrentWeight();
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
        
        return response;
    }
	
	private HashMap<String, String> decodeInput(String in) {
        String[] requestArray = in.split("&");
        HashMap<String, String> out = new HashMap<>();

        for (String s : requestArray) {
            String[] keyVal = s.split("=", 2);
            out.put(keyVal[0], keyVal[1]);
        }

        out.replaceAll((k, v) -> URLDecoder.decode(v, StandardCharsets.UTF_8));
        return out;
    }

}
