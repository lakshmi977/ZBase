package Model;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

public class ConditionGroup {
	private String columnName;
    private String operator;
    private Object value;
    private String logicalOperator;
    
    public ConditionGroup(String columnName, String operator, Object value, String logicalOperator) {
		super();
		this.columnName = columnName;
		this.operator = operator;
		this.value = value;
		this.logicalOperator = logicalOperator;
	}
	
     public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getLogicalOperator() {
		return logicalOperator;
	}

	public void setLogicalOperator(String logicalOperator) {
		this.logicalOperator = logicalOperator;
	}
}
