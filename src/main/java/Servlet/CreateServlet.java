package Servlet;

import java.io.BufferedReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializers.Default;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import Controller.DatabaseOperation;
import Controller.UserDataHandler;
import Model.Column;
import Model.Constraint;
import Model.Database;
import Model.User;
import Util.SessionUtil;

/**
 * Servlet implementation class CreateServlet
 */
@WebServlet("/Service/CreateServlet")
public class CreateServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CreateServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
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

		System.out.println("hekljgudysgfskrhg");

		if ("createDatabase".equals(action) && UserDataHandler.isValidName(dbName)) {
			DatabaseOperation databaseOperationObject = new DatabaseOperation(loggedInUser);
			result = databaseOperationObject.createDatabase(new Database(dbName));
			jsonResponse.put("result", result);
		} else if ("createTable".equals(action)) {
			System.out.println("hekljgudysgfskrhg");
			String tableName = String.valueOf(jsonObject.get("tableName"));
			DatabaseOperation databaseOperationObject = new DatabaseOperation(loggedInUser);
			List<Column> columns = new ArrayList<>();

			// Extract "columns" array
			JSONArray columnsArray = (JSONArray) jsonObject.get("columnList");
			for (Object columnObj : columnsArray) {
				JSONObject columnJson = (JSONObject) columnObj;

				String columnName = String.valueOf(columnJson.get("colName"));
				String columnType = String.valueOf(columnJson.get("datatype"));

				JSONArray constraintsArray = (JSONArray) columnJson.get("consList");
				List<Constraint> constraints = new ArrayList<>();

				for (Object consObj : constraintsArray) {
					System.out.println("hekljgudysgfskrhg");
					JSONObject constraintsJson = (JSONObject) consObj;
					String constraint = (String) constraintsJson.get("constraint");

					Constraint cons = null;
					if ("NONE".equals(constraint)) {
						cons = new Constraint(null);
					} else if ("DEF".equals(constraint)) {
						Object rawValue = constraintsJson.get("default");
						Object value=null;
			            try {
			                switch (columnType) {
			                    case "STRING":
			                        value = rawValue.toString();
			                        System.out.println("heelo");
			                        break;
			                    case "BLOB":
			                        value = rawValue.toString();
			                        break;
			                    case "INT":
			                        if (rawValue instanceof Long) {
			                            value = ((Long) rawValue).intValue();
			                        } else {
			                            value = Integer.parseInt(rawValue.toString());
			                        }
			                        break;
			                    case "FLOAT":
			                        if (rawValue instanceof Double) {
			                            value = ((Double) rawValue).floatValue();
			                        } else {
			                            value = Float.parseFloat(rawValue.toString());
			                        }
			                        break;
			                    case "CHAR":
			                        String charValue = rawValue.toString();
			                        if (charValue.length() == 1) {
			                            value = charValue.charAt(0);
			                        } else {
			                            throw new IllegalArgumentException("Invalid CHAR value");
			                        }
			                        break;
			                    case "BOOL":
			                        if (rawValue instanceof Boolean) {
			                            value = rawValue;
			                        } else {
			                            String boolStr = rawValue.toString().toLowerCase();
			                            if (boolStr.equals("true") || boolStr.equals("false")) {
			                                value = Boolean.parseBoolean(boolStr);
			                            } else {
			                                throw new IllegalArgumentException("Invalid BOOL value");
			                            }
			                        }
			                        break;
			                    default:
			                        throw new IllegalArgumentException("Unknown data type: " + columnType);
			                }
			            } catch (Exception e) {
			                jsonResponse.put("result", false);
			                response.getWriter().write(jsonResponse.toJSONString());
			                return;
			            }
						cons = new Constraint(constraint, value);
					} else if ("FK".equals(constraint)) {
						String refTable = (String) constraintsJson.get("refTable");
						String refColumn = (String) constraintsJson.get("refColumn");
						cons = new Constraint(constraint, refTable, refColumn);
					} else {
						cons = new Constraint(constraint);
					}
					constraints.add(cons);
				}
				Column column = new Column(columnName, columnType, constraints);
				columns.add(column);
			}
			// Call the createTable method with extracted data
			result = databaseOperationObject.createTable(dbName, tableName, columns);
			jsonResponse.put("result", result);
		} else {
			jsonResponse.put("result", false);
		}
		response.getWriter().write(jsonResponse.toJSONString());
	}

}
