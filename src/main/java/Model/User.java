package Model;

import java.io.*;
import java.util.ArrayList;

import org.mindrot.jbcrypt.BCrypt;

public class User implements Serializable {
	private static final long serialVersionUID = 1L;

	private String username;
	private transient String password; // `transient` prevents serialization
	private String email;
	private String homeDirectory;

	
	public User(String userName, String email, String password) {
		
	System.out.println(" hell0");
		this.username=userName;
		this.email = email;
		this.password = BCrypt.hashpw(password, BCrypt.gensalt()); // Use BCrypt
		this.homeDirectory = "/home/naga-zstk392/ZBase/" + userName;
		File folder = new File(homeDirectory);
		if (!folder.exists()) {
			folder.mkdirs();
		}
	}

//    public void saveUser() {
//        String filePath = "/home/sabari-zstk369/Damaal/" + username + "/userData.ser";
//        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
//            oos.writeObject(this);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public String getHomeDirectory() {
		return homeDirectory;
	}

	public String getPassword() {
		return password;
	}

//    public JSONObject toJson() {
//        JSONObject json = new JSONObject();
//        json.put("username", username);
//        json.put("email", email);
//        json.put("name", name);
//        json.put("homeDirectory", homeDirectory);
//        json.put("databases", databases.keySet()); // Include database names
//        return json;
//    }

	public ArrayList<String> getDatabases() {
		ArrayList<String> tableName = new ArrayList<>();
		File parentFolder = new File("/home/naga-zstk392/ZBase/"+ username);
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
}
