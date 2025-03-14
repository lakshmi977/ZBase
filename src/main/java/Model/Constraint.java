package Model;

import java.io.Serializable;

import org.json.simple.JSONObject;

public class Constraint implements Serializable {
    private static final long serialVersionUID = 1L;  
    private String type; 
    private String referenceTable;  
    private String referenceColumn; 
    private Object defaultValue; // New field for the default constraint
    
    public Constraint( String type) {
        this.type = type;
    }

    // Constructor without default value; defaultValue is null by default.
    public Constraint(String type, String referenceTable, String referenceColumn) {
    	this(type);
        this.referenceTable = referenceTable;
        this.referenceColumn = referenceColumn;
    }
    
    // Overloaded constructor that accepts a default value.
    public Constraint( String type, Object defaultValue) {
    	this(type);
        this.defaultValue = defaultValue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReferenceTable() {
        return referenceTable;
    }

    public void setReferenceTable(String referenceTable) {
        this.referenceTable = referenceTable;
    }

    public String getReferenceColumn() {
        return referenceColumn;
    }

    public void setReferenceColumn(String referenceColumn) {
        this.referenceColumn = referenceColumn;
    }

    public boolean isPrimaryKey() {
        return "PRIMARY_KEY".equalsIgnoreCase(type) || "primary key".equalsIgnoreCase(type);
    }


    public boolean isUnique() {
        return "UNIQUE".equalsIgnoreCase(type);
    }

    public boolean isForeignKey() {
        return "FOREIGN_KEY".equalsIgnoreCase(type);
    }

    public boolean isNotNull() {
        return "NOT_NULL".equalsIgnoreCase(type);
    }

    // Returns the default value for the column, if any.
    public Object getDefault() {
        return defaultValue;
    }

    // Sets a default value for the column.
    public void setDefault(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("type", type);
        
        if (referenceTable != null) {
            json.put("referenceTable", referenceTable);
        }
        if (referenceColumn != null) {
            json.put("referenceColumn", referenceColumn);
        }
        if (defaultValue != null) {
            json.put("defaultValue", defaultValue.toString()); // Convert Object to String for JSON
        }
        
        return json;
    }
}