package Servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import Controller.UserSocket;
import Model.User;
import Util.SessionUtil;

@WebServlet("/Service/FetchDataServlet")
public class FetchDataServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000"); // Set correct frontend origin
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true"); // Allow cookies
        response.setStatus(HttpServletResponse.SC_OK);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("🚀 FetchDataServlet called...");

        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000"); // Allow only frontend
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true"); // Allow cookies
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // ✅ Ensure session exists
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("❌ No active session. User not authenticated.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Session expired. Please log in again.\"}");
            return;
        }

        // ✅ Fetch user from session
        
        System.out.println("   sessio nnnn    " +session.getAttribute("loggedInUser"));
//        User user = (User) session.getAttribute("user");
        User user = (User) session.getAttribute("loggedInUser");

        
       if (user == null) {
            System.out.println("❌ User not found in session.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"User not authenticated\"}");
            return;
        }

        // ✅ Debug session details
        System.out.println("✅ User authenticated: " + user.getUsername());
        System.out.println("🕒 Session Timeout: " + session.getMaxInactiveInterval() + " seconds");

        // ✅ Optional: Extend session timeout (1 hour)
        session.setMaxInactiveInterval(3600);

        // ✅ Fetch user database structure
        UserSocket getDatabaseObjSocket = new UserSocket(user.getUsername());
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("DATABASES", getDatabaseObjSocket.getFolderStructure());

        // ✅ Send response
        response.getWriter().write(jsonResponse.toJSONString());
        System.out.println("✅ Data sent successfully.");
    }
}
