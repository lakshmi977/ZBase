package Servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Condition;

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
import Model.*;
import Util.SessionUtil;

/**
 * Servlet implementation class UpdateServlet
 */
@WebServlet("/Service/UpdateServlet")
public class UpdateServlet extends HttpServlet {
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

		User user = SessionUtil.getLoggedInUser(request);
		TableDAO tableDAO = new TableDAO(user, dbName, tableName);

		HashMap<String, String> columnDatatype = new HashMap<>();
		System.out.println("ARRARRARARAR"+(ArrayList<Column>) tableDAO.columnsArray);
		for (Column column : (ArrayList<Column>) tableDAO.columnsArray) {
			columnDatatype.put(column.getName(), column.getDataType());
		}

		JSONObject jsonResponse = new JSONObject();
		// Extract 'values' from JSON
		JSONObject valuesObject = (JSONObject) requestData.get("columnValue");

		// Convert 'values' to HashMap<String, Object>
		HashMap<String, Object> columnData = new HashMap<>();
		for (Object key : valuesObject.keySet()) {
			String columnName = key.toString();

			// Ensure the column exists in the schema
			if (!columnDatatype.containsKey(columnName)) {
//                System.out.println("⚠Warning: Column '" + columnName + "' does not exist in the table schema.");
				continue;
			}

			Object rawValue = valuesObject.get(columnName);
			Object value = null;
			String dataType = columnDatatype.get(columnName);

			// Handle null values
			if (rawValue == null) {
				columnData.put(columnName, null);
				continue;
			}

			// Type Validation
			try {
				switch (dataType) {
				case "STRING":
					value = rawValue.toString();
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
						jsonResponse.put("result", false);
						response.getWriter().write(jsonResponse.toJSONString());
						return;
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
							jsonResponse.put("result", false);
							response.getWriter().write(jsonResponse.toJSONString());
							return;
						}
					}
					break;
				default:
					jsonResponse.put("result", false);
					response.getWriter().write(jsonResponse.toJSONString());
					return;
				}
			} catch (Exception e) {
				System.out.println("Invalid value for column " + columnName + ": " + rawValue);
				jsonResponse.put("result", false);
				response.getWriter().write(jsonResponse.toJSONString());
				return;
			}

			columnData.put(columnName, value);
		}

		JSONArray conditionObject = (JSONArray) requestData.get("conditions");
		
		
		// Convert 'values' to HashMap<String, Object>
		System.out.println("column"+ columnDatatype);
		List<ConditionGroup> conditionList = new ArrayList<ConditionGroup>();
		if(conditionObject.size()>0) {
			for (int i = 0; i < conditionObject.size(); i++) {
				JSONObject obj = (JSONObject) conditionObject.get(i);
				String columnName = obj.get("column").toString();
				String operator = obj.get("operator").toString();
				String dataType = null;
				if(columnDatatype.get(columnName)!=null) {
					dataType = columnDatatype.get(columnName).toString();
				}
				String operation=null;
				if(dataType==null) {
					jsonResponse.put("result", false);
					response.getWriter().write(jsonResponse.toJSONString());
					return;
				}
				if(i>0) {
					operation=obj.get("operation").toString();
				}
				
				Object rawValue = obj.get("value");
				Object value=null;
				// Type Validation
				try {
					switch (dataType) {
					case "STRING":
						value = rawValue.toString();
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
							jsonResponse.put("result", false);
							response.getWriter().write(jsonResponse.toJSONString());
							return;
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
								jsonResponse.put("result", false);
								response.getWriter().write(jsonResponse.toJSONString());
								return;
							}
						}
						break;
					default:
						jsonResponse.put("result", false);
						response.getWriter().write(jsonResponse.toJSONString());
						return;
					}
				} catch (Exception e) {
					System.out.println("Invalid value for column " + columnName + ": " + rawValue);
					jsonResponse.put("result", false);
					response.getWriter().write(jsonResponse.toJSONString());
					return;
				}

				conditionList.add(new ConditionGroup(columnName, operator, value, operation));
			}
		}
		else {
			conditionList=null;
		}
		

        System.out.println("Calleing updatre ethios");
//        System.out.println();
		// Insert data into the table
		boolean updateResult = tableDAO.updatemethod(columnData, conditionList);

		// Send response
		jsonResponse.put("result", updateResult);
		response.getWriter().write(jsonResponse.toJSONString());
	}

}
