const SERVER_URL = `${window.location.origin}`;

window.onload = function() {
	
	full_update();
	
	//Enter listener for button
	var calories_textBox = document.getElementById("calories_input");

	calories_textBox.addEventListener("keypress", function(event) {
		// If the user presses the "Enter" key on the keyboard
		if (event.key === "Enter") {
			// Cancel the default action, if needed
			event.preventDefault();
			// Trigger the button element with a click
			submitCalories();
		}
	});
	
	setInterval(full_update, 600000); //10 minutes
}

function full_update() {
	localDate = getCurrentDateString();
	document.getElementById("date_select").value = localDate;
	
	loadCalories(localDate);
	
	getRecentEntries(5);
	
	getCurrentWeight();
}

function getCurrentDateString() {
	const today = new Date();
	const year = today.getFullYear(); // Local year
	const month = String(today.getMonth() + 1).padStart(2, '0'); // Local month (0-based index, so add 1)
	const day = String(today.getDate()).padStart(2, '0'); // Local day

	// Format the date as YYYY-MM-DD
	const localDate = `${year}-${month}-${day}`;
	
	return localDate;
}

function getCurrentWeight() {
	
	const label = document.getElementById("weight_label");
	
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.onreadystatechange = function() { 
		if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
			var weight = xmlHttp.responseText;
			
			label.innerHTML = "Current Weight: " + weight;
		}
	}
	
	xmlHttp.open("POST", "/api/weight", true);
	
	const type = "get_weight";
	const request = `request_type=${type}`;
	
	xmlHttp.overrideMimeType("text/html");
	xmlHttp.send(request);
}


function clearTable(tableId) {
    // Get the table element by its ID
    const table = document.getElementById(tableId);
    
    // Loop through the rows in reverse order (to avoid re-indexing issues)
    for (let i = table.rows.length - 1; i >= 0; i--) {
        const row = table.rows[i];
        
        // Check if the row contains `<th>` elements
        const isHeader = row.querySelectorAll("th").length > 0;
        
        // If it's not a header row, remove it
        if (!isHeader) {
            table.deleteRow(i);
        }
    }
}


function getRecentEntries(number) {
	
	const table = document.getElementById("recent_entries_table");
	clearTable("recent_entries_table");
	
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.onreadystatechange = function() { 
		if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
			var arr = JSON.parse(xmlHttp.responseText);
			
			arr.forEach(function(entry) {
				var row = table.insertRow();
				
				row.insertCell().innerHTML = entry.date;
				row.insertCell().innerHTML = entry.calories;
			});
		}
	}
	
	xmlHttp.open("POST", "/api/weight", true);
	const type = "get_recent_entries";
	
	const request = `request_type=${type}&num=${number}`;
	
	xmlHttp.overrideMimeType("text/html");
	xmlHttp.send(request);
}

//Change the Calories meter
function loadCalories(date) {
	
	const progressBar = document.getElementById("calories_bar");
	const progressText = document.getElementById("calories_text");
	
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.onreadystatechange = function() { 
		if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
			progressBar.value = xmlHttp.responseText;
			progressText.innerHTML = xmlHttp.responseText + "/2100";
		}
	}
	
	xmlHttp.open("POST", "/api/weight", true);
	//xmlHttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	
	const type = "get_calories";
	
	const request = `request_type=${type}&date_requested=${date}`;
	
	xmlHttp.overrideMimeType("text/html");
	xmlHttp.send(request);
	
}

function submitCalories() {
	var xmlHttp = new XMLHttpRequest();
	
	const responseLabel = document.getElementById("response_label");
	
	xmlHttp.onreadystatechange = function() { 
		if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
			responseLabel.innerHTML = "Added successfully!"
			responseLabel.style.display = "block";
			
			loadCalories(getCurrentDateString());
			getRecentEntries(5);
			
			//Remove message after 5 seconds
			setTimeout(function() {
				responseLabel.style.display = "none";
			}, 5000)
			
			
			
		}
		else if (xmlHttp.readyState == 4 && xmlHttp.status == 500) {
			responseLabel.innerHTML = "Error - Not added!"
			responseLabel.style.display = "block";
			
			setTimeout(function() {
				responseLabel.style.display = "none";
			}, 5000)
		}
				
	}
	
	xmlHttp.open("POST", "/api/weight", true);
	//xmlHttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	
	xmlHttp.overrideMimeType("text/html");
	var formData = new FormData(document.getElementById("form_eat"));
	xmlHttp.send(new URLSearchParams(formData))
	
	
}
