package Model;

import java.util.ArrayList;
import java.util.List;

public class Table {
    private String name;
    public List<Column> columns;
    private List<Row> rows;

    public Table(String name, List<Column> columns) {
        this.name = name;
        this.columns = columns;
        this.rows =new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }
    
    @Override
    public String toString() {
        return "Table{name='" + name + "', columns=" + columns + ", rows=" + rows +"}";
    }
}

