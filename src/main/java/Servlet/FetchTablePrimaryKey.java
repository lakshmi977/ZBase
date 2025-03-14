package Servlet;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import Model.Database;
import Model.User;
import Util.SessionUtil;

/**
 * Servlet implementation class FetchTablePrimaryKey
 */
@WebServlet("/Service/FetchTablePrimaryKey")
public class FetchTablePrimaryKey extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "http://localhost:8080");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setStatus(HttpServletResponse.SC_OK);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setHeader("Access-Control-Allow-Origin", "http://localhost:8080");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		
		response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");

	    // ✅ If the token is valid, continue processing the request
	    BufferedReader reader = request.getReader();
	    StringBuilder jsonBuilder = new StringBuilder();
	    String line;
	    while ((line = reader.readLine()) != null) {
	        jsonBuilder.append(line);
	    }
	    reader.close();

	    // ✅ Parse the JSON request
	    JSONParser parser = new JSONParser();
	    JSONObject requestData;
	    try {
	        requestData = (JSONObject) parser.parse(jsonBuilder.toString());
	    } catch (ParseException e) {
	        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON Format");
	        return;
	    }
	    String dbName = (String) requestData.get("dbName");
	    User user = SessionUtil.getLoggedInUser(request);
	    if (user == null) {
	        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	        response.getWriter().write("{\"error\": \"User not authenticated\"}");
	        return;
	    }
	    Database db = new Database(dbName);

	    JSONObject jsonResponse = new JSONObject();
	    jsonResponse.put("columnData", db.getTablePrimaryKey(user));

	    response.getWriter().write(jsonResponse.toJSONString());
	}

}
