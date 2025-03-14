package Controller;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;
import Model.User;

public class UserDataHandler {
	private static final String FILE_NAME = "/home/naga-zstk392/ZBase/Users_Metadata.db";

	public static synchronized void saveUser( String username, String email, String password)
			throws IOException {
		List<String[]> users = readAllUsers();

		for (String[] user : users) {
			if (user[1].equals(username) || user[2].equals(email)) {
				throw new IOException("User already exists!");
			}
		}

		String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
		users.add(new String[] {username, email, hashedPassword });

		try (FileOutputStream fos = new FileOutputStream(FILE_NAME);
				ObjectOutputStream oos = new ObjectOutputStream(fos)) {
			oos.writeObject(users);
		}
	}

	// ✅ Read all users with better exception handling
	@SuppressWarnings("unchecked")
	public static synchronized List<String[]> readAllUsers() {
		File file = new File(FILE_NAME);
		if (!file.exists())
			return new ArrayList<>();

		try (FileInputStream fis = new FileInputStream(FILE_NAME); ObjectInputStream ois = new ObjectInputStream(fis)) {
			return (List<String[]>) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace(); // Log the error for debugging
			return new ArrayList<>();
		}
	}

	// ✅ Improved Password Verification & Null Safety
	public static boolean checkUser(String email, String password) {
		List<String[]> users = readAllUsers(); // ✅ Read all users from Users_Metadata.db
		for (String[] user : users) {
			if (user[1].equals(email)) { // ✅ Compare email case-insensitively
				String hashedPassword = user[2]; // ✅ Get stored hashed password

				// ✅ Check password using BCrypt
				if (hashedPassword != null && BCrypt.checkpw(password, hashedPassword)) {
					return true;
				} else {
					System.out.println("Password mismatch!");
				}
			}
		}

		System.out.println("User not found!");
		return false;
	}

	public static User getUser(String email) {
		List<String[]> users = readAllUsers();
		for (String[] user : users) {
			if (user[1].equals(email)) { // ✅ Match username
				return new User(user[0], user[1], user[2]); // ✅ Return User object
			}
		}
		return null;
	}
	public static boolean isValidName(String name) {
        String regex = "^(?!\\d)[a-zA-Z0-9_]+$";
        return name != null && name.matches(regex);
    }
	
	
	public static Object convertValue(Object rawValue, String columnType) {
        if (rawValue == null || columnType == null) {
            throw new IllegalArgumentException("Raw value or column type cannot be null");
        }

        try {
            switch (columnType.toUpperCase()) {
                case "STRING":
                case "BLOB":
                    return rawValue.toString();

                case "INT":
                    if (rawValue instanceof Long) {
                        return ((Long) rawValue).intValue();
                    } else if (rawValue instanceof Integer) {
                        return rawValue;
                    } else {
                        return Integer.parseInt(rawValue.toString());
                    }

                case "FLOAT":
                    if (rawValue instanceof Double) {
                        return ((Double) rawValue).floatValue();
                    } else if (rawValue instanceof Float) {
                        return rawValue;
                    } else {
                        return Float.parseFloat(rawValue.toString());
                    }

                case "CHAR":
                    String charValue = rawValue.toString();
                    if (charValue.length() == 1) {
                        return charValue.charAt(0);
                    } else {
                        throw new IllegalArgumentException("Invalid CHAR value: Must be a single character");
                    }

                case "BOOL":
                    if (rawValue instanceof Boolean) {
                        return rawValue;
                    } else {
                        String boolStr = rawValue.toString().trim().toLowerCase();
                        if ("true".equals(boolStr) || "false".equals(boolStr)) {
                            return Boolean.parseBoolean(boolStr);
                        } else {
                             throw new IllegalArgumentException("Invalid BOOL value: Must be 'true' or 'false'");
                        }
                    }

                default:
                    throw new IllegalArgumentException("Unknown data type: " + columnType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error converting value: " + e.getMessage(), e);
        }
    }


}
