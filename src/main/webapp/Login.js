function handleSubmit(event) {
    event.preventDefault();
    handleLogin();
	console.log("Auth servlet");
}

document.addEventListener("DOMContentLoaded", function () {
    document.querySelector(".submit-btn").addEventListener("click", async function (event) {
        event.preventDefault();

        let usernameInput = document.getElementById("email"); 
        let passwordInput = document.getElementById("password");

        if (!usernameInput || !passwordInput) {
            console.error("Username or password input field not found!");
            return;
        }

        let userData = {
            email: usernameInput.value.trim(),
            password: passwordInput.value.trim(),
        };

        console.log("Sending request data:", JSON.stringify(userData));

        try {
            const response = await fetch("http://localhost:8080/Database/LoginServlet", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify(userData),
            });

            const text = await response.text();

            try {
                const responseData = JSON.parse(text);
                if (response.ok) {
                    window.location.href = "dashboard.html";
                } else {
                    console.error(`Sign Up failed: ${responseData.error || "Unknown error"}`);
                }
            } catch (jsonError) {
                console.error("Unexpected response:", text);
            }
        } catch (error) {
            console.error("Error:", error);
        }
    });
});

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function showValidationErrors(email, password) {
    const emailGroup = document.getElementById("email").parentNode;
    const passwordGroup = document.getElementById("password").parentNode;

    if (!isValidEmail(email)) {
        emailGroup.classList.add("error");
    } else {
        emailGroup.classList.remove("error");
    }

    if (password.length < 6) {
        passwordGroup.classList.add("error");
    } else {
        passwordGroup.classList.remove("error");
    }
}

document.getElementById("loginForm").addEventListener("submit", handleSubmit);


window.onload = function () {
	console.log("Auth servlet2");
    fetch("http://localhost:8080/Database/CheckAuthServlet", {
        method: "GET",
        credentials: "include",
    })
        .then(response => response.json())
        .then(data => {
            if (data.authenticated && window.location.pathname.endsWith("Login.html")) {
                window.location.href = "dashboard.html";
            }
        })

        .catch(error => console.error("Auth check failed:", error));
};
