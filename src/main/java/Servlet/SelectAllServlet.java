package Servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import Controller.TableDAO;
import Model.Column;
import Model.ConditionGroup;
import Model.User;
import Util.SessionUtil;

/**
 * Servlet implementation class SelectAllServlet
 */
@WebServlet("/Service/SelectAllServlet")
public class SelectAllServlet extends HttpServlet {
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

		// Read JSON request body
		BufferedReader reader = request.getReader();
		StringBuilder jsonBuilder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			jsonBuilder.append(line);
		}
		reader.close();

		// Parse JSON
		JSONParser parser = new JSONParser();
		JSONObject requestData;
		try {
			requestData = (JSONObject) parser.parse(jsonBuilder.toString());
		} catch (ParseException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON Format");
			return;
		}

		// Extract dbName and tableName
		String dbName = (String) requestData.get("dbName");
		String tableName = (String) requestData.get("tableName");
		User user=SessionUtil.getLoggedInUser(request);
		
		if (user == null) {
		    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
		    return;
		}
		
		TableDAO tableDAO = new TableDAO(user, dbName, tableName);

		
		JSONObject jsonResponse = new JSONObject();

//        System.out.println();
		// Insert data into the table
		List<List<Object>> insertResult = tableDAO.viewData();
		JSONArray jsonArray = new JSONArray();

		for (List<Object> column : insertResult) {
		    JSONArray jsonColumn = new JSONArray();
		    jsonColumn.addAll(column);
		    jsonArray.add(jsonColumn);
		}

		// Send response
		jsonResponse.put("result", jsonArray);
		response.getWriter().write(jsonResponse.toJSONString());
	}

}
