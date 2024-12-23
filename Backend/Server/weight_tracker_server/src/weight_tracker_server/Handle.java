package weight_tracker_server;


import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;


public class Handle {
	static String insertCalories = "INSERT INTO CALORIES (date, calories) VALUES (?, ?)";
	static String getCalories = "SELECT COALESCE(SUM(calories), 0) FROM CALORIES WHERE date = ?";

	public static void main(String[] args) throws SQLException {
		
		Connection con = create_connection(args[1]);
		
		
		HashMap<String, String> in_map = decodeInput(args[0]);
		
		String httpResponse;
		
		if (in_map.get("request_type").equals("insert_calories")) {
			httpResponse = addCalories(con, in_map.get("date_eaten"), Integer.valueOf(in_map.get("calories")));
		}
		else if(in_map.get("request_type").equals("get_calories")) {
			httpResponse = getCalories(con, in_map.get("date_requested"));
		}
		else {
			httpResponse = "Error";
		}
		
		
		
        System.out.println(httpResponse);
		
	}
	
	public static String getCalories(Connection con, String date) {
		String httpResponse = null;
		
		try {
			PreparedStatement pstmt = con.prepareStatement(getCalories);

	        // Set parameters for the prepared statement
	        pstmt.setDate(1, Date.valueOf(date));    // Set the date

	        // Execute the insert
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs != null) {
	        	rs.next();
	        	int calories = rs.getInt(1); //SQL columns - not zero indexed
	        	
	        	
	        	httpResponse = 
	                "Status: 200 OK\n" +
	                "Content-Type: text/html\n\n" +
	                calories;
	        }
	        else {
	        	httpResponse = 
	                "Status: 500 INTERNAL SERVER ERROR\n" +
	                "Content-Type: text/html\n\n";
	        }
	        
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return httpResponse;
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
	                "Content-Type: text/html\n\n";
	        }
	        else {
	        	httpResponse = 
	                "Status: 500 INTERNAL SERVER ERROR\n" +
	                "Content-Type: text/html\n\n";
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
	
	public static Connection create_connection(String pass) {
		
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
