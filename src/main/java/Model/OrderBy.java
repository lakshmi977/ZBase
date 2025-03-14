package Model;

public class OrderBy {

	private String  columnName;
	private String  orderDirection;
	
	public OrderBy(String columnName, String orderDirection) {
		super();
		this.columnName = columnName;
		this.orderDirection = orderDirection;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getOrderDirection() {
		return orderDirection;
	}

	public void setOrderDirection(String orderDirection) {
		this.orderDirection = orderDirection;
	}
	
}