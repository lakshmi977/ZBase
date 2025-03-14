package Servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

import Controller.TableDAO;
import Controller.UserDataHandler;
import Model.Column;
import Model.Constraint;
import Model.User;
import Util.SessionUtil;

/**
 * Servlet implementation class AlterServlet
 */
@WebServlet("/Service/AlterServlet")
public class AlterServlet extends HttpServlet {
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
		String action = (String) requestData.get("action");

		User user = SessionUtil.getLoggedInUser(request);
		TableDAO tableDAO = new TableDAO(user, dbName, tableName);

		JSONObject jsonResponse = new JSONObject();

		boolean result = false;

		if (action.equals("DROP_COLUMN")) {
			String columnName = (String) requestData.get("columnName");
			result=tableDAO.dropColumn(columnName);

		} else if (action.equals("ADD_COLUMN")) {

			String columnName = String.valueOf(requestData.get("columnName"));
			String columnType = String.valueOf(requestData.get("datatype"));

			JSONArray constraintsArray = (JSONArray) requestData.get("conslist");
			List<Constraint> constraints = new ArrayList<>();

			for (Object consObj : constraintsArray) {
				JSONObject constraintsJson = (JSONObject) consObj;
				String constraint = (String) constraintsJson.get("constraint");

				Constraint cons = null;
				if ("NONE".equals(constraint)) {
					cons = new Constraint(null);
				} else if ("DEF".equals(constraint)) {
					Object rawValue = constraintsJson.get("default");
					Object value = null;
					try {
						value=UserDataHandler.convertValue(rawValue, columnType);
					} catch (Exception e) {
						jsonResponse.put("result", false);
						response.getWriter().write(jsonResponse.toJSONString());
						return;
					}
					cons = new Constraint(constraint, value);
				} else if ("FK".equals(constraint)) {
					String refTable = (String) constraintsJson.get("fkTable");
					String refColumn = (String) constraintsJson.get("fkColumn");
					cons = new Constraint(constraint, refTable, refColumn);
				} else {
					cons = new Constraint(constraint);
				}

				constraints.add(cons);
			}
			Column column = new Column(columnName, columnType, constraints);
			result=tableDAO.addColumn(column);

		} else if (action.equals("ADD_CONSTRAINT")) {
	
			String columnName = String.valueOf(requestData.get("columnName"));
			HashMap<String, String> columnDatatype = new HashMap<>();
			for (Column column : (ArrayList<Column>) tableDAO.columnsArray) {
				columnDatatype.put(column.getName(), column.getDataType());
			}
			
			String columnType=null;
			if(!columnDatatype.containsKey(columnName)) {
				result=false;
				////////////
				jsonResponse.put("result", false);
				response.getWriter().write(jsonResponse.toJSONString());
				return;
			}
			
			columnType=columnDatatype.get(columnName);
			
			JSONObject constraintsJson = (JSONObject) requestData.get("constraints");
			String constraint = (String) constraintsJson.get("constraint");

			Constraint cons = null;
			if ("NONE".equals(constraint)) {
				jsonResponse.put("result", false);
				response.getWriter().write(jsonResponse.toJSONString());
				return;
			} else if ("DEF".equals(constraint)) {
				Object rawValue = constraintsJson.get("default");
				Object value = null;
				try {
					value=UserDataHandler.convertValue(rawValue, columnType);
				} catch (Exception e) {
					jsonResponse.put("result", false);
					response.getWriter().write(jsonResponse.toJSONString());
					return;
				}
				cons = new Constraint(constraint, value);
				
			} else if ("FK".equals(constraint)) {
				String refTable = (String) constraintsJson.get("fkTable");
				String refColumn = (String) constraintsJson.get("fkColumn");
				cons = new Constraint(constraint, refTable, refColumn);
			} else {
				cons = new Constraint(constraint);
			}
			

			result=tableDAO.addConstraint(columnName,cons);
			

		} else if (action.equals("DROP_CONSTRAINT")) {
			String columnName = (String) requestData.get("columnName");
			String constraintName = (String) requestData.get("constraintName");
			
			result=tableDAO.dropConstraint(columnName,constraintName);

		} else if (action.equals("RENAME_COLUMN")) {
			String oldColumnName = (String) requestData.get("oldColumnName");
			String newColumnName = (String) requestData.get("newColumnName");
			
			result=tableDAO.renameColumn(oldColumnName,newColumnName);

		} else if (action.equals("CHANGE_DATATYPE")) {
			String columnName = (String) requestData.get("columnName");
			String dataType = (String) requestData.get("datatype");
			
			result=tableDAO.changeColumnDataType(columnName,dataType);

		}
		jsonResponse.put("result", result);
		response.getWriter().write(jsonResponse.toJSONString());

	}

}
