package Model;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Column {
	
	String name;        
	String dataType;
	List<Constraint> constraint;
	
	public Column(String name, String dataType, List<Constraint> constraint) {
		super();
		this.name = name;
		this.dataType = dataType;
		this.constraint = constraint;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public List<Constraint> getConstraints() {
		return constraint;
	}
	
	public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("dataType", dataType);

        // ✅ Convert constraints list to JSON
        JSONArray constraintsJson = new JSONArray();
        for (Constraint constraint : constraint) {
            constraintsJson.add(constraint.toJson()); // Using toJson() from Constraint class
        }
        json.put("constraints", constraintsJson);

        return json;
    }

}
