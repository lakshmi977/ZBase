package Servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import Controller.TableDAO;
import Model.Column;
import Model.User;
import Util.SessionUtil;

@WebServlet("/Service/InsertServlet")
public class InsertServlet extends HttpServlet {
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

        ArrayList<String> datatypes = new ArrayList<>();
        ArrayList<String> columnNames = new ArrayList<>();
        for (Column column : (ArrayList<Column>)tableDAO.columnsArray) {
            datatypes.add(column.getDataType());
            columnNames.add(column.getName());
        }

        // Extract 'values' from JSON
        JSONObject valuesObject = (JSONObject) requestData.get("values");

        // Convert 'values' to HashMap<String, Object>
        HashMap<String, Object> columnData = new HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            String dataType = datatypes.get(i);

            Object rawValue = valuesObject.get(columnName);
            Object value = null;
            System.out.println("teasyduyfgihourystdfhjhjotyireryst"+rawValue);

            if(!rawValue.equals("NONE")) {
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
                            throw new IllegalArgumentException("Unknown data type: " + dataType);
                    }
                } catch (Exception e) {
                    System.out.println("Invalid value for column " + columnName + ": " + rawValue);
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("result", false);
                    response.getWriter().write(jsonResponse.toJSONString());
                    return;
                }
            }
            else {
            	value="NONE";
            }
            columnData.put(columnName, value);
        }

        // Insert data into the table
        boolean insertResult = tableDAO.insertValue(columnData);

        // Send response
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("result", insertResult);
        response.getWriter().write(jsonResponse.toJSONString());
    }
}
