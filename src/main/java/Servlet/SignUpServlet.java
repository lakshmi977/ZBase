package Servlet;

import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import Controller.UserDataHandler;
import Model.User;
import Util.AuthService;

@WebServlet("/SignUpServlet")
public class SignUpServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final AuthService authService = AuthService.getInstance(); // Singleton AuthService

    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:8080"); // Allow only frontend
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true"); // Enable credentials
        response.setStatus(HttpServletResponse.SC_OK);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:8080"); // Only allow frontend origin
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true"); // Allow cookies

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonRequest = (JSONObject) parser.parse(sb.toString());

            String username = (String) jsonRequest.get("username");
            String email = (String) jsonRequest.get("email");
            String password = (String) jsonRequest.get("password");

            if (username == null || email == null || password == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Missing required fields\"}");
                return;
            }

            // ✅ Save user
            UserDataHandler.saveUser(username, email, password);

            // ✅ Create JWT Token
            String token = authService.generateToken(username);

            // ✅ Store Token in HTTP-Only Cookie
            Cookie cookie = new Cookie("token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);

            // ✅ Store user in session after successful signup
            HttpSession session = request.getSession(true);
            User user = new User(username, email, password);
            session.setAttribute("loggedInUser", user); // Store user in session
            session.setMaxInactiveInterval(3600); // Optional: 1 hour session timeout

            // ✅ Send JSON Response
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("message", "Signup successful");
            jsonResponse.put("token", token);
            jsonResponse.put("username", username);

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(jsonResponse.toJSONString());
            System.out.println("Signup successful for user: " + username);

        } catch (ParseException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print("{\"error\": \"User already exists\"}");
        }
    }
}
