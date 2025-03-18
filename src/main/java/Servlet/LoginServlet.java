package Servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import Model.User;
import Util.AuthService;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final AuthService authService = AuthService.getInstance();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Read JSON from request body
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
    	System.out.println("login");
        // Parse JSON using json-simple
        JSONParser parser = new JSONParser();
        JSONObject jsonRequest;
        try {
            jsonRequest = (JSONObject) parser.parse(sb.toString());

            String email = (String) jsonRequest.get("email");
            String password = (String) jsonRequest.get("password");

            // Authenticate user
            User user = authService.login(email, password);
            if (user != null) {
                // Generate JWT Token
                String token = authService.generateToken(user.getUsername());

                // Store user in session for later retrieval
                HttpSession session = request.getSession(true);
                session.setAttribute("loggedInUser", user);

                // Construct JSON response manually
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("message", "Login successful");
                jsonResponse.put("username", user.getUsername());
                jsonResponse.put("token", token);
                jsonResponse.put("success", true);

                // Set JWT token in HttpOnly Cookie
                Cookie jwtCookie = new Cookie("token", token);
                jwtCookie.setHttpOnly(true);
                jwtCookie.setPath("/");
                response.addCookie(jwtCookie);

                response.setStatus(HttpServletResponse.SC_OK);
                out.print(jsonResponse.toJSONString());
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("error", "Invalid email or password");
                out.print(errorResponse.toJSONString());
            }
        } catch (org.json.simple.parser.ParseException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", "Invalid JSON format");
            out.print(errorResponse.toJSONString());
        }
    }
}
