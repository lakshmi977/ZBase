package Servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import Controller.TableDAO;
import Model.Column;
import Model.Constraint;
import Model.User;
import Util.SessionUtil;

@WebServlet("/Service/FetchTableData")
public class FetchTableData extends HttpServlet {
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

		BufferedReader reader = request.getReader();
		StringBuilder jsonBuilder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			jsonBuilder.append(line);
		}
		reader.close();

		JSONParser parser = new JSONParser();
		JSONObject requestData;
		try {
			requestData = (JSONObject) parser.parse(jsonBuilder.toString());
		} catch (ParseException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON Format");
			return;
		}

		String tableName = (String) requestData.get("tableName");
		String dbName = (String) requestData.get("dbName");

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		User user = SessionUtil.getLoggedInUser(request);

		TableDAO tableDAO = new TableDAO(user, dbName, tableName);

		JSONArray columnsJsonArray = new JSONArray();
		for (Column column :(ArrayList<Column>) tableDAO.columnsArray) {
		    columnsJsonArray.add(column.toJson()); //Now using toJson() directly
		}
		
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("columns", columnsJsonArray);
		
		response.getWriter().write(jsonResponse.toJSONString());
	}
}
