package Controller;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import Model.Column;
import Model.Constraint;
import Model.Database;
import Model.User;
import io.jsonwebtoken.io.IOException;

public class Main1 {

	public static void main(String[] args) throws java.io.IOException {
		
		System.out.println();

//		User user = new User("Sabari", "sabari", "sabari@gmail.com", "asdfghjk");
//		DatabaseOperation dbOperation = new DatabaseOperation(user);
//		Database db = new Database("Sabari1234");
//
//		System.out.println("Creating database: " + dbOperation.createDatabase(db));
//
//		List<Column> employeeColumns = Arrays.asList(new Column("name", "String", new Constraint("Try", 1)),
//
//				new Column("place", "String", new Constraint("Try"))
//
//		);
//		System.out.println(
//				"Creating Employee table: " + dbOperation.createTable("Sabari1234", "Employee", employeeColumns));
//
//		TableDAO employeeTableDAO = new TableDAO(user, "Sabari1234", "Employee");
//
//		HashMap<String, Object> listofColumns = new HashMap<String, Object>();
//
//		listofColumns.put("name","ghj");
//
//		System.out.println("=================================== " + employeeTableDAO.insertValue(listofColumns));
//		System.out.println("========== " + employeeTableDAO.readingFile("name_metadata", "name"));

	}

}
