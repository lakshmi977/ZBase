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
import Model.Column;
import Model.ConditionGroup;
import Model.OrderBy;
import Model.User;
import Util.SessionUtil;

/**
 * Servlet implementation class SelectServlet
 */
@WebServlet("/Service/SelectServlet")
public class SelectServlet extends HttpServlet {
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

		HashMap<String, Object> columnDatatype = new HashMap<>();
		for (Column column : (ArrayList<Column>) tableDAO.columnsArray) {
			columnDatatype.put(column.getName(), column.getDataType());
		}

		// Extract 'values' from JSON
		JSONArray columnDatasArray = (JSONArray) requestData.get("columns");

		// Convert 'values' to HashMap<String, Object>
		List<String> columndata=new ArrayList<String>();
		for (Object obj : columnDatasArray) {
		    columndata.add(obj.toString());
		}
		

		JSONArray conditionObject = (JSONArray) requestData.get("Conditions");
		JSONObject jsonResponse = new JSONObject();

		// Convert 'values' to HashMap<String, Object>
		List<ConditionGroup> conditionList = new ArrayList<ConditionGroup>();
		if(conditionObject!=null && conditionObject.size()>0) {
			for (int i = 0; i < conditionObject.size(); i++) {
				JSONObject obj = (JSONObject) conditionObject.get(i);
				String columnName = obj.get("columnName").toString();
				String operator = obj.get("conditionType").toString();
				String dataType = columnDatatype.get(columnName).toString();
				String operation=null;
				if(i>0) {
					operation=obj.get("operation").toString();
				}
				
				Object rawValue = obj.get("conditionValue");
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
		

        System.out.println(columndata);
        
        HashMap<String, List<String>> aggregate = new HashMap<>();
        JSONArray aggregateObject = (JSONArray) requestData.get("aggregate");
        
        if(aggregateObject.size()<1) {
        	aggregate=null;
        }
        else {
        	for (int i = 0; i < aggregateObject.size(); i++) {
                JSONObject obj = (JSONObject) aggregateObject.get(i);  // Corrected from conditionObject
                String name = obj.get("column").toString();
                String function = obj.get("function").toString();

                if (!aggregate.containsKey(name)) {
                    // Initialize list if the column is not already present
                    List<String> arr = new ArrayList<>();
                    aggregate.put(name, arr);
                }
                
                // Add function to the existing list
                aggregate.get(name).add(function);
            }
        }

        
        JSONObject obj = (JSONObject) requestData.get("order");
        OrderBy order=null;
        if(obj!=null) {
        	String orderByColumn=obj.get("column").toString();
            String dir=obj.get("direction").toString();
            order=new OrderBy(orderByColumn, dir);
        }
        
        
//        System.out.println();
		// Insert data into the table
		List<List<Object>> insertResult = tableDAO.wholeSelectCondition(columndata,aggregate,conditionList,order);
		System.out.println(("(((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((("));
		JSONArray jsonArray = new JSONArray();
		for (List<Object> row : insertResult) {
		    JSONArray jsonRow = new JSONArray();
		    jsonRow.addAll(row);
		    jsonArray.add(jsonRow);
		}
		// Send response
		jsonResponse.put("result", jsonArray);
		response.getWriter().write(jsonResponse.toJSONString());
	}

}
