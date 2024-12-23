package weight_tracker_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;


public class Handle {
	static String insertCalories = "INSERT INTO CALORIES (date, calories) VALUES (?, ?)";

	public static void main(String[] args) throws SQLException {
		Connection con = create_connection();
		
		HashMap<String, String> in_map = decodeInput(args[0]);
		
		
		
		String httpResponse;
		
		if (in_map.get("request_type").equals("insert_calories")) {
			httpResponse = addCalories(con, in_map.get("date_eaten"), Integer.valueOf(in_map.get("calories")));
		}
		else {
			httpResponse = "Error";
		}
		
        System.out.println(httpResponse);
		
	}
	
	public static String addCalories(Connection con, String date, int amount) {
		String httpResponse = null;
		
		try {
			PreparedStatement pstmt = con.prepareStatement(insertCalories);

	        // Set parameters for the prepared statement
	        pstmt.setDate(1, Date.valueOf(date));    // Set the date (1st placeholder)
	        pstmt.setInt(2, amount);  // Set the calories (2nd placeholder)

	        // Execute the insert
	        int rowsAffected = pstmt.executeUpdate();
	        
	        
	        if (rowsAffected != 0) {
	        	httpResponse = 
	                "Status: 200 OK\n" +
	                "Content-Type: text/html\n\n" +
	                "Calories submitted successfully!";
	        }
	        else {
	        	httpResponse = 
	                "Status: 500 INTERNAL SERVER ERROR\n" +
	                "Content-Type: text/html\n\n" +
	                "Bad Database stuff";
	        }
	        
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return httpResponse;
		
	}
	
	public static HashMap<String, String> decodeInput(String in) {
		String request_array[] = in.split("&");
		HashMap<String, String> out = new HashMap<String, String>();
		
		for (String s : request_array) {
			String[] keyVal = s.split("=", 2);
			out.put(keyVal[0], keyVal[1]);
		}
		
		
		out.replaceAll( (k, v) -> URLDecoder.decode(v, StandardCharsets.UTF_8));
		return out;
	}
	
	public static Connection create_connection() {
		
		InputStream in = Handle.class.getResourceAsStream("/password.conf");
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		
		String pass = "";
		try {
			pass = reader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Connection con = DriverManager.getConnection("jdbc:postgresql:Weight", "postgres", pass);
			return con;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

}
