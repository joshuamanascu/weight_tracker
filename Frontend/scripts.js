const SERVER_URL = `${window.location.origin}`;

window.onload = function() {
	
	localDate = getCurrentDateString();
	document.getElementById("date_select").value = localDate;
	
	loadCalories(localDate);
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

//Change the Calories meter
function loadCalories(date) {
	
	const progressBar = document.getElementById("calories_bar");
	const progressText = document.getElementById("calories_text");
	
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.onreadystatechange = function() { 
		if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
			progressBar.value = xmlHttp.responseText;
			progressText.innerHTML = xmlHttp.responseText + "/1800";
		}
	}
	
	xmlHttp.open("POST", SERVER_URL + ":8000/weight", true);
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
	
	xmlHttp.open("POST", SERVER_URL + ":8000/weight", true);
	//xmlHttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	
	xmlHttp.overrideMimeType("text/html");
	var formData = new FormData(document.getElementById("form_eat"));
	xmlHttp.send(new URLSearchParams(formData))
	
	
}
