document.querySelector(".submit").addEventListener("click", async function(event) {
	event.preventDefault();

	let userData = {
		username: document.querySelector("input[name='username']").value.trim(),
		email: document.querySelector("input[name='email']").value.trim(),
		password: document.querySelector("input[name='password']").value.trim()
	};

	try {
		const response = await fetch("http://localhost:8080/Database/SignUpServlet", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			credentials: "include", // 🔥 Important: Allows cookies to be sent
			body: JSON.stringify(userData),
		});

		const text = await response.text(); // Get raw response text

		try {
			const responseData = JSON.parse(text); // Try parsing JSON
			if (response.ok) {
				/*alert("Signup Successful! Redirecting...");*/
				window.location.href = "dashboard.html";
			} else {
				/*alert(`Sign Up failed: ${responseData.error || "Unknown error"}`);*/
			}
		} catch (jsonError) {
			console.error("Unexpected response:", text);
			/*alert("Server error. Please check logs.");*/
		}

	} catch (error) {
		console.error("Error:", error);
		/*alert("Network error. Check your connection.");*/
	}
});


window.onload = function() {
	fetch("http://localhost:8080/Database/CheckAuthServlet", {
		method: "GET",
		credentials: "include", //  Sends cookies to check authentication
	})
		.then(response => response.json())
		.then(data => {
			console.log("asdfghjk",data.authenticated)
			if (data.authenticated && window.location.pathname.endsWith("SignUp.html")) {
				window.location.href = "dashboard.html"; // Redirect if already logged in
			}
		})
		.catch(error => console.error("Auth check failed:", error));
};

