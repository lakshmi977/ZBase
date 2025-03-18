package Model;

import java.util.*;

public class Row {
    private Map<String, Object> data;

    public Row() {
        this.data = new HashMap<>();
    }

    public void setColumnValue(String columnName, Object value) {
        data.put(columnName, value);
    }

    public Object getColumnValue(String columnName) {
        return data.get(columnName);
    }

    public Map<String, Object> getData() {
        return data;
    }
}
