package Servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import Controller.DatabaseOperation;
import Controller.UserDataHandler;
import Model.Column;
import Model.Constraint;
import Model.Database;
import Model.User;
import Util.SessionUtil;

/**
 * Servlet implementation class DropServlet
 */
@WebServlet("/Service/DropServlet")
public class DropServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DropServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */

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
		response.setCharacterEncoding("UTF-8");

		// Validate user session
		User loggedInUser = SessionUtil.getLoggedInUser(request);
		if (loggedInUser == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().write("{\"error\":\"User not logged in\"}");
			return;
		}

		// Read JSON request
		StringBuilder jsonBuffer = new StringBuilder();
		String line;
		try (BufferedReader reader = request.getReader()) {
			while ((line = reader.readLine()) != null) {
				jsonBuffer.append(line);
			}
		}

		JSONParser parser = new JSONParser();
		JSONObject jsonObject;
		try {
			jsonObject = (JSONObject) parser.parse(jsonBuffer.toString());
		} catch (ParseException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().write("{\"error\":\"Invalid JSON format\"}");
			return;
		}

		// Extract action
		String action = String.valueOf(jsonObject.get("action"));
		boolean result = false;
		JSONObject jsonResponse = new JSONObject();

		String dbName = String.valueOf(jsonObject.get("dbName"));

		if ("dropDb".equals(action) && UserDataHandler.isValidName(dbName)) {
			DatabaseOperation databaseOperationObject = new DatabaseOperation(loggedInUser);
			result = databaseOperationObject.dropDatabase(dbName);
			jsonResponse.put("result", result);
		} else if ("dropTable".equals(action)) {
			String tableName = String.valueOf(jsonObject.get("tableName"));
			DatabaseOperation databaseOperationObject = new DatabaseOperation(loggedInUser);
			result = databaseOperationObject.dropTable(dbName,tableName);
			jsonResponse.put("result", result);
		} else {
			jsonResponse.put("result", false);
		}
		response.getWriter().write(jsonResponse.toJSONString());

	}

}
