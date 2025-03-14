package Model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import Controller.TableDAO;


public class Database {
	String name;
	
	public Database(String name) {
		this.name=name;
	}
	
	public String getDabaseName() {
		return name;
	}

	public ArrayList<String> getTables(String userName){
		ArrayList<String> tableName=new ArrayList<>();
		File parentFolder = new File("/home/naga-zstk392/ZBase/"+userName+"/"+name);
        if (parentFolder.exists() && parentFolder.isDirectory()) {
            File[] folders = parentFolder.listFiles(File::isDirectory);
            if (folders != null) {
                for (File folder : folders) {
                    tableName.add(folder.getName());
                }
            }
        }
		return tableName;
	}
	
	public <E> HashMap<String, ArrayList<String>> getTablePrimaryKey(User user) {
	    HashMap<String, ArrayList<String>> result = new HashMap<>();
	    
	    for (String tableName : getTables(user.getUsername())) {
	        ArrayList<String> arrayList = new ArrayList<>();
	        TableDAO tableDAO = new TableDAO(user, this.name, tableName);
	        loop:
	        for (Column column :(ArrayList<Column>) tableDAO.columnsArray) {
	            for (Constraint constraint : column.getConstraints()) {
	                if (constraint.getType() != null && constraint.getType().equals("PK")) {
	                    arrayList.add(column.getName());
	                    arrayList.add(column.getDataType());
	                    result.put(tableName, arrayList);
	                    break loop;
	                }
	            }
	        }
	    }
	    return result;
	}

}
