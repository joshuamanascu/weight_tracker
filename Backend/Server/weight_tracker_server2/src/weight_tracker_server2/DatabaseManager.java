package weight_tracker_server2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;

public class DatabaseManager {
	
	private static String insertCalories = "INSERT INTO CALORIES (date, calories) VALUES (?, ?)";
	private static String getCalories = "SELECT COALESCE(SUM(calories), 0) FROM CALORIES WHERE date = ?";
	private static String getRecentEntries = "SELECT date, calories FROM CALORIES ORDER BY date DESC LIMIT ?";
	private static String getCurrentWeight = "SELECT weight FROM WEIGHT ORDER BY date DESC LIMIT 1";
	
	public Connection con;
	
	public DatabaseManager() {
        this.con = create_connection();
    }

	private static Connection create_connection() {
    	InputStream in = DatabaseManager.class.getResourceAsStream("/password.conf");
        String password = null;

        // Read the password from the file
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            password = br.readLine();
            br.close();
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
	
	public String getCurrentWeight() throws SQLException {
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
    
    public String getRecentEntries(int num) throws SQLException {
    	
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


    public String getCalories(String date) throws SQLException {
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

    public int addCalories(String date, int amount) throws SQLException {
        PreparedStatement statement = con.prepareStatement(insertCalories);
        statement.setDate(1, Date.valueOf(date));
        statement.setInt(2, amount);

        int rowsAffected = statement.executeUpdate();
        if (rowsAffected > 0 ) {
        	return 200; //Calories added successfully!
        }
        else {
        	return 500; //Failed to add calories.
        }
    }

}
