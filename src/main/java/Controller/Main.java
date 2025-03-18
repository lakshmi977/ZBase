package Controller;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import Model.*;

public class Main {

	public static <E> void main(String[] args) throws IOException {

		User user = new User( "sabari", "sabari@gmail.com", "124");
		DatabaseOperation dbOperation = new DatabaseOperation(user);
		Database db = new Database("Sabari1234");

		System.out.println("Creating database: " + dbOperation.createDatabase(db));

		List<Constraint> constraints = new ArrayList<>();
		constraints.add(new Constraint(null));
		constraints.add(new Constraint("PK"));
		constraints.add(new Constraint("NN"));

		List<Constraint> constraints1 = new ArrayList<>();
		constraints1.add(new Constraint("UK"));
		constraints1.add(new Constraint("NN"));
//        

		List<Column> listofColumns = Arrays.asList(new Column("id", "FLOAT", null),
				new Column("name", "STRING", null), new Column("age", "INT", constraints1));

		
		System.out.println(
				"Creating Employee table: " + dbOperation.createTable("Sabari1234", "Employee", listofColumns));
		TableDAO employeeTableDAO1 = new TableDAO(user, "Sabari1234", "Employee");

//		TableDAO<E> tableDAO=new TableDAO<E>(user, "Sabari1234","Employee" );
		
		System.out.println(employeeTableDAO1.columnsArray.size());
		
//		
		HashMap<String,E> columns=new HashMap<String,E>();
//		columns.put("id",36);
		columns.put("name",(E) "raji");
//		columns.put("age",20);
		 
		System.out.println("insert=======================" +employeeTableDAO1.insertValue(columns));
		System.out.println(employeeTableDAO1.addConstraint("id",new Constraint("UK")));
		System.out.println(employeeTableDAO1.dropConstraint("id","UK"));
		
//		String key = Base64.getEncoder()
//				.encodeToString(Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded());
//		System.out.println("Generated Key: " + key);

//		String dateString = "2025-02-11";  // Adjust this to match your format
//        
//        // Define the format of the date string
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  // Match this pattern with your string format
//        Date date=null;
//        try {
//            // Parse the string into a Date object
//            date = dateFormat.parse(dateString);
//
//        } catch (ParseException e) {
//            System.out.println("Error parsing the date: " + e.getMessage());
//        }
//		
//		User user=new User("Sabari", "sabari","sabari@gamil.com", date);
//		
//		
//		DatabaseOperation db=new DatabaseOperation(user);
//		Database dbDatabase=new Database("Sabari1234");
//		
//		System.out.println(db.createDatabase(dbDatabase));
//		
//		
//		List<Column> list=Arrays.asList(new Column("id", "Integer", false, null),new Column("Name", "String", false, new Constraint("Try", "Unique",null, null, null)));
//		
//		System.out.println(db.createTable("Sabari1234", "Employee",list));
////		
//		TableDAO table=new TableDAO(user,"Sabari1234","Employee");
//		
//		System.out.println(table.insertValue("id",1));
//		System.out.println(table.insertValue("Name",1));
//		System.out.println(table.insertValue("Name","Sabari"));
	}

}

















