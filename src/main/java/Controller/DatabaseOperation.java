package Controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import Model.*;

public class DatabaseOperation {

	User user;

	public DatabaseOperation(User user) {
		this.user = user;
	}

	public boolean createDatabase(Database db) {

		String path = user.getHomeDirectory() + "/" + db.getDabaseName();

		File folder = new File(path);

		if (folder.exists()) {
			return false;
		} else {
			if (folder.mkdir()) {
				return true;
			}
		}
		return false;
	}

	static boolean deleteFolder(String folderPath) throws IOException {
		Path folder = Paths.get(folderPath);
		if (Files.exists(folder)) {
			Files.walk(folder).sorted((a, b) -> b.compareTo(a)).forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
//                         throw new RuntimeException("Failed to delete: " + path, e);
				}
			});
			return true;
		}

		return false;

	}

	public boolean dropDatabase(String dbName) {

		System.out.println("hello");
		for (String db : user.getDatabases()) {

			System.out.println(db);
			if (db.equals(dbName)) {
				System.out.println(db);
				try {
					if (deleteFolder(user.getHomeDirectory() + "/" + dbName)) {
						user.getDatabases().remove(db);
						return true;
					}
					return false;
				} catch (Exception e) {
					return false;
				}
			}
		}
		return false;
	}

	private static void writeMetaData(ByteBuffer buffer, String value) {
		byte[] stringBytes = value.getBytes(StandardCharsets.UTF_8);
		buffer.putInt(stringBytes.length);
		buffer.put(stringBytes);
	}


	public boolean createTable(String databaseName, String tableName, List<Column> columns) {
		System.out.println("hjadIUGFCYKSDAVU");
		if (!user.getDatabases().contains(databaseName)) {
			System.out.println("Database does not exist: " + databaseName);
			return false;
		}

		if (new Database(databaseName).getTables(user.getUsername()).contains(tableName)) {
			System.out.println("Table already exists: " + tableName);
			return false;
		}

		Table newTable = new Table(tableName, columns);

		String tableFolderPath = user.getHomeDirectory() + "/" + databaseName + "/" + tableName;

		File tableFolder = new File(tableFolderPath);
		
		boolean autCheck=true;

		if (!tableFolder.exists() && tableFolder.mkdir()) {
			try (FileChannel channel = FileChannel.open(Paths.get(tableFolderPath + "/Metadata"),
					StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

				ByteBuffer buffer = ByteBuffer.allocate(1024);

				boolean primaryKeyCheck = false;

				for (Column c : columns) {
					if (c.getName().equals("Metadata") || c.getName().contains("metadata")) {
						deleteFolder(tableFolderPath);
						return false;
					}

					boolean check = true;
                    if(c.getConstraints()!=null){
					for (Constraint constraint : c.getConstraints()) {
						if (constraint.getType() != null && constraint.equals("PK")) {
							if (!primaryKeyCheck) {
								primaryKeyCheck = true;
							} else {
								deleteFolder(tableFolderPath);
								return false;
							}
						}
						else if (constraint.getType() != null && constraint.getType().equals("AUT")) {
							autCheck=false;
							if (!c.getDataType().equals("INT") && !c.getDataType().equals("FLOAT"))  {
								deleteFolder(tableFolderPath);
								System.out.println("hello");
								return false;
							}
							else {
								for (Constraint con : c.getConstraints()) {
									if(con.getType().equals("PK") || con.getType().equals("UK")) {
										autCheck=true;
										break;
									}
								}
							}

						}
						else if (constraint.getType() != null && constraint.getType().equals("FK")) {
							if (constraint.getReferenceTable().equals(tableName)
									&& constraint.getReferenceColumn().equals(c.getName())) {
								deleteFolder(tableFolderPath);
								return false;
							}
							loop: for (String tabName : new Database(databaseName).getTables(user.getUsername())) {
								List<Column> columnsCheck = new TableDAO(user, databaseName, tabName).columnsArray;
								for (Column col : columnsCheck) {
									for (Constraint constr : col.getConstraints()) {
										if (!constr.getType().equals("PK")
												|| !col.getDataType().equals(c.getDataType())) {
											deleteFolder(tableFolderPath);
											return false;
										}
									}
								}
							}
						}
					}}

                    if(!autCheck) {
                    	deleteFolder(tableFolderPath);
                    	return false;
                    }
					String filePath = (tableFolderPath + "/" + c.getName());

					if (Files.exists(Paths.get(filePath))) {
						deleteFolder(tableFolderPath);
						return false;
					} else {
						try (FileOutputStream fos = new FileOutputStream(filePath)) {
				           
				        } catch (IOException e) {
				            e.printStackTrace();
				        }
					}

					writeMetaData(buffer, c.getName()); // Writing column name
					writeMetaData(buffer, c.getDataType()); // Writing column data type
					if (c.getDataType().equals("STRING") || c.getDataType().equals("BLOB")) {
						try (FileOutputStream fos = new FileOutputStream(tableFolderPath + "/" + c.getName() + "_metadata")) {
				        } catch (IOException e) {
				            e.printStackTrace();
				        }
					}

					// Serialize Constraint object
					try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
							ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

						objectOutputStream.writeObject(c.getConstraints()); // Serialize the list
						objectOutputStream.flush();

						byte[] constraintBytes = byteArrayOutputStream.toByteArray();
						ByteBuffer constraintBuffer = ByteBuffer.wrap(constraintBytes);

						// Ensure there's enough space in buffer
						if (buffer.remaining() < constraintBuffer.remaining()) {
							buffer.flip();
							channel.write(buffer);
							buffer.clear();
						}

						buffer.putInt(constraintBytes.length); // Store length
						buffer.put(constraintBuffer); // Store actual data
					}
				}

				buffer.flip();
				while (buffer.hasRemaining()) {
					channel.write(buffer);
				}
				buffer.clear();
				return true;

			} catch (IOException e) {
	 	        System.out.println("Error writing metadata: " + e.getMessage());
				return false;
			}
		}
		try {
			deleteFolder(tableFolderPath);
		} catch (Exception e) {
			// TODO: handle exception
		}

		return false;
	}

	public boolean dropTable(String dbName, String tableName) {
		for (String db : user.getDatabases()) {
			if (db.equals(dbName)) {
				if (new Database(db).getTables(user.getUsername()).contains(tableName)) {
					try {
						if (deleteFolder(user.getHomeDirectory() + "/" + dbName + "/" + tableName)) {
							return true;
						}
						return false;
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			}
		}
		return false;
	}

}
