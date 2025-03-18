package Controller;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.catalina.core.FrameworkListener;

import javax.crypto.Cipher;
import Model.*;

public class TableDAO<E> {
	private static final int INITIAL_CAPACITY = 1024;
	private static final int BOOLEAN_BYTES = 1;
	private static final SecretKey SECRET_KEY = new SecretKeySpec("nW2jF6Y8q9u$B&E)H@McQfTjWnZr4t7x".getBytes(), "AES");
	private User user;
	private Database database;
	private Table table;
	private String directory;
	public List<Column> columnsArray;
	private List<E> primarykey = new ArrayList<>();
	private HashMap<String, List<E>> uniqueKeys = new HashMap<>();
	private HashMap<String, List<E>> foreignKeys = new HashMap<>();
	private String tableName;

	public TableDAO(User user, String dbName, String tableName) {
		this.user = user;
		this.tableName = tableName;
		System.out.println("TABLE NAME:" + tableName + "   DBNAME  :" + dbName);
		this.directory = user.getHomeDirectory() + "/" + dbName + "/";
		this.columnsArray = readMetadata();
		System.out.println("METADATA:" + columnsArray);
		System.out.println(
				columnsArray.size() + "             size          ++++++++++++++++++++++++++++++++++++++          ");
		if (user.getDatabases().contains(dbName)) {
			this.database = new Database(dbName);
			if (database.getTables(user.getUsername()).contains(tableName)) {
				this.table = new Table(tableName, columnsArray);
			}
		}

		if (this.table != null) {
			for (Column col : columnsArray) {
				List<Constraint> listOfConstraint = col.getConstraints();

				for (Constraint constraint : listOfConstraint) {

					if (constraint != null) {

						if (constraint.getType() != null) {
							if (constraint.getType().equals("PK")) {
								primarykey = (List<E>) loadPrimaryKey(tableName + "/" + col.getName(),
										col.getDataType());

							} else if (constraint.getType().equals("UK")) {
								List<E> uniqueColumnValues = (List<E>) loadUniqueKey(col.getName(), col.getDataType());
								uniqueKeys.put(col.getName(), (List<E>) uniqueColumnValues);

							} else if (constraint.getType().equals("FK")) {
								List<E> foreignKeyValues = (List<E>) loadPrimaryKey(
										constraint.getReferenceTable() + "/" + constraint.getReferenceColumn(),
										col.getDataType());
								foreignKeys.put(constraint.getReferenceTable() + "_" + constraint.getReferenceColumn(),
										(List<E>) foreignKeyValues);
							}
						}
					}
				}
			}
		}
		System.out.println("00000000000000chekingg0000000000000t555555555555555555555555555555555555555");

	}

	public List<Column> readMetadata() {
		List<Column> columns = new ArrayList<>();
		Path metaPath = Paths.get(directory + tableName + "/" + "Metadata");

		if (!Files.exists(metaPath)) {
			System.out.println("fails here");
			return columns; // Return empty list if file doesn't exist
		}
		try (FileChannel channel = FileChannel.open(metaPath, StandardOpenOption.READ)) {
			ByteBuffer buffer = ByteBuffer.allocate(4096);
			channel.read(buffer);
			buffer.flip();

			while (buffer.hasRemaining()) {
				String columnName = readString(buffer);
				String dataType = readString(buffer);
				Object constraintObj = readObject(buffer);

				List<Constraint> constraints = new ArrayList<>();
				if (constraintObj instanceof List<?>) {
					constraints = (List<Constraint>) constraintObj;
				} else if (constraintObj instanceof Constraint) {
					constraints.add((Constraint) constraintObj);
				}
				System.out.println("ADDING");
				columns.add(new Column(columnName, dataType, constraints));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return columns;
	}

	private String readString(ByteBuffer buffer) {
		int length = buffer.getInt();
		byte[] bytes = new byte[length];
		buffer.get(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private List<Constraint> readObject(ByteBuffer buffer) {
		try {
			int length = buffer.getInt(); // Read stored length
			if (length <= 0)
				return new ArrayList<>();

			byte[] constraintBytes = new byte[length];
			buffer.get(constraintBytes); // Read serialized constraint bytes

			try (ObjectInputStream objectInputStream = new ObjectInputStream(
					new ByteArrayInputStream(constraintBytes))) {
				Object obj = objectInputStream.readObject();
				if (obj instanceof List<?>) {
					return (List<Constraint>) obj; // Type-safe cast
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return new ArrayList<>(); // Return empty list if deserialization fails

	}

	private List<E> loadPrimaryKey(String columnName, String datatype) {

		List<E> primaryKey = new ArrayList<>();

		Path filePath = Paths.get(directory + columnName);
		if (datatype.equals("STRING")) {

			primarykey = (List<E>) loadPrimaryStringFile(columnName + "_metadata", columnName);
			return primarykey;
		}

		if (!Files.exists(filePath)) {
			System.out.println(" File does not exist: " + filePath);
			return primarykey;
		}

		try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {

			System.out.println("primary key   " + filePath);

			ByteBuffer countBuffer = ByteBuffer.allocate(Long.BYTES);
			if (channel.read(countBuffer) < Long.BYTES) {
				return primarykey;
			}
			countBuffer.flip();
			long rowCount = countBuffer.getLong();

			System.out.println(" Row count in primary key: " + rowCount);

			for (int i = 0; i < rowCount; i++) {
				int rowSize = getRowSize(datatype);
				System.out.println("rowsize    " + rowSize);

				if (rowSize <= 0)
					continue;
				ByteBuffer rowBuffer = ByteBuffer.allocate(rowSize);
				if (channel.read(rowBuffer) < rowSize)
					break;
				rowBuffer.flip();

				long rowId = rowBuffer.getLong();
				byte isDeleted = rowBuffer.get();
				byte isNull = rowBuffer.get();

				if (isDeleted == 1 || isNull == 1)
					continue;

				System.out.println("rowid     " + rowId);

				System.out.println("isDeleted     " + isDeleted);
				System.out.println("isNull     " + isNull);

				E value = (E) readValue(rowBuffer, datatype);
				System.out.println("9999999999999999999     " + value);
				System.out.println("9999999999999999999     " + primaryKey);

				if (value != null) {
					primarykey.add((E) value);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return primarykey;
	}

	private int getRowSize(String dataType) {
		if (dataType.equalsIgnoreCase("INT") && dataType != null) {
			return Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Integer.BYTES;
		} else if (dataType.equalsIgnoreCase("FLOAT") && dataType != null) {
			return Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Double.BYTES;
		} else if (dataType.equalsIgnoreCase("BOOL") && dataType != null) {
			return Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES;
		} else if (dataType.equalsIgnoreCase("CHAR") && dataType != null) {
			return Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Character.BYTES;
		} else if (dataType.equalsIgnoreCase("STRING") && dataType != null) {
			return Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Long.BYTES + Integer.BYTES;
		} else if (dataType.equalsIgnoreCase("BLOB") && dataType != null) {
			return Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Long.BYTES + Integer.BYTES;
		}

		return -1;

	}

	private Object readValue(ByteBuffer buffer, String dataType) {
		if (dataType.equalsIgnoreCase("INT")) {
			return buffer.getInt();
		} else if (dataType.equalsIgnoreCase("FLOAT")) {
			return buffer.getDouble();
		} else if (dataType.equalsIgnoreCase("CHAR")) {
			return buffer.getChar();
		} else if (dataType.equalsIgnoreCase("BOOL")) {
			return buffer.get();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public boolean insertValue(HashMap<String, E> result) {

		if (!columnNameConstraintCheck(result)) {

			return false;
		}

		for (Entry<String, E> entry : result.entrySet()) {
			String columnName = entry.getKey();
			Object value = entry.getValue();
			String datatype = null;

//			System.out.println("Datatype  "+datatype);y

			for (int i = 0; i < columnsArray.size(); i++) {
				System.out.println("---------------------checking========================");

				if (columnsArray.get(i).getName().equals(columnName)) {
					if ("NONE".equals(value) || value == null) {
						datatype = columnsArray.get(i).getDataType();
					}
					if (value instanceof String && ((String) value).isEmpty()) {
						datatype = columnsArray.get(i).getDataType();

					}
					if (columnsArray.get(i).getName().equals(columnName)) {

						List<Constraint> listofConstraints = columnsArray.get(i).getConstraints();
						for (Constraint constraint : listofConstraints) {

							System.out.println(
									"---------------------checking========================  " + constraint.getType());

							if (constraint.getType() != null && constraint.getType().equals("DEF")) {

								if (value.equals("NONE")) {
									value = constraint.getDefault();
								}

							}
							if (constraint.getType() != null && constraint.getType().equals("AUT")
									&& value.equals("NONE")
									|| constraint.getType() != null && constraint.getType().equals("AUT")
											&& value.equals("")) {

//								value = getLastIntegerValue(columnName) + 1;
								
								List<E> listOfvalues=new ArrayList<>();
								
								listOfvalues=readColumnDataAsList(columnName, datatype);
								
								value=calculateMax(listOfvalues);
								value=(Integer)value+1;
								
								System.out.println("yyyyyyyyyyyyyyyyyy   " +value);
								

							}

						}
					}
					datatype = columnsArray.get(i).getDataType();
				}

			}

			ByteBuffer valueBuffer = loadBuffer(columnName, (E) value, datatype);
			if (valueBuffer != null && valueBuffer.hasRemaining()) {
				writeToTheFile(columnName, valueBuffer);
			}

		}
		return true;
	}

	public int getLastIntegerValue(String column) {
		int lastNumber = 0;
		Path path = Paths.get(directory + tableName + "/" + column);

		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {

			ByteBuffer headerBuffer = ByteBuffer.allocate(8);
			int bytesRead = channel.read(headerBuffer);
			if (bytesRead != 8) {
				System.out.println("Faile +++++++++++++++++d to read the complete header.");
				return lastNumber;
			}
			headerBuffer.flip();
			long rowCount = headerBuffer.getLong();
			System.out.println("Row count: " + rowCount);
			long position = ((rowCount - 1) * 14) + 10 + 8;
			if (position < 1) {
				position = 0;
			}

			channel.position(position);
			ByteBuffer readBuffer = ByteBuffer.allocate(4);
			channel.read(readBuffer);
			readBuffer.flip();
			lastNumber = readBuffer.getInt();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return lastNumber;
	}

	public boolean columnNameConstraintCheck(Map<String, E> setColumnValues) {
		boolean isvalid;
		columnsArray = readMetadata();
		Column column = null;

		HashMap<String, Boolean> listofColumns = new HashMap<String, Boolean>();
		for (Entry<String, E> entry : setColumnValues.entrySet()) {
			String columnName = entry.getKey();
			Object value = entry.getValue();

			for (Column col : columnsArray) {
				if (col.getName().trim().equalsIgnoreCase(columnName.trim())) {
					column = col;

					break; // Stop searching after finding the column
				}
			}

			if (column == null) {
				listofColumns.put(columnName, false);
				System.out.println("Check1");
				break;
			}

			if (column.getDataType().equals("BLOB")) {
				isvalid = true;
				listofColumns.put(columnName, isvalid);
				break;
			}

			List<Constraint> listOfConstraint = column.getConstraints();

			String constraint1 = "";
			String constraint2 = "";
			for (Constraint constraint : listOfConstraint) {

				if (constraint.getType().equals("PK")) {
					constraint1 = "PK";
				} else if (constraint.getType().equals("AUT")) {
					constraint2 = "AUT";
				}

			}

			if (constraint1.equals("PK") && constraint2.equals("AUT") && value.equals("NONE")
					|| constraint1.equals("PK") && constraint2.equals("AUT") && value.equals("")
					|| constraint2.equals("PK") && constraint1.equals("AUT") && value.equals("NONE")
					|| constraint2.equals("PK") && constraint1.equals("AUT") && value.equals("")) {
				isvalid = true;
//				System.out.println("Check5");

				listofColumns.put(columnName, isvalid);
			} else {

				for (Constraint constraint : listOfConstraint) {

					if (constraint.getType().equals("DEF") && value.equals("NONE")) {

						value = constraint.getType();

					}
					if (value != null) {
						// Avoid NullPointerException

						if (column.getDataType().equalsIgnoreCase("INT")
								&& !(value instanceof Integer || value.equals("NONE"))
								|| column.getDataType().equalsIgnoreCase("CHAR")
										&& !(value instanceof Character || value.equals("NONE"))
								|| column.getDataType().equalsIgnoreCase("BOOL")
										&& !(value instanceof Byte || value.equals("NONE"))
								|| column.getDataType().equalsIgnoreCase("FLOAT") && !(value instanceof Double
										|| value instanceof Integer || value.equals("NONE"))) {

							System.out.println(
									"******* " + column.getDataType() + "  **** " + constraint.getType() + "*" + value);

							isvalid = false;
							System.out.println("Check2----------");
							listofColumns.put(columnName, isvalid);
						}
					}

					if ("NN".equalsIgnoreCase(constraint.getType())) {
						if (value.equals("NONE")) {
							System.out.println("not null violation");
							isvalid = false;
							System.out.println("Check3");

							listofColumns.put(columnName, isvalid);

						}

					}
					if ("PK".equalsIgnoreCase(constraint.getType())) {

						if (value.equals("NONE")) {
							isvalid = false;
							System.out.println("Check4");

							System.out.println(
									"*************QQQQQQQQQQQQQQQQQQQQQQQQQQ*****************************************************");

							listofColumns.put(columnName, isvalid);
							break;
						}

						if (primarykey.contains(value)) {

							isvalid = false;
							System.out.println("Check5");

							listofColumns.put(columnName, isvalid);

						}

						System.out.println("primary key   " + primarykey);
						primarykey.add((E) value);

					}

					if ("UK".equalsIgnoreCase(constraint.getType())) {

						System.out.println("  constraint  type =========>   " + constraint.getType());

						System.out.println("Check6");

						boolean exists = false;

						System.out.println("Unique   keuysssssssss  => " + uniqueKeys);

						for (List<E> valuesList : uniqueKeys.values()) {
							if (valuesList.contains(value)) {
								exists = true;
								break;
							}
						}

						if (exists) {
							System.out.println("Check17");

							isvalid = false;
							listofColumns.put(columnName, isvalid);

						}

						uniqueKeys.computeIfAbsent(columnName, k -> new ArrayList<>()).add((E) value);

					}
					if ("FK".equalsIgnoreCase(constraint.getType()) && value.equals("NONE")) {
						System.out.println("------------   " + value);

						listofColumns.put(columnName, true);
						break;
					}

					if ("FK".equalsIgnoreCase(constraint.getType()) && value != null) {

						System.out.println("Foreign key validation started for value: ===========" + value);
						String referencedTable = constraint.getReferenceTable();
						String referencedColumn = constraint.getReferenceColumn();

						String datatype = returntype(value);

						if (referencedTable == null || referencedColumn == null) {
							System.out.println("Invalid foreign key constraint in column " + columnName);
							isvalid = false;
							listofColumns.put(columnName, isvalid);

						}

						boolean exists = false;

						System.out.println(" Foreign keys -----------------  " + foreignKeys);

						for (List<E> valuesList : foreignKeys.values()) {
							if (valuesList.contains(value)) {

								exists = true;
								break;
							}
						}

						if (!exists) {
							System.out.println("Foreign key violation");
							isvalid = false;
							listofColumns.put(columnName, isvalid);
						}
						foreignKeys.computeIfAbsent(referencedTable + "_" + referencedColumn, k -> new ArrayList<>())
								.add((E) value);

					}

				}
			}
		}

		for (HashMap.Entry<String, Boolean> map : listofColumns.entrySet()) {
			if (map.getValue().equals(false)) {

				return false;
			}

		}

//		System.out.println("check datatytpe   "+column.getDataType());
		return true;
	}

//	private Column findColumnByName(String columnName) {
//		// Ensure columnsArray is always up-to-date
//		this.columnsArray = readMetadata();
//
//		return columnsArray.stream().filter(col -> col.getName().equalsIgnoreCase(columnName)).findFirst().orElse(null);
//	}

	private ByteBuffer loadBuffer(String columnName, E value, String datatype) {
		ByteBuffer valueBuffer = null;
		System.out.println("check1");

		if ("BLOB".equals(datatype)) {
			try {
				System.out.println("check2");
				saveImage(columnName, value);

			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		if (datatype.equals("STRING")) {
			String strValue = String.valueOf(value); // Convert safely
			System.out.println(
					"check5=================================================================================================");

			textAndImageMetadata(columnName, (E) strValue);
			if (!strValue.equals("NONE")) {
				byte[] stringBytes = strValue.getBytes(StandardCharsets.UTF_8);
				valueBuffer = ByteBuffer.wrap(stringBytes);
			}
//
//           textAndImageMetadata(columnName, value);
			return valueBuffer;

		}

		if (value == null || "NONE".equals(value) && datatype.equals("STRING")) {
			if ("STRING".equals(datatype)) {
				textAndImageMetadata(columnName, (E) value);
				return null;
			}

			long rowId = getRowCount(columnName);
			boolean isDeleted = false;
			int capacity = getRowSize(datatype);
			valueBuffer = ByteBuffer.allocate(capacity);
			valueBuffer.putLong(rowId);
			valueBuffer.put((byte) (isDeleted ? 1 : 0));
			valueBuffer.put((byte) 1);
			valueBuffer.putInt(0);
			return valueBuffer;
		}

		if (value instanceof String && ((String) value).isEmpty() && datatype.equals("STRING")) {

			System.out.println("check6");
			textAndImageMetadata(columnName, (E) "");
			return null;
		}

		long rowId = getRowCount(columnName);
		boolean isDeleted = false;
		valueBuffer = ByteBuffer.allocate(INITIAL_CAPACITY);

		if (datatype.equals("FLOAT")) {
			if (value.equals("NONE")) {
				valueBuffer = ByteBuffer.allocate(Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Double.BYTES);
				valueBuffer.putLong(rowId);
				valueBuffer.put((byte) (isDeleted ? 1 : 0));
				valueBuffer.put((byte) 1);
				valueBuffer.putDouble(0);
			}
			if (value instanceof Double) {
				valueBuffer = ByteBuffer.allocate(Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Double.BYTES);
				valueBuffer.putLong(rowId);
				valueBuffer.put((byte) (isDeleted ? 1 : 0));
				valueBuffer.put((byte) 0);
				valueBuffer.putDouble((Double) value);
			}
		} else if (datatype.equals("INT")) {
			System.out.println("check3");
			if (value.equals("NONE")) {
				System.out.println("check2");
				valueBuffer = ByteBuffer.allocate(Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Integer.BYTES);
				valueBuffer.putLong(rowId);
				valueBuffer.put((byte) (isDeleted ? 1 : 0));
				valueBuffer.put((byte) 1);
				valueBuffer.putInt(0);
			} else if (value instanceof Integer) {
				valueBuffer = ByteBuffer.allocate(Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Integer.BYTES);
				valueBuffer.putLong(rowId);
				valueBuffer.put((byte) (isDeleted ? 1 : 0));
				valueBuffer.put((byte) 0);
				valueBuffer.putInt((Integer) value);
			} else if (value.equals("")) {
				valueBuffer = ByteBuffer.allocate(Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Integer.BYTES);
				valueBuffer.putLong(rowId);
				valueBuffer.put((byte) (isDeleted ? 1 : 0));
				valueBuffer.put((byte) 0);
				System.out.println(" valiues" + value + "naga");
				valueBuffer.putInt((Integer) value);
			}
		}
		if (datatype.equals("BYTE")) {

			if (value.equals("NONE")) {
				valueBuffer = ByteBuffer.allocate(Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES);
				valueBuffer.putLong(rowId);
				valueBuffer.put((byte) (isDeleted ? 1 : 0));
				valueBuffer.put((byte) 1);
				valueBuffer.put((byte) 0);

			}

			else if (value instanceof Byte) {
				valueBuffer = ByteBuffer.allocate(Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES);
				valueBuffer.putLong(rowId);
				valueBuffer.put((byte) (isDeleted ? 1 : 0));
				valueBuffer.put((byte) 0);
				valueBuffer.put((byte) value);
			}
		}
		if (datatype.equals("CHAR")) {
			if (value.equals("NONE")) {
				valueBuffer = ByteBuffer.allocate(Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Character.BYTES);
				valueBuffer.putLong(rowId);
				valueBuffer.put((byte) (isDeleted ? 1 : 0));
				valueBuffer.put((byte) 1);
				valueBuffer.putChar((char) 0);
			}

			else if (value instanceof Character) {
				valueBuffer = ByteBuffer.allocate(Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Character.BYTES);
				valueBuffer.putLong(rowId);
				valueBuffer.put((byte) (isDeleted ? 1 : 0));
				valueBuffer.put((byte) 0);
				valueBuffer.putChar((char) value);
			}
		}
		valueBuffer.flip();
		return valueBuffer;
	}

	public <E> ArrayList<E> readingFile1(String metadataFile, String dataFile) {
		ArrayList<E> result = new ArrayList<E>();

		try (FileChannel metaChannel = FileChannel.open(Paths.get(directory + tableName + "/" + metadataFile),
				StandardOpenOption.READ);
				FileChannel dataChannel = FileChannel.open(Paths.get(directory + tableName + "/" + dataFile),
						StandardOpenOption.READ)) {

			ByteBuffer countBuffer = ByteBuffer.allocate(Long.BYTES);
			if (metaChannel.read(countBuffer) < Long.BYTES)
				return result;
			countBuffer.flip();
			long rowCount = countBuffer.getLong();

			System.out.println("rowCount for unique strings: " + rowCount);

			for (int i = 0; i < rowCount; i++) {
				int rowSize = getRowSize("String");
				if (rowSize <= 0)
					continue;

				ByteBuffer rowBuffer = ByteBuffer.allocate(rowSize);
				if (metaChannel.read(rowBuffer) < rowSize)
					break;
				rowBuffer.flip();

				long rowId = rowBuffer.getLong();
				byte isDeleted = rowBuffer.get();
				byte isNull = rowBuffer.get();

				if (isDeleted == 1)
					continue;
				if (isNull == 1) {
					result.add(null);
					continue;
				}

				long offset = rowBuffer.getLong();
				int length = rowBuffer.getInt();

				if (length == 0) {
					result.add((E) "");
					continue;
				}

				ByteBuffer dataBuffer = ByteBuffer.allocate(length);
				dataChannel.position(offset);
				dataChannel.read(dataBuffer);
				dataBuffer.flip();
				String actualString = new String(dataBuffer.array(), StandardCharsets.UTF_8);

				System.out.println("Unique String Value: " + actualString);
				result.add((E) actualString);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("+++++++++++++++    " + result);
		return result;
	}

	public <E> ArrayList<E> readingFile2(String metadataFile, String dataFile) {
		ArrayList<E> result = new ArrayList<E>();

		try (FileChannel metaChannel = FileChannel.open(Paths.get(directory + tableName + "/" + metadataFile),
				StandardOpenOption.READ);
				FileChannel dataChannel = FileChannel.open(Paths.get(directory + tableName + "/" + dataFile),
						StandardOpenOption.READ)) {

			ByteBuffer countBuffer = ByteBuffer.allocate(Long.BYTES);
			if (metaChannel.read(countBuffer) < Long.BYTES)
				return result;
			countBuffer.flip();
			long rowCount = countBuffer.getLong();

			System.out.println("rowCount for unique strings: " + rowCount);

			for (int i = 0; i < rowCount; i++) {
				int rowSize = getRowSize("String");
				if (rowSize <= 0)
					continue;

				ByteBuffer rowBuffer = ByteBuffer.allocate(rowSize);
				if (metaChannel.read(rowBuffer) < rowSize)
					break;
				rowBuffer.flip();

				long rowId = rowBuffer.getLong();
				byte isDeleted = rowBuffer.get();
				byte isNull = rowBuffer.get();

				if (isDeleted == 1)
					continue;
				if (isNull == 1) {
					result.add(null);
					continue;
				}

				long offset = rowBuffer.getLong();
				int length = rowBuffer.getInt();

				if (length == 0) {
					result.add((E) "");
					continue;
				}

				ByteBuffer dataBuffer = ByteBuffer.allocate(length);
				dataChannel.position(offset);
				dataChannel.read(dataBuffer);
				dataBuffer.flip();
				String actualString = new String(dataBuffer.array(), StandardCharsets.UTF_8);

				System.out.println("Unique String Value: " + actualString);
				result.add((E) actualString);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("+++++++++++++++    " + result);
		return result;
	}

	public void textAndImageMetadata(String fileName, Object value) {
		ByteBuffer buffer = ByteBuffer
				.allocate(Long.BYTES + BOOLEAN_BYTES + BOOLEAN_BYTES + Integer.BYTES + Long.BYTES);
		System.out.println("valllllllllllllllllllllllllueee   " + value);
		try (FileChannel channel = FileChannel.open(Paths.get(directory + tableName + "/" + fileName + "_metadata"),
				StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {

			long rowId = getRowCount(fileName + "_metadata");
			long offset = getOffset(fileName, value);
			boolean isDeleted = false;

			int dataLength;
			if (value instanceof byte[]) {
				dataLength = ((byte[]) value).length;

				System.out.println("dddddddddddddddddataa Length  =   " + dataLength);

			} else if (value instanceof String) {
				dataLength = ((String) value).getBytes(StandardCharsets.UTF_8).length;
			} else {
				throw new IllegalArgumentException("Unsupported data type: " + value.getClass().getSimpleName());
			}

			// Handle NULL or EMPTY cases
			if (value == null || "NONE".equals(value)) {
				buffer.putLong(rowId);
				buffer.put((byte) (isDeleted ? 1 : 0));
				buffer.put((byte) 1); // NULL flag
				buffer.putLong(offset);
				buffer.putInt(0);
				buffer.flip();
				channel.write(buffer);
				return;
			}

			if (value.equals("")) {
				buffer.putLong(rowId);
				buffer.put((byte) (isDeleted ? 1 : 0));
				buffer.put((byte) 0);
				buffer.putLong(offset);
				buffer.putInt(0);
				buffer.flip();
				channel.write(buffer);
				return;
			}

			buffer.putLong(rowId);
			buffer.put((byte) (isDeleted ? 1 : 0));
			buffer.put((byte) 0); // Not NULL
			buffer.putLong(offset);
			buffer.putInt(dataLength);
			buffer.flip();
			channel.write(buffer);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveImage(String column, E value) throws Exception {
		byte[] dataToEncrypt;

		System.out.println("Save image");

		if (value instanceof byte[]) {
			dataToEncrypt = (byte[]) value;
		} else if (value instanceof String) {
			dataToEncrypt = Base64.getDecoder().decode((String) value);
		} else {
			throw new IllegalArgumentException("Unsupported data type: " + value.getClass().getSimpleName());
		}

		byte[] encryptedData = encryptData(dataToEncrypt, SECRET_KEY);

		Path filePath = Paths.get(directory, tableName, column);
		Files.createDirectories(filePath.getParent());

		try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
				StandardOpenOption.APPEND)) {

			ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
			System.out.println("Encrypted Data Length: " + encryptedData.length);

			System.out.println("Encrypted Data (Base64): " + Base64.getEncoder().encodeToString(encryptedData));

			while (buffer.hasRemaining()) {
				fileChannel.write(buffer);
			}
		}

		textAndImageMetadata(column, encryptedData);
	}

	private static byte[] encryptData(byte[] data, SecretKey key) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

		SecureRandom secureRandom = new SecureRandom();
		byte[] iv = new byte[16];
		secureRandom.nextBytes(iv);

		IvParameterSpec ivSpec = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

		byte[] encryptedData = cipher.doFinal(data);

		ByteBuffer byteBuffer = ByteBuffer.allocate(16 + encryptedData.length);
		byteBuffer.put(iv);
		byteBuffer.put(encryptedData);

		return byteBuffer.array();
	}

	public static byte[] decryptData(byte[] encryptedData, SecretKey key) throws Exception {
		if (encryptedData.length < 16) {
			throw new IllegalArgumentException("Invalid encrypted data: Too short to contain IV.");
		}

		ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);

		byte[] iv = new byte[16];
		byteBuffer.get(iv); // Read IV first

		byte[] cipherText = new byte[byteBuffer.remaining()];
		byteBuffer.get(cipherText); // Read actual encrypted data

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

		return cipher.doFinal(cipherText);
	}

	public long getRowCount(String columnFile) {
		Path filePath = Paths.get(directory + tableName + "/" + columnFile);

		try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ, StandardOpenOption.WRITE,
				StandardOpenOption.CREATE)) {
			ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
			if (channel.size() < Long.BYTES) {
				System.out.println("Initializing row count...");
				buffer.putLong(1).flip();
				channel.write(buffer);
				return 1;
			}
			channel.read(buffer);
			buffer.flip();
			long rowCount = buffer.getLong();
			if (rowCount < 1) {
				System.out.println("Corrupt row count detected, resetting to 1.");
				rowCount = 1;
			}
			buffer.clear();
			buffer.putLong(rowCount + 1).flip();
			channel.position(0).write(buffer);

			return rowCount + 1;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public long getOffset(String column, Object data) {
		Path filePath = Paths.get(directory + tableName + "/" + column + "_metadata");

		int dataLength;

		if (data == null || data.equals("NONE")) {
			dataLength = 0;
		}

		else if (data instanceof byte[]) {
			dataLength = ((byte[]) data).length; // Get length of byte array
		} else if (data instanceof String) {
			dataLength = ((String) data).getBytes(StandardCharsets.UTF_8).length; // Convert to bytes for accurate size
		} else {
			throw new IllegalArgumentException("Unsupported data type: " + data.getClass().getSimpleName());
		}

		try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ, StandardOpenOption.WRITE,
				StandardOpenOption.CREATE)) {

			// Check if header is initialized (expects at least 16 bytes)
			if (channel.size() < 16) {
				ByteBuffer initBuffer = ByteBuffer.allocate(8);
				channel.position(8);
				initBuffer.putLong(dataLength); // Store the new offset as dataLength
				initBuffer.flip();
				channel.write(initBuffer);
				return 0;
			}

			// Read the current offset from the header (position 8 to 15)
			ByteBuffer readBuffer = ByteBuffer.allocate(8);
			channel.position(8);
			channel.read(readBuffer);
			readBuffer.flip();
			long currentOffset = readBuffer.getLong();

			// Calculate the new offset by adding the length of the new data.
			long newOffset = currentOffset + dataLength;

			// Write the updated offset back to position 8
			ByteBuffer offsetBuffer = ByteBuffer.allocate(8);
			offsetBuffer.putLong(newOffset);
			offsetBuffer.flip();
			channel.position(8);
			channel.write(offsetBuffer);

			// Return the original offset (where new data should be written)
			return currentOffset;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public int[] getRowIdAndOffset(String columnFile, String data) {
		Path filePath = Paths.get(directory + tableName + "/" + columnFile + "_metadata");

		System.out.println("FilePath: " + filePath);
		int[] array = new int[2]; // Stores rowCount and offset

		int dataLength = data.length();

		try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ, StandardOpenOption.WRITE,
				StandardOpenOption.CREATE)) {

			// 8 bytes for row count, 8 bytes for offset, 4 bytes for length = 20 bytes
			// total.
			ByteBuffer buffer = ByteBuffer.allocate(20);

			if (channel.size() < 20) { // First-time initialization
				System.out.println("Initializing row count, offset, and length...");
				buffer.putLong(1); // Row count = 1
				buffer.putLong(0); // Offset = 0 (start from beginning)
				buffer.putInt(0); // Length set to dataLength (or 0 if desired)
				buffer.flip();
				channel.write(buffer);

				array[0] = 1; // Initial row count
				array[1] = 0; // Initial offset
				return array;
			}

			// Read existing row count, offset, and length
			channel.read(buffer);
			buffer.flip();
			long rowCount = buffer.getLong();
			long offset = buffer.getLong();

			if (rowCount < 1) {
				System.out.println("Corrupt row count detected, resetting to 1.");
				rowCount = 1;
			}

			// For example, if you want to update the offset based on dataLength:
			long newOffset = dataLength - offset;

			System.out.println("ROWID: " + (rowCount + 1));
			System.out.println("Offset: " + newOffset);

			buffer.clear();
			buffer.putLong(rowCount + 1);
			buffer.putLong(newOffset);

			buffer.flip();
			channel.position(0);
			channel.write(buffer);

			// Store updated values in the array
			array[0] = (int) (rowCount + 1); // Row count
			array[1] = (int) newOffset; // Updated offset

			return array;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new int[] { 1, 0 }; // Error case
	}

	public List<E> loadPrimaryStringFile(String metadataFile, String dataFile) {
		List<E> primaryKey = new ArrayList<>();
		try (FileChannel metaChannel = FileChannel.open(Paths.get(directory + metadataFile), StandardOpenOption.READ);
				FileChannel dataChannel = FileChannel.open(Paths.get(directory + dataFile), StandardOpenOption.READ)) {

			ByteBuffer countBuffer = ByteBuffer.allocate(16);
			if (metaChannel.read(countBuffer) < 16)
				return (List<E>) primarykey;
			countBuffer.flip();
			long rowCount = countBuffer.getLong();
			for (int i = 0; i < rowCount; i++) {
				int rowSize = getRowSize("STRING");
				if (rowSize <= 0)
					continue;

				ByteBuffer rowBuffer = ByteBuffer.allocate(rowSize);
				int bytesRead = metaChannel.read(rowBuffer);
				if (bytesRead < rowSize) {
					break;
				}

				rowBuffer.flip();
				long rowId = rowBuffer.getLong();
				byte isDeleted = rowBuffer.get();
				byte isNull = rowBuffer.get();

				if (isDeleted == 1 || isNull == 1)
					continue;
				long offset = rowBuffer.getLong();
				int length = rowBuffer.getInt();

				ByteBuffer dataBuffer = ByteBuffer.allocate(length);
				dataChannel.position(offset);
				dataChannel.read(dataBuffer);
				dataBuffer.flip();
				String actualString = new String(dataBuffer.array(), StandardCharsets.UTF_8);

				System.out.println("actual String  " + actualString);

				primarykey.add((E) actualString);
				if (length == 0) {
					primarykey.add((E) "");
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return primarykey;
	}

	private HashMap<Object, String> splitStringByLength(String str, int chunkSize, String datafile) {
		HashMap<Object, String> primarykey = new HashMap<Object, String>();
		for (int i = 0; i < str.length(); i += chunkSize) {

			primarykey.put(str.substring(i, Math.min(i + chunkSize, str.length())), datafile);

			System.out.println("Substring   " + str.substring(i, Math.min(i + chunkSize, str.length())));

		}
		return primarykey;
	}

	private List<E> loadUniqueKey(String columnName, String datatype) {

		List<E> uniqueKey = new ArrayList<>();

		if (!datatype.equalsIgnoreCase("STRING")) {
			Path filePath = Paths.get(directory + tableName + "/" + columnName);

			System.out.println("[=============    " + filePath);

			try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
				ByteBuffer countBuffer = ByteBuffer.allocate(Long.BYTES);
				if (channel.read(countBuffer) < Long.BYTES)
					return uniqueKey;
				countBuffer.flip();
				long rowCount = countBuffer.getLong();

				System.out.println("Processing unique key for column: " + columnName);
				for (int i = 0; i < rowCount; i++) {
					int rowSize = getRowSize(datatype);
					if (rowSize <= 0)
						continue;

					ByteBuffer rowBuffer = ByteBuffer.allocate(rowSize);
					if (channel.read(rowBuffer) < rowSize)
						break;
					rowBuffer.flip();

					long rowId = rowBuffer.getLong();
					byte isDeleted = rowBuffer.get();
					byte isNull = rowBuffer.get();

					if (isDeleted == 1)
						continue;

					Object value = readValue(rowBuffer, datatype);

					uniqueKey.add((E) value);

				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {
			// Handle String data using metadata + data file
			uniqueKey = loadUniqueStringFile(columnName + "_metadata", columnName);
		}

		System.out.println("Uniqu--------------------------------------------------e key values for column "
				+ columnName + ": " + uniqueKey);
		return uniqueKey;
	}

	private List<E> loadUniqueStringFile(String metadataFile, String dataFile) {

		List<E> uniqueKey = new ArrayList<>();

//        System.out.println("999999999  "+())

		try (FileChannel metaChannel = FileChannel.open(Paths.get(directory + tableName + "/" + metadataFile),
				StandardOpenOption.READ);
				FileChannel dataChannel = FileChannel.open(Paths.get(directory + tableName + "/" + dataFile),
						StandardOpenOption.READ)) {

			ByteBuffer countBuffer = ByteBuffer.allocate(16);
			if (metaChannel.read(countBuffer) < 16)
				return uniqueKey;
			countBuffer.flip();
			long rowCount = countBuffer.getLong();

			System.out.println("rowCount for unique strings: " + rowCount);

			for (int i = 0; i < rowCount; i++) {
				int rowSize = getRowSize("STRING");
				if (rowSize <= 0)
					continue;

				ByteBuffer rowBuffer = ByteBuffer.allocate(rowSize);
				if (metaChannel.read(rowBuffer) < rowSize)
					break;
				rowBuffer.flip();

				long rowId = rowBuffer.getLong();
				byte isDeleted = rowBuffer.get();
				byte isNull = rowBuffer.get();

				if (isDeleted == 1)
					continue; // Ignore deleted rows
				if (isNull == 1)
					continue; // Allow NULL values (skip storing them)

				long offset = rowBuffer.getLong();
				int length = rowBuffer.getInt();

				ByteBuffer dataBuffer = ByteBuffer.allocate(length);
				dataChannel.position(offset);
				dataChannel.read(dataBuffer);
				dataBuffer.flip();
				String actualString = new String(dataBuffer.array(), StandardCharsets.UTF_8);

				System.out.println("Unique String Value: " + actualString);

				uniqueKey.add((E) actualString);
				if (length == 0) {

					uniqueKey.add((E) "");
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return uniqueKey;
	}

	public void writeToTheFile(String filename, ByteBuffer buffer) {
		Path filePath = Paths.get(directory + tableName + "/" + filename);
		File file = new File(filePath.toString());
//
//	    if (file.exists()) {
//	        System.out.println("File already exists! Size before opening: " + file.length() + " bytes");
//	    } else {
//	        System.out.println("File does NOT exist, creating new file.");
//	    }

		try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {

//	        // Convert buffer to array safely (only if backed by an array)
//	        if (buffer.hasArray()) {
//	            System.out.println("Buffer Content: " + Arrays.toString(buffer.array()));
//	        } else {
//	            System.out.println("Buffer has no backing array!");
//	        }

			buffer.rewind();
			channel.write(buffer);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	public List<Column> readMetadata() {
//		List<Column> columns = new ArrayList<>();
//		Path metaPath = Paths.get(directory + tableName + "/" + "Metadata");
//
//		if (!Files.exists(metaPath))
//			return columns; // Return empty list if file doesn't exist
//
//		try (FileChannel channel = FileChannel.open(metaPath, StandardOpenOption.READ)) {
//			ByteBuffer buffer = ByteBuffer.allocate(4096);
//			channel.read(buffer);
//			buffer.flip();
//
//			while (buffer.hasRemaining()) {
//				String columnName = readString(buffer);
//				String dataType = readString(buffer);
//				Object constraintObj = readObject(buffer);
//
//				List<Constraint> constraints = new ArrayList<>();
//				if (constraintObj instanceof List<?>) {
//					constraints = (List<Constraint>) constraintObj;
//				} else if (constraintObj instanceof Constraint) {
//					constraints.add((Constraint) constraintObj);
//				}
//
//				columns.add(new Column(columnName, dataType, constraints));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		return columns;
//	}

//	private String readString(ByteBuffer buffer) {
//		int length = buffer.getInt();
//		byte[] bytes = new byte[length];
//		buffer.get(bytes);
//		return new String(bytes, StandardCharsets.UTF_8);
//	}
//	
//	private List<Constraint> readObject(ByteBuffer buffer) {
//		try {
//	        int length = buffer.getInt(); // Read stored length
//	        if (length <= 0) return new ArrayList<>();
//
//	        byte[] constraintBytes = new byte[length];
//	        buffer.get(constraintBytes); // Read serialized constraint bytes
//
//	        try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(constraintBytes))) {
//	            Object obj = objectInputStream.readObject();
//	            if (obj instanceof List<?>) {
//	                return (List<Constraint>) obj; // Type-safe cast
//	            }
//	        }
//	    } catch (IOException | ClassNotFoundException e) {
//	        e.printStackTrace();
//	    }
//	    return new ArrayList<>(); // Return empty list if deserialization fails
//
//	}

//	private List<Constraint> readObject(ByteBuffer buffer) {
//		try {
//			int length = buffer.getInt(); // Read stored length
//			if (length <= 0)
//				return new ArrayList<>();
//
//			byte[] constraintBytes = new byte[length];
//			buffer.get(constraintBytes); // Read serialized constraint bytes
//
//			try (ObjectInputStream objectInputStream = new ObjectInputStream(
//					new ByteArrayInputStream(constraintBytes))) {
//				Object obj = objectInputStream.readObject();
//				if (obj instanceof List<?>) {
//					return (List<Constraint>) obj; // Type-safe cast
//				}
//			}
//		} catch (IOException | ClassNotFoundException e) {
//			e.printStackTrace();
//		}
//		return new ArrayList<>(); // Return empty list if deserialization fails
//
//	}

	public String returntype(Object value) {
		if (value instanceof String) {
			return "STRING";
		}
		if (value instanceof Integer) {
			return "INT";
		}
		if (value instanceof Character) {
			return "CHAR";
		}
		if (value instanceof Double) {
			return "FLOAT";
		}
		if (value instanceof Byte) {
			return "BOOL";
		}
		return null;
	}

	public int getOffsetforSelect(String filePath, long rowid) {
		Path path = Paths.get(directory + tableName + "/" + filePath + "_metadata");
		long offset = 0;
		int length = 0;

		System.out.println("Path: " + path);

		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {

			if (channel.size() < 8) {
				System.out.println("Error: File too small to contain row count.");
				return 0;
			}
			ByteBuffer headerBuffer = ByteBuffer.allocate(16);
			if (channel.read(headerBuffer) != 16) {
				System.out.println("Failed 1111111111111111111111 to read the complete header.");
				return 0;
			}
			headerBuffer.flip();
			long rowCount = headerBuffer.getLong();
			System.out.println("rowcountttttttttttt     " + rowCount);
			System.out.println("Row count in file: " + rowCount);
			if (rowCount <= 0) {
				System.out.println("Invalid row count in file.");
				return 0;
			}

			System.out.println("sssssssize   " + channel.size());

			long position = ((rowid - 1) * 22) + 26;

			if (position < 1) {
				return 0;
			}

			System.out.println("possssssssss ition   " + position);
			if (position >= channel.size()) {
				System.out.println("Error: Computed position is beyond file size.");
				return 0;
			}

			channel.position(position);

			// Read offset and length
			ByteBuffer buffer = ByteBuffer.allocate(12);
			int bytesRead = channel.read(buffer);
			System.out.println("Bytes read: " + bytesRead);

			if (bytesRead != 12) {
				System.out.println("Failed to r000000000000000000ead the row's offset and length.");
				return 0;
			}
			buffer.flip();

			offset = buffer.getLong();
			length = buffer.getInt();

			System.out.println("hello lengt  " + length);
			System.out.println("Computed1 offset + length: " + (offset + length));
			return (int) (offset + length);

		} catch (IOException e) {

			e.printStackTrace();
			return 0;
		}
	}

	public int getlengths(String filePath) {
		Path path = Paths.get(directory + tableName + "/" + filePath);
		long offset = 0;
		int length = 0;

		System.out.println("Path: " + path);

		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {

			if (channel.size() < 8) {
				System.out.println("Error: File too small to contain row count.");
				return 0;
			}
			ByteBuffer headerBuffer = ByteBuffer.allocate(8);
			if (channel.read(headerBuffer) != 8) {
				System.out.println("Failed to99999999999999999 read the complete header.");
				return 0;
			}
			headerBuffer.flip();
			long rowCount = headerBuffer.getLong();
			System.out.println("rowcountttttttttttt     " + rowCount);
			System.out.println("Row count in file: " + rowCount);
			if (rowCount <= 0) {
				System.out.println("Invalid row count in file.");
				return 0;
			}

			System.out.println("sssssssize   " + channel.size());

			long position = ((rowCount - 1) * 22) + 18 + 8;

			if (position < 1) {
				return 0;
			}

			System.out.println("possssssssss ition   " + position);
			if (position >= channel.size()) {
				System.out.println("Error: Computed position is beyond file size.");
				return 0;
			}

			channel.position(position);

			// Read offset and length
			ByteBuffer buffer = ByteBuffer.allocate(4);
			int bytesRead = channel.read(buffer);
			System.out.println("Bytes read: " + bytesRead);

//	        if (bytesRead != 12) {
//	            System.out.println("Failed to read the row's offset and length.");
//	            return 0;
//	        }
			buffer.flip();

//	        offset = buffer.getLong();
			length = buffer.getInt();
			System.out.println("Computed length: " + length);
//	        System.out.println("Computed offset + length: " + (offset + length));
			return length;

		} catch (IOException e) {

			e.printStackTrace();
			return 0;
		}
	}

	public long getOffset(String column) {
		Path path = Paths.get(directory + tableName + "/" + column);
		long offset = 0;
		int length = 0;

		System.out.println("Path: " + path);

		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {

			if (channel.size() < 8) {
				System.out.println("Error: File too small to contain row count.");
				return 0;
			}
			ByteBuffer headerBuffer = ByteBuffer.allocate(8);
			if (channel.read(headerBuffer) != 8) {
				System.out.println("Failed to r-----------------ead the complete header.");
				return 0;
			}
			headerBuffer.flip();
			long rowCount = headerBuffer.getLong();
			System.out.println("rowcountttttttttttt     " + rowCount);
			System.out.println("Row count in file: " + rowCount);
			if (rowCount <= 0) {
				System.out.println("Invalid row count in file.");
				return 0;
			}

			System.out.println("sssssssize   " + channel.size());

			long position = ((rowCount - 2) * 22) + 18;

			if (position < 1) {
				return 0;
			}

			System.out.println("possssssssss ition   " + position);
			if (position >= channel.size()) {
				System.out.println("Error: Computed position is beyond file size.");
				return 0;
			}

			channel.position(position);

			// Read offset and length
			ByteBuffer buffer = ByteBuffer.allocate(12);
			int bytesRead = channel.read(buffer);
			System.out.println("Bytes read: " + bytesRead);

			if (bytesRead != 12) {
				System.out.println("Failed to read th--------------------e row's offset and length.");
				return 0;
			}
			buffer.flip();

			offset = buffer.getLong();
			length = buffer.getInt();
			System.out.println("Computed length: " + length);
			System.out.println("Computed offset + length: " + (offset + length));
			return offset + length;

		} catch (IOException e) {

			e.printStackTrace();
			return 0;
		}
	}

	public int getLength(String filePath, long lengthPosition) {
		Path path = Paths.get(directory + tableName + "/" + filePath);
		int length = 0;
		System.out.println("Error: Lengt------------." + path);

		System.out.println("Error: Lengt------------." + lengthPosition);
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			if (lengthPosition >= channel.size()) {
				System.out.println("Error: Length position is beyond file size.");
				return 0;
			}

			channel.position(lengthPosition);
			ByteBuffer buffer = ByteBuffer.allocate(4); // Only read length (int = 4 bytes)
			int bytesRead = channel.read(buffer);
			System.out.println("Bytes read for length: " + bytesRead);

			if (bytesRead != 4) {
				System.out.println("Failed to read the r===============ow's length.");
				return 0;
			}
			buffer.flip();
			length = buffer.getInt();

			System.out.println("*****************  " + length);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return length;
	}

	public long getRowId(String fileName) {
		long rowid = 0;
		try (FileChannel channel = FileChannel.open(Paths.get(directory + tableName + "/" + fileName),
				StandardOpenOption.READ)) {

			ByteBuffer buffer1 = ByteBuffer.allocate(8);

			while (channel.read(buffer1) > 0) {
				buffer1.flip();
				while (buffer1.remaining() >= 8) {

					long rowCount = buffer1.getLong();
					System.out.println("rrrrrrrowCount " + rowCount);
				}
			}
			ByteBuffer buffer = ByteBuffer.allocate(1024);

			while (channel.read(buffer) > 0) {
				buffer.flip();
				while (buffer.remaining() >= 13) {
					rowid = buffer.getLong();
					boolean booleanValue = buffer.get() != 0;
					boolean booleanValue1 = buffer.get() != 0;
					int value = buffer.getInt();
					System.out.println("value  " + value);
				}
				buffer.clear();

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return rowid;
	}

	public boolean updatemethod(Map<String, E> setColumnValues, List<ConditionGroup> conditionGroups) {
		List<List<Long>> listOfsetRowNumbers = new ArrayList<>();
		List<String> logicalOperator = new ArrayList<>();
		System.out.println("cehcking....................");

		if (!columnNameConstraintCheck(setColumnValues)) {
			return false;
		}
		if (conditionGroups == null) {
			for (Map.Entry<String, E> entry : setColumnValues.entrySet()) {
				return setValuesWithoutCondition(null, entry.getKey(), entry.getValue());
			}

		}
		for (ConditionGroup group : conditionGroups) {
			List<Long> rows = iterateGroupCondition(group);
			listOfsetRowNumbers.add(rows);
			logicalOperator.add(group.getLogicalOperator());
		}

		Set<Long> finalResultSet = new HashSet<>(listOfsetRowNumbers.get(0));
		for (int i = 1; i < listOfsetRowNumbers.size(); i++) {
			if ("AND".equals(logicalOperator.get(i - 1))) {
				finalResultSet.retainAll(listOfsetRowNumbers.get(i));
			} else {
				finalResultSet.addAll(listOfsetRowNumbers.get(i));
			}
		}

		List<Long> finalRows = new ArrayList<>(finalResultSet);
		for (Map.Entry<String, E> entry : setColumnValues.entrySet()) {
			setValues(finalRows, entry.getKey(), entry.getValue());
		}

		return true;
	}

	private <E> ArrayList<E> readingFile(String metadataFile, String dataFile) {
		ArrayList<E> result = new ArrayList<E>();

		try (FileChannel metaChannel = FileChannel.open(Paths.get(directory + tableName + "/" + metadataFile),
				StandardOpenOption.READ);
				FileChannel dataChannel = FileChannel.open(Paths.get(directory + tableName + "/" + dataFile),
						StandardOpenOption.READ)) {

			ByteBuffer countBuffer = ByteBuffer.allocate(Long.BYTES);
			if (metaChannel.read(countBuffer) < Long.BYTES)
				return result;
			countBuffer.flip();
			long rowCount = countBuffer.getLong();

			System.out.println("rowCount for unique strings: " + rowCount);

			for (int i = 0; i < rowCount; i++) {
				int rowSize = getRowSize("STRING");
				if (rowSize <= 0)
					continue;

				ByteBuffer rowBuffer = ByteBuffer.allocate(rowSize);
				if (metaChannel.read(rowBuffer) < rowSize)
					break;
				rowBuffer.flip();

				long rowId = rowBuffer.getLong();
				byte isDeleted = rowBuffer.get();
				byte isNull = rowBuffer.get();

				if (isDeleted == 1)
					continue;
				if (isNull == 1) {
					result.add(null);
					continue;
				}

				long offset = rowBuffer.getLong();
				int length = rowBuffer.getInt();

				if (length == 0) {
					result.add((E) "");
					continue;
				}

				ByteBuffer dataBuffer = ByteBuffer.allocate(length);
				dataChannel.position(offset);
				dataChannel.read(dataBuffer);
				dataBuffer.flip();
				String actualString = new String(dataBuffer.array(), StandardCharsets.UTF_8);

				System.out.println("Unique String Value: " + actualString);
				result.add((E) actualString);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("+++++++++++++++    " + result);
		return result;
	}

	public List<Long> iterateGroupCondition(ConditionGroup group) {
		String columnName = group.getColumnName();
		Object value = group.getValue();
		String datatype = returntype(value);
		String operator = group.getOperator();

		if (datatype.equals("STRING")) {
			System.out.println(
					"CoulmnName --------------------====================================================================================  ---- >  "
							+ columnName + "  value -------  >" + value);

			return stringFileIterate(columnName, (String) value);
		}

		Path path = Paths.get(directory + tableName + "/" + columnName);
		List<Long> listofRowid = new ArrayList<>();

		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			ByteBuffer countBuffer = ByteBuffer.allocate(Long.BYTES);
			if (channel.read(countBuffer) < Long.BYTES)
				return listofRowid;
			countBuffer.flip();
			long rowCount = countBuffer.getLong();

			for (int i = 0; i < rowCount; i++) {
				int rowSize = getRowSize(datatype);
				if (rowSize <= 0)
					continue;

				ByteBuffer rowBuffer = ByteBuffer.allocate(rowSize);
				if (channel.read(rowBuffer) < rowSize)
					break;
				rowBuffer.flip();

				long rowId = rowBuffer.getLong();
				byte isDeleted = rowBuffer.get();
				byte isNull = rowBuffer.get();

				if (isDeleted == 1 || isNull == 1)
					continue;

				Object data = readValue(rowBuffer, datatype);

				System.out.println("Operatoerrrrrrr ==== >" + operator);

				if (operator.equals("=") && Objects.equals(data, value)) {
					System.out.println(" check 1 rowid" + rowId);
					listofRowid.add(rowId);
				} else if (data instanceof Number && value instanceof Number) {
					double dataValue = ((Number) data).doubleValue();
					double compareValue = ((Number) value).doubleValue();

					if (operator.equals(">") && (dataValue > compareValue)) {
						System.out.println(" check 1 rowid " + rowId + " datavalue " + dataValue);

						listofRowid.add(rowId);
					} else if (operator.equals("<") && (dataValue < compareValue)) {
						listofRowid.add(rowId);
					} else if (operator.equals(">=") && (dataValue >= compareValue)) {
						listofRowid.add(rowId);
					} else if (operator.equals("<=") && (dataValue <= compareValue)) {
						listofRowid.add(rowId);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("List of  full rowid   " + listofRowid);
		return listofRowid;
	}

	public void setValues(List<Long> listofRowid, String columnname, Object value) {
		Path path = Paths.get(directory + tableName + "/" + columnname);

		System.out.println("Path: " + path);
		String datatype = returntype(value);

		System.out.println("99999999999999999999listofrowid99999999999999999999999999999999  " + listofRowid);

		if (datatype.equals("STRING")) {
			path = Paths.get(path + "_metadata");
		}

		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) {

			ByteBuffer headerBuffer = ByteBuffer.allocate(16);
			int bytesRead = channel.read(headerBuffer);
			if (bytesRead != 16) {
				System.out.println("Failed to read the complete header.");
				return;
			}
			headerBuffer.flip();
			long rowCount = headerBuffer.getLong();
			long lastOffset = headerBuffer.getLong();
			System.out.println("Row count: " + rowCount);

			for (Long rowid : listofRowid) {

				long position = 0;

				System.out.println("Processing row ID: " + rowid);

				if (datatype.equals("INT")) {
					position = ((rowid - 1) * 14) + 10 + 8;
				} else if (datatype.equals("FLOAT")) {
					position = ((rowid - 1) * 18) + 10 + 8;
				} else if (datatype.equals("BOOL")) {
					position = ((rowid - 1) * 11) + 10 + 8;
				} else if (datatype.equals("CHAR")) {
					position = ((rowid - 1) * 12) + 10 + 8;
				} else if (datatype.equals("STRING")) {
					position = ((rowid - 1) * 22) + 18 + 8;
				}

				if (position < 1) {
					position = 0;
				}

				channel.position(position);
				ByteBuffer writeBuffer = ByteBuffer.allocate(12);
				if (datatype.equals("INT")) {
					writeBuffer.putInt((Integer) value);
				} else if (datatype.equals("FLOAT")) {
					writeBuffer.putDouble((Double) value);
				} else if (datatype.equals("BOOL")) {
					writeBuffer.put((byte) value);
				} else if (datatype.equals("CHAR")) {
					writeBuffer.put((byte) value);
				} else if (datatype.equals("STRING")) {
					byte[] stringBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
					ByteBuffer valueBuffer = ByteBuffer.allocate(stringBytes.length);
					valueBuffer.put(stringBytes);
					valueBuffer.flip();
					writeToTheFile(columnname, valueBuffer);
					long lastOffset1 = getOffset(columnname, (String) value);
					int lastLength = getlengths(columnname + "_metadata");

					long offset = lastOffset + lastLength;
					int cuurentLength = ((String) value).length();
//		     
					System.out.println("laasssssssssssssssssssssOfffffffffffffffset  " + lastOffset1);

					writeBuffer.putLong(lastOffset1);
					writeBuffer.putInt(cuurentLength);
				}

				writeBuffer.flip();
				channel.write(writeBuffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean setValuesWithoutCondition(List<Long> listofRowid, String columnname, Object value) {
		Path path = Paths.get(directory + tableName + "/" + columnname);

		System.out.println("Path: " + path);
		String datatype = returntype(value);

		System.out.println("99999999999999999999listofrowid99999999999999999999999999999999  " + listofRowid);

		if (datatype.equals("STRING")) {
			path = Paths.get(path + "_metadata");
		}

		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) {

			ByteBuffer headerBuffer = ByteBuffer.allocate(16);
			int bytesRead = channel.read(headerBuffer);

			if (bytesRead != 16) {
				System.out.println("Failed to read the complete header.");
				return false;
			}
			headerBuffer.flip();
			long rowCount = headerBuffer.getLong();
			System.out.println("Row count: " + rowCount);
			long headerOffset = headerBuffer.getLong();

			for (int i = 1; i <= rowCount + 1; i++) {

				long position = 0;

				System.out.println("Processing row ID: " + i);

				if (datatype.equals("INT")) {
					position = ((i - 1) * 14) + 10 + 8;
				} else if (datatype.equals("FLOAT")) {
					position = ((i - 1) * 18) + 10 + 8;
				} else if (datatype.equals("BOOL")) {
					position = ((i - 1) * 11) + 10 + 8;
				} else if (datatype.equals("CHAR")) {
					position = ((i - 1) * 12) + 10 + 8;
				} else if (datatype.equals("STRING")) {
					position = ((i - 1) * 22) + 18 + 8;
				}

				if (position < 1) {
					position = 0;
				}

				channel.position(position);

				ByteBuffer writeBuffer = ByteBuffer.allocate(12);
				if (datatype.equals("INT")) {
					writeBuffer.putInt((Integer) value);
				} else if (datatype.equals("FLOAT")) {
					writeBuffer.putDouble((Double) value);
				} else if (datatype.equals("BOOL")) {
					writeBuffer.put((byte) value);
				} else if (datatype.equals("CHAR")) {
					writeBuffer.put((byte) value);
				} else if (datatype.equals("STRING")) {
					byte[] stringBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
					ByteBuffer valueBuffer = ByteBuffer.allocate(stringBytes.length);
					valueBuffer.put(stringBytes);
					valueBuffer.flip();
					writeToTheFile(columnname, valueBuffer);
					long lastOffset = getOffset(columnname, (String) value);

					int cuurentLength = ((String) value).length();
//		     
					writeBuffer.putLong(lastOffset);
					writeBuffer.putInt(cuurentLength);
				}

				writeBuffer.flip();
				channel.write(writeBuffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

//	public void updateStringValue(List<Long> listofRowid, String columnname, String newValue) {
//		Path metadataPath = Paths.get(directory + tableName + "/" + columnname + "_metadata");
//		Path dataPath = Paths.get(directory + tableName + "/" + columnname);
//
//		try (FileChannel metadataChannel = FileChannel.open(metadataPath, StandardOpenOption.READ,
//				StandardOpenOption.WRITE);
//				FileChannel dataChannel = FileChannel.open(dataPath, StandardOpenOption.APPEND,
//						StandardOpenOption.WRITE)) {
//
//			ByteBuffer headerBuffer = ByteBuffer.allocate(8);
//			if (metadataChannel.read(headerBuffer) != 8) {
//				System.out.println("Failed to read the header.");
//				return;
//			}
//			headerBuffer.flip();
//			long rowCount = headerBuffer.getLong();
//
//			System.out.println("Row count: " + rowCount);
//
//			for (Long rowid : listofRowid) {
//				if (rowid == 1)
//					continue;
//
//				long position = ((rowid - 2) * 22) + 18; // Position for offset and length
//				metadataChannel.position(position);
//
//				ByteBuffer readBuffer = ByteBuffer.allocate(12);
//				if (metadataChannel.read(readBuffer) != 12) {
//					System.out.println("Failed to read existing metadata for row " + rowid);
//					continue;
//				}
//
//				readBuffer.flip();
//				long existingOffset = 0;
//				int existingLength = 0;
//				if (rowid == 1) {
//					existingOffset = 0;
//					existingLength = getOffsetforSelect(columnname, rowid);
//				} else {
//					existingOffset = readBuffer.getLong();
//					existingLength = readBuffer.getInt();
//				}
//
//				System.out.println("Existing STRING offset: " + existingOffset + ", Length: " + existingLength);
//
//				// Write new value to the data file at the end
//				byte[] newValueBytes = newValue.getBytes(StandardCharsets.UTF_8);
//				ByteBuffer newValueBuffer = ByteBuffer.allocate(newValueBytes.length);
//				newValueBuffer.put(newValueBytes);
//				newValueBuffer.flip();
//
//				long newOffset = dataChannel.size(); // Append at end of file
//				dataChannel.position(newOffset);
//				dataChannel.write(newValueBuffer);
//
//				// Update metadata file with new offset and length
//				metadataChannel.position(position);
//				ByteBuffer updateBuffer = ByteBuffer.allocate(12);
//				updateBuffer.putLong(newOffset);
//				updateBuffer.putInt(newValueBytes.length);
//				updateBuffer.flip();
//				metadataChannel.write(updateBuffer);
//			}
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	public long calculatePosition(String datatype, long rowid, String columnName) {
		long baseOffset = 10 + 8;
		long rowOffset = 0;

		switch (datatype) {
		case "INT":
			rowOffset = (rowid - 1) * 14;
			break;
		case "FLOAT":
			rowOffset = (rowid - 1) * 18;
			break;
		case "BOOL":
			rowOffset = (rowid - 1) * 11;
			break;
		case "CHAR":
			rowOffset = (rowid - 1) * 12;
			break;
		case "STRING":

			if (rowid == 1) {
				baseOffset = 0;
				rowOffset = getOffsetforSelect(columnName, rowid);
			} else {
				baseOffset = 18;
				rowOffset = (rowid - 1) * 22;
			}

			break;
		default:
			throw new IllegalArgumentException("Unsupported datatype: " + datatype);
		}

		return baseOffset + rowOffset;
	}

	public String stringFile1(String dataFile) {
		Path filePath = Paths.get(directory + tableName + "/" + dataFile);
		StringBuilder fullData = new StringBuilder();

		System.out.println("filePath " + filePath);
		try (FileChannel dataChannel = FileChannel.open(filePath, StandardOpenOption.READ)) {
			ByteBuffer buffer = ByteBuffer.allocate((int) dataChannel.size());
			dataChannel.read(buffer);
			buffer.flip();
			System.out.println("-----full---size---   " + dataChannel.size());
			byte[] byteArray = new byte[buffer.remaining()];
			buffer.get(byteArray);

			String fileContent = new String(byteArray, StandardCharsets.UTF_8).replace("\0", "").trim();
			fullData.append(fileContent);

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("===00000full==============   " + (fullData.toString()));
		return fullData.toString();
	}

	public List<Long> stringFileIterate(String filename, String value) {
		List<String> result = readingStringFile(filename + "_metadata", filename);
		List<Long> rowid = new ArrayList<>();
		System.out.println("*****result ********** " + result);
		for (int i = 0; i < result.size(); i++) {

			System.out.println("***************   " + result.get(i) + "  ==== " + value);

			if (result.get(i) != null && result.get(i).contains(value)) {
				rowid.add((long) i + 1);
			}
		}

		System.out.println("***************   " + rowid);
		return rowid;
	}

//	public List<Long> stringFileRowId(String metadataFile, String dataFile,Object value) {
//		List<Long> listofDatas = new ArrayList<>();
//
//		try (FileChannel metaChannel = FileChannel.open(Paths.get(directory + tableName + "/" + metadataFile),
//				StandardOpenOption.READ);
//				FileChannel dataChannel = FileChannel.open(Paths.get(directory + tableName + "/" + dataFile),
//						StandardOpenOption.READ)) {
//
//			System.out.println("Metadata File Size: " + metaChannel.size());
//			System.out.println("Data File Size: " + dataChannel.size());
//
//			ByteBuffer countBuffer = ByteBuffer.allocate(Long.BYTES);
//			if (metaChannel.read(countBuffer) < Long.BYTES)
//				return listofDatas;
//			countBuffer.flip();
//			long rowCount = countBuffer.getLong();
//
//			System.out.println("rowCount: " + rowCount);
//
//			for (int i = 0; i < rowCount; i++) {
//				int rowSize = getRowSize("STRING");
//				if (rowSize <= 0)
//					continue;
//
//				ByteBuffer rowBuffer = ByteBuffer.allocate(rowSize);
//				int bytesRead = metaChannel.read(rowBuffer);
//				if (bytesRead < rowSize) {
//					System.out.println("Stopping loop early due to insufficient bytes.");
//					break;
//				}
//
//				rowBuffer.flip();
//				long rowId = rowBuffer.getLong();
//				byte isDeleted = rowBuffer.get();
//				byte isNull = rowBuffer.get();
//				long offset = rowBuffer.getLong();
//				int length = rowBuffer.getInt();
//
//				if (isDeleted == 1) {
//					continue;
//				}
//				if (isNull == 1) {
//					listofDatas.add(rowId);
//					continue;
//
//				}
//				if (length == 0) {
//					listofDatas.add(rowId);
//					continue;
//
//				}
//            
//				listofDatas.add(rowId);
//			}
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		System.out.println("List contents: " + listofDatas);
//		return listofDatas;
//	}

	private List<String> splitStringByLengths(String str, int chunkSize) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < str.length(); i += chunkSize) {
			result.add(str.substring(i, Math.min(i + chunkSize, str.length())));
		}
		return result;
	}

	public List<E> readColumnDataAsList(String columnName, String datatype) {
		List<E> datas = new ArrayList<>();
		Path filePath = Paths.get(directory + tableName + "/" + columnName);

		if (!Files.exists(filePath)) {

			return datas;
		}

		try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
			ByteBuffer countBuffer = ByteBuffer.allocate(Long.BYTES);
			if (channel.read(countBuffer) < Long.BYTES)
				return datas;

			countBuffer.flip();
			long rowCount = countBuffer.getLong();

			for (int i = 0; i < rowCount; i++) {
				Object value = readDataValue(channel, datatype);

				if (value != null) {
					datas.add((E) value);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return datas;
	}

	public Map<Integer, E> readingDatasAsMap(String columnName, String datatype) {
		Map<Integer, E> dataMap = new HashMap<>();
		Path filePath = Paths.get(directory + tableName + "/" + columnName);

		if (!Files.exists(filePath)) {
			System.out.println("File does not exist: " + filePath);
			return dataMap;
		}

		try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {

			System.out.println("FFFFFFFFFFFFFFFFile Path  " + filePath);

			ByteBuffer countBuffer = ByteBuffer.allocate(Long.BYTES);
			if (channel.read(countBuffer) < Long.BYTES)
				return dataMap;

			countBuffer.flip();
			long rowCount = countBuffer.getLong();
			System.out.println(
					"Row count in colu00000000000000000000000000000000000mn '" + columnName + "': " + rowCount);

			for (int i = 0; i < rowCount; i++) {
				Object value = readDataValue(channel, datatype);
				dataMap.put(i, (E) value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		dataMap.forEach((key, value) -> System.out.println("Row " + key + ": " + value));
		return dataMap;
	}

	private Object readDataValue(FileChannel channel, String datatype) throws IOException {
		int rowSize = getRowSize(datatype);
		if (rowSize <= 0)
			return null;

		ByteBuffer rowBuffer = ByteBuffer.allocate(rowSize);
		if (channel.read(rowBuffer) < rowSize)
			return null;

		rowBuffer.flip();
		rowBuffer.getLong();
		byte isDeleted = rowBuffer.get();
		byte isNull = rowBuffer.get();
		if (isDeleted == 1)

			return null;
		return isNull == 1 ? "null" : readValue(rowBuffer, datatype);
	}

	public <E> ArrayList<E> readingStringFile(String metadataFile, String dataFile) {
		ArrayList<E> result = new ArrayList<>();
		System.out.println("hello");

		try (FileChannel metaChannel = FileChannel.open(Paths.get(directory + tableName + "/" + metadataFile),
				StandardOpenOption.READ);
				FileChannel dataChannel = FileChannel.open(Paths.get(directory + tableName + "/" + dataFile),
						StandardOpenOption.READ)) {

			// Read header: first 16 bytes (8 for rowCount, 8 for headerOffset)
			ByteBuffer headerBuffer = ByteBuffer.allocate(16);
			if (metaChannel.read(headerBuffer, 0) < 16) {
//				System.err.println("Metadata file header is incomplete.");
				return result;
			}
			headerBuffer.flip();
			long rowCount = headerBuffer.getLong();
			long headerOffset = headerBuffer.getLong();
			System.out.println("rowCount for unique strings: " + rowCount);
			System.out.println("Header offset for unique strings: " + headerOffset);

			// Expected record size (in bytes).
			// (Must match what is written! For example: 8 + 1 + 1 + 8 + 4 = 22 bytes)
			int recordSize = getRowSize("STRING");
			if (recordSize <= 0) {
				System.err.println("Invalid row size for STRING.");
				return result;
			}
			System.out.println("Expected record size: " + recordSize);

			// Log metadata file size for debugging
			long metaFileSize = metaChannel.size();
			System.out.println("Metadata file size: " + metaFileSize);

			// Loop through each metadata record using absolute positions.
			for (int i = 0; i < rowCount; i++) {
				long recordPosition = 16 + (i * recordSize);
				// Check if we have enough bytes in the file for a complete record:
				if (metaFileSize < recordPosition + recordSize) {
					System.err.println("Incomplete record at position " + recordPosition);
					break;
				}

				ByteBuffer rowBuffer = ByteBuffer.allocate(recordSize);
				if (metaChannel.read(rowBuffer, recordPosition) < recordSize) {
					System.err.println("Incomplete record read at position " + recordPosition);
					break;
				}
				rowBuffer.flip();

				long rowId = rowBuffer.getLong();
				byte isDeleted = rowBuffer.get();
				byte isNull = rowBuffer.get();
				long rowRelativeOffset = rowBuffer.getLong();
				int length = rowBuffer.getInt();

				// Debug print of the record fields
				System.out.println("Record " + (i + 1) + " metadata:");
				System.out.println("  rowId: " + rowId);
				System.out.println("  isDeleted: " + isDeleted);
				System.out.println("  isNull: " + isNull);
				System.out.println("  rowRelativeOffset: " + rowRelativeOffset);
				System.out.println("  length: " + length);

				// Skip deleted or null rows
				if (isDeleted == 1)
					continue;
				if (isNull == 1) {
					result.add(null);
					continue;
				}
				if (length == 0) {
					result.add((E) "");
					continue;
				}

				// Calculate the actual position in the data file:
				// actualPosition = headerOffset + rowRelativeOffset
//	            long actualPosition = headerOffset + rowRelativeOffset;
				System.out.println("  Calculated actual data offset: " + rowRelativeOffset);

				// Read the data from the data file
				ByteBuffer dataBuffer = ByteBuffer.allocate(length);
				dataChannel.position(rowRelativeOffset);
				int bytesRead = dataChannel.read(dataBuffer);
				if (bytesRead < 0) {
					System.err.println("No data read from data file at offset " + rowRelativeOffset);
					continue;
				}
				dataBuffer.flip();
				String actualString = new String(dataBuffer.array(), 0, bytesRead, StandardCharsets.UTF_8);
				System.out.println("  Unique String Value: " + actualString);

				result.add((E) actualString);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("result: " + result);
		return result;
	}

	public <E> HashMap<Integer, E> readingStringFileAsMap(String metadataFile, String dataFile) {

		System.out.println("*************************************************************************");

		HashMap<Integer, E> result = new HashMap<>();
		try (FileChannel metaChannel = FileChannel.open(Paths.get(directory + tableName + "/" + metadataFile),
				StandardOpenOption.READ);
				FileChannel dataChannel = FileChannel.open(Paths.get(directory + tableName + "/" + dataFile),
						StandardOpenOption.READ)) {

			System.out.println("PPPPPPPatrha   1  " + directory + tableName + "/" + metadataFile);

			System.out.println("PPPPPPPatrha   2  " + directory + tableName + "/" + dataFile);

			ByteBuffer countBuffer = ByteBuffer.allocate(16);
			if (metaChannel.read(countBuffer) < 16) {
				System.out.println("GHello");
				return result;
			}
			countBuffer.flip();
			long rowCount = countBuffer.getLong();
			long headerOffset = countBuffer.getLong();
			System.out.println("rowCount for unique strings: " + rowCount);

			for (int i = 0; i < rowCount; i++) {
				int rowSize = getRowSize("STRING");
				if (rowSize <= 0)
					continue;

				ByteBuffer rowBuffer = ByteBuffer.allocate(rowSize);
				if (metaChannel.read(rowBuffer) < rowSize)
					break;
				rowBuffer.flip();

				long rowId = rowBuffer.getLong();
				byte isDeleted = rowBuffer.get();
				byte isNull = rowBuffer.get();

				long offset = rowBuffer.getLong();
				int length = rowBuffer.getInt();

				if (isDeleted == 1)
					continue;

				if (isNull == 1) {
//					result.add(null);
					result.put((int) rowId, null);
					continue;
				}

				if (length == 0) {

					result.put((int) rowSize, (E) "");
					continue;
				}
				System.out.println("rowId  " + rowId);

				System.out.println("offset  " + offset);
				System.out.println("length  " + length);

				ByteBuffer dataBuffer = ByteBuffer.allocate(length);
				dataChannel.position(offset);
				int bytesRead = dataChannel.read(dataBuffer);
				dataBuffer.flip();

				// Read only the actual bytes read (avoid extra buffer space)
				String actualString = new String(dataBuffer.array(), 0, bytesRead, StandardCharsets.UTF_8);

				System.out.println("Unique String Value: " + actualString.length());

				result.put((int) rowId, (E) actualString);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("result   " + result);
		return result;
	}

	//////////////////////// Delete Methods

	public boolean deletemethod(List<ConditionGroup> conditionGroups) {

		if (conditionGroups == null || conditionGroups.isEmpty()) {
			for (Column column : columnsArray) {
				String filePath = directory + tableName + "/" + column.getName(); // File path
				String metadataFilePath = directory + tableName + "/" + column.getName() + "_metadata"; // Metadata file
																										// path

				String datatype = "STRING"; // Assume we get this info from somewhere

				try {
					// Delete main file
					deleteFile(filePath);

					// If datatype is STRING, also delete metadata file
					if ("STRING".equals(datatype)) {
						deleteFile(metadataFilePath);
					}
				} catch (IOException e) {
					System.err.println("Error deleting files for: " + column.getName());
					e.printStackTrace();
					return false;
				}
			}

			// Recreate files
			for (Column column : columnsArray) {
				String filePath = directory + tableName + "/" + column.getName(); // File path
				String metadataFilePath = directory + tableName + "/" + column.getName() + "_metadata"; // Metadata file
																										// path

				try {
					// Create main file
					createFile(filePath);

					// If datatype is STRING, also create metadata file
					if ("STRING".equals("STRING")) {
						createFile(metadataFilePath);
					}
				} catch (IOException e) {
					System.err.println("Error creating files for: " + column.getName());
					e.printStackTrace();
					return false;
				}
			}

			return true;
		}

		List<List<Long>> listOfdeleteRowNumbers = new ArrayList<>();
		List<String> logicalOperator = new ArrayList<>();

		for (ConditionGroup group : conditionGroups) {
			List<Long> rows = iterateGroupCondition(group);
			listOfdeleteRowNumbers.add(rows);
			logicalOperator.add(group.getLogicalOperator());
		}

		if (listOfdeleteRowNumbers.isEmpty()) {
			return false;
		}

		List<Set<Long>> andGroups = new ArrayList<>();
		Set<Long> currentAndSet = new HashSet<>(listOfdeleteRowNumbers.get(0));

		for (int i = 1; i < listOfdeleteRowNumbers.size(); i++) {
			if ("AND".equals(logicalOperator.get(i - 1))) {
				currentAndSet.retainAll(listOfdeleteRowNumbers.get(i));
			} else {

				andGroups.add(new HashSet<>(currentAndSet));

				currentAndSet = new HashSet<>(listOfdeleteRowNumbers.get(i));
			}
		}

		andGroups.add(new HashSet<>(currentAndSet));
		Set<Long> finalResultSet = new HashSet<>();
		for (Set<Long> group : andGroups) {
			finalResultSet.addAll(group);
		}
		List<Long> finalRows = new ArrayList<>(finalResultSet);

		System.out.println("------  List of Row ids  ---------- " + finalRows);

		if (finalRows.isEmpty()) {
			return false;
		}

		if (deleteValues(finalRows)) {
			return true;
		}
		return false;

	}

	private static void deleteFile(String path) throws IOException {
		Path filePath = Paths.get(path);
		if (Files.exists(filePath)) {
			Files.delete(filePath);
			System.out.println("Deleted: " + path);
		}
	}

	private static void createFile(String path) throws IOException {
		Path filePath = Paths.get(path);
		Files.createFile(filePath);
		System.out.println("Created: " + path);
	}

	public boolean deleteValues(List<Long> listofRowid) {
		for (Column column : columnsArray) {
			String datatype = column.getDataType();
			String columnName = column.getName();

			System.out.println("  deleted values  " + columnName);

			Path path = Paths.get(directory + tableName + "/" + columnName);

			System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");

			if (datatype.equals("STRING") || datatype.equals("BLOB")) {

				System.out.println("%%%%%%%%%%$$$$$$$$$$$$$$$$$$$$$$%%%%%%%%%%%%%%%%%%%%%%%%%%  " + datatype);

				path = Paths.get(path + "_metadata");
			}

			try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) {

				if (datatype.equals("STRING") || datatype.equals("BLOB")) {
					ByteBuffer headerBuffer = ByteBuffer.allocate(16);
					int bytesRead = channel.read(headerBuffer);

					if (bytesRead != 16) {

						return false;
					}
					headerBuffer.flip();
					long rowCount = headerBuffer.getLong();
					System.out.println("=========================================  pathsssssssssssss  " + path);

				} else {
					ByteBuffer headerBuffer = ByteBuffer.allocate(8);
					int bytesRead = channel.read(headerBuffer);

					if (bytesRead != 8) {

						return false;
					}
					headerBuffer.flip();
					long rowCount = headerBuffer.getLong();
					System.out.println("=========================================  777777777777777777 " + path);

				}

				for (Long rowid : listofRowid) {

					long position = 0;

					if (datatype.equals("INT")) {
						position = ((rowid - 1) * 14) + 16;
					} else if (datatype.equals("FLOAT")) {
						position = ((rowid - 1) * 18) + 16;
					} else if (datatype.equals("BOOL")) {
						position = ((rowid - 1) * 11) + 16;
					} else if (datatype.equals("CHAR")) {
						position = ((rowid - 1) * 12) + 16;
					} else if (datatype.equals("STRING") || datatype.equals("BLOB")) {
						position = ((rowid - 1) * 22) + 16 + 8;
					}
//					

					if (position < 1) {
						position = 0;
					}

					channel.position(position);
					ByteBuffer writeBuffer = ByteBuffer.allocate(1); // Allocate only 1 byte
					writeBuffer.put((byte) 1); // Marking the row as deleted

					System.out.println("   valuuuuuu  es  cccccccccccccheckkkkk   " + position);

					writeBuffer.flip();
					channel.write(writeBuffer);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

/////////////////// select methods

	public List<E> wholeSelectCondition(List<String> listofcolumns, HashMap<String, List<String>> columnFunctionMap,
			List<ConditionGroup> conditiongroups, OrderBy orderby) {

		System.out.println("column:" + listofcolumns + ":aggre:" + conditiongroups + ":condition:" + conditiongroups
				+ ":order:" + orderby);

		if (listofcolumns == null && columnFunctionMap == null && conditiongroups == null && orderby == null) {
			return viewData();
		} else if (listofcolumns != null && columnFunctionMap == null && conditiongroups == null && orderby == null) {
			List<E> resulyEs = particularViewData(listofcolumns);
			return resulyEs;
		} else if (listofcolumns != null && conditiongroups != null && columnFunctionMap == null && orderby == null) {
			
			System.out.println("***********************************************************************************************************************************************************");
			return (List<E>) getRecordsWithCondition(listofcolumns, conditiongroups);
		} else if (listofcolumns != null && columnFunctionMap != null && conditiongroups == null
				&& (orderby == null || orderby != null)) {

		
			return applyAggregateFunctions(listofcolumns, columnFunctionMap);
		} else if (listofcolumns != null && columnFunctionMap != null && conditiongroups != null
				&& (orderby == null || orderby != null)) {
			return applyAggregateFunctionsWithWhereCondition(listofcolumns, columnFunctionMap, conditiongroups);

		} else if (listofcolumns != null && columnFunctionMap == null && conditiongroups != null && orderby != null) {
			return getRecordsusingOrderByWithWhereCondition(listofcolumns, conditiongroups, orderby);
		} else if (listofcolumns != null && columnFunctionMap == null && conditiongroups == null && orderby != null) {

			return getRecordsusingOrderBy(listofcolumns, orderby);
		}

		return null;

	}

	// selecting full table
	public List<E> viewData() {
		List<E> listOfColumns = new ArrayList<>();
		String columnName = null;
		for (Column column : columnsArray) {
			columnName = column.getName();
			String datatype = column.getDataType();

			if (datatype.equals("STRING")) {
				listOfColumns.add((E) readingStringFile(columnName + "_metadata", columnName));
			} else if (datatype.equals("BLOB")) {
				try {
					listOfColumns.add((E) getImage(columnName + "_metadata", columnName));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				listOfColumns.add((E) readColumnDataAsList(columnName, datatype));
			}
		}
		return listOfColumns;
	}

	// selecting particular columns

	public List<E> particularViewData(List<String> listofcolumns) {
		List<E> listOfColumnsViewData = new ArrayList<>();

		for (String specificColumn : listofcolumns) {
			Column column = getColumnByName(specificColumn); // Get the column object
			System.out.println("ccccccc-------------cccccc  " + column);
//			
			if (column == null) {
				System.out.println("Column not found: " + specificColumn);
				continue;
			}

			String datatype = column.getDataType();

			if (datatype.equals("STRING")) {
				listOfColumnsViewData.add((E) readingStringFile(specificColumn + "_metadata", specificColumn));
			} else if (datatype.equals("BLOB")) {
				try {
					listOfColumnsViewData.add((E) getImage(specificColumn + "_metadata", specificColumn));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				listOfColumnsViewData.add((E) readColumnDataAsList(specificColumn, datatype));
			}
		}

		return listOfColumnsViewData;
	}

	// Helper method to get Column by name
	private Column getColumnByName(String columnName) {
		for (Column column : columnsArray) {
			if (column.getName().equals(columnName)) {
				return column;
			}
		}
		return null;
	}

	// selecting particular columns using where condition

	public List<List<Object>> getRecordsWithCondition(List<String> listofColumns,
			List<ConditionGroup> conditiongroups) {
		List<E> listOfRecords = new ArrayList<>();
		List<List<Long>> listOfsetRowNumbers = new ArrayList<>();
		List<String> logicalOperator = new ArrayList<>();

		List<List<Object>> result = new ArrayList<>(); // ✅ Changed to hold flattened results

		// Collect Row IDs based on conditions
		for (ConditionGroup group : conditiongroups) {
			List<Long> rows = iterateGroupCondition(group);
			System.out.println(" rrowsssssssssssssssss   " + rows);
			listOfsetRowNumbers.add(rows);
			logicalOperator.add(group.getLogicalOperator());
		}

		if (listOfsetRowNumbers.isEmpty()) {
			System.out.println("No matching rows found.");
			return result;
		}

		Set<Long> finalResultSet = new HashSet<>(listOfsetRowNumbers.get(0));
		for (int i = 1; i < listOfsetRowNumbers.size(); i++) {
			if ("AND".equalsIgnoreCase(logicalOperator.get(i - 1))) {
				finalResultSet.retainAll(listOfsetRowNumbers.get(i));
			} else {
				finalResultSet.addAll(listOfsetRowNumbers.get(i));
			}
		}

		List<Long> finalRowList = new ArrayList<>(finalResultSet);

		for (String specificColumn : listofColumns) {
			Column column = getColumnByName(specificColumn);
			if (column == null) {
				System.out.println("Column not found: " + specificColumn);
				continue;
			}

			String datatype = column.getDataType();
//	 
			for (String columnname : listofColumns) {
				if (columnname.equals(column.getName())) {
					System.out.println("[[[[[[[[[[[[[   " + finalRowList);
					System.out.println("result  ==== > " + getValues(finalRowList, columnname, datatype));

					result.add(getValues(finalRowList, columnname, datatype));
				}
			}
		}
		System.out.println("RRRRRRRRRREEEEEEEEEESSSSSSSSSLLLLLLTTTTTTTTT   0000000000000000000000000000000000000000000000000000000000000000000"+result);
		return result;
	}

	public HashMap<Long, E> getValuesWithMap(List<Long> listofRowid, String columnName, String datatype) {
		HashMap<Long, Object> values = new HashMap<>();
		Path path = Paths.get(directory + tableName + "/" + columnName);
		Path path1 = Paths.get(directory + tableName + "/" + columnName);
		long offset = 0;

		if (datatype.equals("STRING")) {
			path = Paths.get(path + "_metadata");
		}

		System.out.println(" paths   ----------------->" + path + "List of row id  " + listofRowid);

		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {

			ByteBuffer headerBuffer = ByteBuffer.allocate(8);
			int bytesRead = channel.read(headerBuffer);
			if (bytesRead != 8) {
				System.out.println("Failed t===============o read the complete header.");
				return (HashMap<Long, E>) values;
			}
			headerBuffer.flip();
			long rowCount = headerBuffer.getLong();
			System.out.println("Row count: " + rowCount);

			for (Long rowid : listofRowid) {

				long position = 0;

				if (datatype.equals("INT")) {
					position = ((rowid - 1) * 14) + 10 + 8;
				} else if (datatype.equals("FLOAT")) {
					position = ((rowid - 1) * 18) + 10 + 8;
				} else if (datatype.equals("BYTE")) {
					position = ((rowid - 1) * 11) + 10 + 8;
				} else if (datatype.equals("CHAR")) {
					position = ((rowid - 1) * 12) + 10 + 8;
				} else if (datatype.equals("STRING")) {
					position = ((rowid - 2) * 22) + 18 + 8;
				}

				if (position < 1) {
					position = 0;
				}

				channel.position(position);
				int rowSize = getRowSize(datatype);
				ByteBuffer readBuffer = ByteBuffer.allocate(rowSize);
				channel.read(readBuffer);
				readBuffer.flip();

				if (datatype.equals("INT")) {

					values.put(rowid, readBuffer.getInt());
//    					System.out.println("-------->check1"+ (readBuffer.getInt()));

				} else if (datatype.equals("FLOAT")) {

					readBuffer.flip();
					values.put(rowid, readBuffer.getDouble());
				} else if (datatype.equals("BYTE")) {
					values.put(rowid, readBuffer.get());
				} else if (datatype.equals("CHAR")) {
					values.put(rowid, (char) readBuffer.get());
				} else if (datatype.equals("STRING")) {
					ByteBuffer metaBuffer = ByteBuffer.allocate(12);
					channel.read(metaBuffer);
					metaBuffer.flip();
					int length = getLength(columnName, rowid);
//					if (rowid == 1) {
//						offset = 0;
//						length = getOffsetforSelect(columnName, rowid);
//					} else {
//						offset = metaBuffer.getLong();
//						length = metaBuffer.getInt();
//					}

					offset = getOffsetforSelect(datatype, rowid);
					System.out.println("---------offset Check ======= " + offset);

					System.out.println("---hhhhhhhhh------Length ======= " + length);

					ByteBuffer stringBuffer = ByteBuffer.allocate(length);
					Path metadataPath = Paths.get(directory + tableName + "/" + columnName + "_metadata");
					try (FileChannel metaChannel = FileChannel.open(path1, StandardOpenOption.READ)) {
						metaChannel.position(offset);
						metaChannel.read(stringBuffer);
						stringBuffer.flip();
						values.put(rowid, StandardCharsets.UTF_8.decode(stringBuffer).toString());
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("-------->values of the esult " + values);
		return (HashMap<Long, E>) values;
	}
	
	
	public List<Object> getValues(List<Long> listofRowid, String columnName, String datatype) {
		List<Object> values = new ArrayList<>();
		Path path = Paths.get(directory + tableName + "/" + columnName);

		if (datatype.equals("STRING") || datatype.equals("BLOB")) {
			path = Paths.get(directory + tableName + "/" + columnName + "_metadata");
		}
     
		System.out.println("getValues");
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {

			if (datatype.equals("STRING")) {
				ByteBuffer headerBuffer = ByteBuffer.allocate(16);
				int bytesRead = channel.read(headerBuffer);
				if (bytesRead != 16) {
					return values;
				}
				headerBuffer.flip();
				long rowCount = headerBuffer.getLong();
			} else {
				// Read header: first 8 bytes (row count)
				ByteBuffer headerBuffer = ByteBuffer.allocate(8);
				int bytesRead = channel.read(headerBuffer);
				if (bytesRead != 8) {
					return values;
				}
				headerBuffer.flip();
				long rowCount = headerBuffer.getLong();
			}
			for (Long rowid : listofRowid) {
				long position = 0;

				if (datatype.equals("INT")) {
					position = ((rowid - 1) * 14) + 10 + 8;
				} else if (datatype.equals("FLOAT")) {
					position = ((rowid - 1) * 18) + 10 + 8;
				} else if (datatype.equals("BYTE")) {
					position = ((rowid - 1) * 11) + 10 + 8;
				} else if (datatype.equals("CHAR")) {
					position = ((rowid - 1) * 12) + 10 + 8;
				} else if (datatype.equals("STRING")) {
					position = ((rowid - 2) * 22) + 18 + 8;
				} else if (datatype.equals("BLOB")) {
					position = ((rowid - 2) * 22) + 18 + 8;
				}

				if (rowid == 0) {
					position = 0;
				}

				channel.position(position);
				int rowSize = getRowSize(datatype);
				ByteBuffer readBuffer = ByteBuffer.allocate(rowSize);
				channel.read(readBuffer);
				readBuffer.flip();

				if (datatype.equals("INT")) {
					values.add(readBuffer.getInt());
				} else if (datatype.equals("FLOAT")) {
					values.add(readBuffer.getDouble());
				} else if (datatype.equals("BYTE")) {
					values.add(readBuffer.get());
				} else if (datatype.equals("CHAR")) {
					values.add((char) readBuffer.get());
				} else if (datatype.equals("STRING")) {
					ByteBuffer metaBuffer = ByteBuffer.allocate(12);
					channel.read(metaBuffer);
					metaBuffer.flip();
					long offset = metaBuffer.getLong();
					int length = metaBuffer.getInt();

					ByteBuffer stringBuffer = ByteBuffer.allocate(length);
					Path metadataPath = Paths.get(directory + tableName + "/" + columnName);
					try (FileChannel metaChannel = FileChannel.open(metadataPath, StandardOpenOption.READ)) {
						metaChannel.position(offset);
						metaChannel.read(stringBuffer);
						stringBuffer.flip();
						
						System.out.println("Offfffffsettttttttttttttt  "+offset);
//						System.out.println("getValues ------    > "+(StandardCharsets.UTF_8.decode(stringBuffer).toString()));
						
						String value=StandardCharsets.UTF_8.decode(stringBuffer).toString();
						values.add(value);
						
						System.out.println("value  "+value);
						
						System.out.println("pppppppppppppppppppppp  "+values);
					
					}
				} else if (datatype.equals("BLOB")) {
					ByteBuffer metaBuffer = ByteBuffer.allocate(12);
					channel.read(metaBuffer);
					metaBuffer.flip();
					long rowRelativeOffset = metaBuffer.getLong(); // Extract BLOB position
					int length = metaBuffer.getInt();

					// Call getImage() to get the BLOB data
					List<String> images = null;
					try {
						images = getImage(columnName + "_metadata", columnName);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (rowid <= images.size()) {
						values.add(images.get(rowid.intValue() - 1));
					} else {
						values.add(null);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN  "+values);
		return values;
	}
	
	
	public String getImageWithID(String metadataFile, String dataFile, long position) throws Exception {
		String result = "";

		try (FileChannel metaChannel = FileChannel.open(Paths.get(directory + tableName + "/" + metadataFile),
				StandardOpenOption.READ);
				FileChannel dataChannel = FileChannel.open(Paths.get(directory + tableName + "/" + dataFile),
						StandardOpenOption.READ)) {

			// Read header: first 16 bytes (8 for rowCount, 8 for headerOffset)
			ByteBuffer headerBuffer = ByteBuffer.allocate(16);
			if (metaChannel.read(headerBuffer, 0) < 16) {
				System.out.println("Metadata file is empty!");
				return result;
			}
			headerBuffer.flip();
			long rowCount = headerBuffer.getLong();
			long headerOffset = headerBuffer.getLong();

			System.out.println("Row count: " + rowCount);
			System.out.println("Header offset: " + headerOffset);

			int recordSize = getRowSize("BLOB");
			if (recordSize <= 0) {
				System.out.println("Invalid record size!");
				return result;
			}

			// Read the length of the image at the given position
			dataChannel.position(position);
			ByteBuffer lengthBuffer = ByteBuffer.allocate(4); // Assuming image size is stored as an int (4 bytes)
			if (dataChannel.read(lengthBuffer) < 4) {
				System.out.println("Could not read image length!");
				return result;
			}
			lengthBuffer.flip();
			int imageLength = lengthBuffer.getInt();

			if (imageLength <= 0) {
				System.out.println("Invalid image length: " + imageLength);
				return result;
			}

			// Read the actual image data
			ByteBuffer dataBuffer = ByteBuffer.allocate(imageLength);
			if (dataChannel.read(dataBuffer) < imageLength) {
				System.out.println("Incomplete image data read!");
				return result;
			}

			dataBuffer.flip();
			byte[] rawData = dataBuffer.array();

			// Decrypt the image and encode it as Base64
			byte[] decryptedData = decryptData(rawData, SECRET_KEY);
			result = Base64.getEncoder().encodeToString(decryptedData);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	/// aggerate fumction selecting

	public List<E> applyAggregateFunctionsWithWhereCondition(List<String> listOfColumns,
			Map<String, List<String>> columnFunctionMap, List<ConditionGroup> conditiongroups) {

		List<E> result = new ArrayList<>();
		List<E> values = null;
		List<List<Long>> listOfsetRowNumbers = new ArrayList<>();
		List<String> logicalOperator = new ArrayList<>();
		List<String> aggregateColumns = new ArrayList<>();

		// Collect Row IDs based on conditions
		for (ConditionGroup group : conditiongroups) {
			List<Long> rows = iterateGroupCondition(group);
			listOfsetRowNumbers.add(rows);
			logicalOperator.add(group.getLogicalOperator());
		}

		if (listOfsetRowNumbers.isEmpty()) {
			System.out.println("No matching rows found.");
			return result; // Returning an empty list instead of null
		}

		// Compute final result set based on logical operators
		Set<Long> finalResultSet = new HashSet<>(listOfsetRowNumbers.get(0));
		for (int i = 1; i < listOfsetRowNumbers.size(); i++) {
			if ("AND".equalsIgnoreCase(logicalOperator.get(i - 1))) {
				finalResultSet.retainAll(listOfsetRowNumbers.get(i)); // INTERSECTION for AND
			} else {
				finalResultSet.addAll(listOfsetRowNumbers.get(i)); // UNION for OR
			}
		}

		List<Long> finalRowList = new ArrayList<>(finalResultSet);

		if (finalRowList.isEmpty()) {
			System.out.println("No rows match the conditions.");
			return null; // No matching rows after conditions, return null
		}

		// Iterate over columnFunctionMap where each column has a list of aggregate
		// functions
		for (Map.Entry<String, List<String>> entry : columnFunctionMap.entrySet()) {
			String columnName = entry.getKey();
			List<String> functionNames = entry.getValue(); // List of functions for this column
			aggregateColumns.add(columnName);

			for (Column column : columnsArray) {
				if (!column.getName().equals(columnName)) {
					continue;
				}

				String datatype = column.getDataType();
				values = (List<E>) getValues(finalRowList, columnName, datatype);

				// If values is null or empty, no need to process
				if (values == null || values.isEmpty()) {
					System.out.println("No data found for column: " + columnName);
					continue;
				}

				for (String functionName : functionNames) { // Iterate over multiple functions
					switch (functionName.toUpperCase()) {
					case "COUNT":
						String countColumn = datatype.equals("STRING") ? columnName + "_metadata" : columnName;
						List<E> result1 = new ArrayList<>();

						System.out.println("RRRRRRRRRRowcount " + values.size());

						result1.add((E) (values.size() + ""));
						result.add((E) result1); // Cast Long to E safely
						break;

					case "SUM":
						if (isNumericType(datatype)) {
							List<E> result2 = new ArrayList<>();
							result2.add((E) calculateSum(values, datatype));

							result.add((E) result2);
						}
						break;

					case "AVG":
						if (isNumericType(datatype)) {
							List<E> result3 = new ArrayList<>();
							result3.add((E) calculateAverage(values, datatype));
							result.add((E) result3);
						}
						break;

					case "MAX":
						List<E> result3 = new ArrayList<>();
						result3.add(calculateMax(values));
						result.add((E) result3);
						break;

					case "MIN":
						List<E> result4 = new ArrayList<>();
						result4.add(calculateMin(values));

						result.add((E) result4);
						break;

					default:
						System.out.println("Unsupported function: " + functionName);
					}
				}
			}
		}

		if (!listOfColumns.containsAll(aggregateColumns)) {
			System.out.println("Not all aggregate columns are present in listOfColumns.");
			return null; 
		}

		return result;
	}

	public List<E> applyAggregateFunctions(List<String> listOfColumns, Map<String, List<String>> array1) {
		List<E> result = new ArrayList<>();
		List<E> values = null;

		System.out.println("ColumnFunctionMap: " + array1);

		List<String> aggregateColumns = new ArrayList<>();

		for (Map.Entry<String, List<String>> entry : array1.entrySet()) {
			String columnName = entry.getKey();
			List<String> functions = entry.getValue();
			aggregateColumns.add(columnName);

			for (Column column : columnsArray) {
				if (!column.getName().equals(columnName)) {
					continue;
				}

				String datatype = column.getDataType();
				values = null; // Reset values to avoid using stale data

				// Read column values
				if (datatype.equals("STRING")) {
					values = (List<E>) readingStringFile(columnName + "_metadata", columnName);
				} else {
					values = (List<E>) readColumnDataAsList(columnName, datatype);
				}

				System.out.println("Column: " + columnName + ", Values: " + values);

				// Apply aggregate functions
				for (String functionName : functions) {

					switch (functionName.toUpperCase()) {
					case "COUNT":
						String countColumn = datatype.equals("STRING") ? columnName + "_metadata" : columnName;
						List<E> result1 = new ArrayList<>();

						System.out.println("RRRRRRRRRRowcount " + values.size());

						result1.add((E) (values.size() + ""));
						result.add((E) result1); // Cast Long to E safely
						break;

					case "SUM":
						if (isNumericType(datatype)) {
							List<E> result2 = new ArrayList<>();
							result2.add((E) calculateSum(values, datatype));

							result.add((E) result2);
						}
						break;

					case "AVG":
						if (isNumericType(datatype)) {
							List<E> result3 = new ArrayList<>();
							result3.add((E) calculateAverage(values, datatype));
							result.add((E) result3);
						}
						break;

					case "MAX":
						List<E> result3 = new ArrayList<>();
						result3.add(calculateMax(values));
						result.add((E) result3);
						break;

					case "MIN":
						List<E> result4 = new ArrayList<>();
						result4.add(calculateMin(values));

						result.add((E) result4);
						break;

					default:
						System.out.println("Unsupported function: " + functionName);
					}
				}
			}
		}

		System.out.println("----------------------------------------------------------------list of Columns   "
				+ listOfColumns + "  aggreate columns " + aggregateColumns);
		if (!aggregateColumns.containsAll(listOfColumns)) {

			System.out.println("Not all aggregate columns are present in listOfColumns.");
			return null; // Return null if any column is missing
		}

		System.out.println("AAAAAAAAAAAAAAAAggggggggreate function  " + result);

		return result;

	}

	private boolean isNumericType(String datatype) {
		return datatype.equals("INT") || datatype.equals("FLOAT") || datatype.equals("DOUBLE");
	}

	private E calculateSum(List<E> numbers, String datatype) {
		if (numbers.isEmpty())
			return null;

		if (datatype.equals("FLOAT") || datatype.equals("DOUBLE")) {
			Double sum = numbers.stream().mapToDouble(e -> ((Number) e).doubleValue()).sum();
			return (E) sum;
		} else if (datatype.equals("INT")) {
			Integer sum = numbers.stream().mapToInt(e -> ((Number) e).intValue()).sum();
			return (E) sum;
		}
		return null;
	}

	private E calculateAverage(List<E> numbers, String datatype) {
		if (numbers.isEmpty())
			return null;

		if (datatype.equals("FLOAT") || datatype.equals("DOUBLE")) {
			Double avg = numbers.stream().mapToDouble(e -> ((Number) e).doubleValue()).average().orElse(0.0);
			return (E) avg;
		} else if (datatype.equals("INT")) {
			Double avg = numbers.stream().mapToInt(e -> ((Number) e).intValue()).average().orElse(0.0);
			return (E) avg; // Return Double to keep precision
		}
		return null;
	}

	private E calculateMax(List<E> values) {
		if (values.isEmpty())
			return null;
		if (!(values.get(0) instanceof Comparable))
			return null; // Ensure it's comparable
		return values.stream().max((Comparator<? super E>) Comparator.naturalOrder()).orElse(null);
	}

	private E calculateMin(List<E> values) {
		if (values.isEmpty())
			return null;
		if (!(values.get(0) instanceof Comparable))
			return null; // Ensure it's comparable
		return values.stream().min((Comparator<? super E>) Comparator.naturalOrder()).orElse(null);
	}

	// selecting image

	public <E> ArrayList<E> getImage(String metadataFile, String dataFile) throws Exception {
		ArrayList<E> result = new ArrayList<>();

		try (FileChannel metaChannel = FileChannel.open(Paths.get(directory + tableName + "/" + metadataFile),
				StandardOpenOption.READ);
				FileChannel dataChannel = FileChannel.open(Paths.get(directory + tableName + "/" + dataFile),
						StandardOpenOption.READ)) {

			// Read header: first 16 bytes (8 for rowCount, 8 for headerOffset)
			ByteBuffer headerBuffer = ByteBuffer.allocate(16);
			if (metaChannel.read(headerBuffer, 0) < 16) {
				System.out.println("Metadata file is empty!");
				return result;
			}
			headerBuffer.flip();
			long rowCount = headerBuffer.getLong();
			long headerOffset = headerBuffer.getLong();

			System.out.println("Row count: " + rowCount);
			System.out.println("Header offset: " + headerOffset);

			int recordSize = getRowSize("STRING");
			if (recordSize <= 0) {
				return result;
			}

			long metaFileSize = metaChannel.size();

			for (int i = 0; i < rowCount; i++) {
				long recordPosition = 16 + (i * recordSize);
				if (metaFileSize < recordPosition + recordSize) {
					break;
				}

				ByteBuffer rowBuffer = ByteBuffer.allocate(recordSize);
				if (metaChannel.read(rowBuffer, recordPosition) < recordSize) {
					System.err.println("Incomplete record read at position " + recordPosition);
					break;
				}
				rowBuffer.flip();

				long rowId = rowBuffer.getLong();
				byte isDeleted = rowBuffer.get();
				byte isNull = rowBuffer.get();
				long rowRelativeOffset = rowBuffer.getLong();
				int length = rowBuffer.getInt();

				System.out.println("Row ID: " + rowId + ", Offset: " + rowRelativeOffset + ", Length: " + length);

				if (isDeleted == 1)
					continue;
				if (isNull == 1) {
					result.add(null);
					continue;
				}
				if (length == 0) {
					result.add((E) "");
					continue;
				}

				ByteBuffer dataBuffer = ByteBuffer.allocate(length);
				dataChannel.position(rowRelativeOffset);
				int bytesRead = dataChannel.read(dataBuffer);
				if (bytesRead < 0)
					continue;

				dataBuffer.flip();
				byte[] rawData = dataBuffer.array();

				// Decrypt the image and encode it as Base64
				byte[] decryptedData = decryptData(rawData, SECRET_KEY);
				String base64Image = Base64.getEncoder().encodeToString(decryptedData);
				result.add((E) base64Image);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}
	// selecting with orderBy

	public List<E> getRecordsusingOrderBy(List<String> listOfColumns, OrderBy orderBy) {

		System.out.println("#############################################################");

		String columnName = null;
		String datatype = null;
		HashMap<Integer, Object> records = null;
		List<E> sortedListRecords = new ArrayList<>();

		for (Column column : columnsArray) {
			columnName = column.getName();
			if (columnName.equals(orderBy.getColumnName())) {
				datatype = column.getDataType();
			}
		}

		System.out.println("KKKKKKKKKKKKKKKKKKKKKKKKKKK   " + datatype);

		if (datatype.equals("STRING")) {

			System.out.println("YYYYYYYYYYYYYYYYYYYYYYYYYYYYYY  " + datatype);
			records = readingStringFileAsMap(orderBy.getColumnName() + "_metadata", columnName);

			System.out.println("RRRRRRRRecrsds   " + records);

		} else {
			System.out.println("   ddddddddddddddddd " + orderBy.getColumnName() + "     " + datatype);
			records = (HashMap<Integer, Object>) readingDatasAsMap(orderBy.getColumnName(), datatype);
		}
		boolean isAscending = orderBy.getOrderDirection().equalsIgnoreCase("ASC");
		// Filter out null values before sorting
		Map<Integer, Object> filteredRecords = records.entrySet().stream().filter(entry -> entry.getValue() != null) // Only
																														// keep
																														// non-null
																														// values
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Map<Integer, Object> sortedRecords = HashMapSorting.sortByValue(filteredRecords, isAscending);

		System.out.println("==================  sorted Records========= " + sortedRecords);
		List<Long> listOfRowId = new ArrayList<>();
		for (Integer key : sortedRecords.keySet()) {
			listOfRowId.add(key.longValue() + 1);
		}

		for (String column : listOfColumns) {
			for (Column column1 : columnsArray) {
				columnName = column1.getName();
				datatype = column1.getDataType();
				if (columnName.equals(column)) {
					System.out.println("------------0000000resulttttt0000---------- "
							+ getValues(listOfRowId, columnName, datatype));

					sortedListRecords.add((E) getValues(listOfRowId, columnName, datatype));
				}
			}
		}

		return sortedListRecords;

	}

	public List<E> getRecordsusingOrderByWithWhereCondition(List<String> listOfColumns,
			List<ConditionGroup> conditionGroups, OrderBy orderBy) {
		String columnName = null;
		String datatype = null;
		HashMap<Long, E> records = null;
		List<String> logicalOperator = new ArrayList<>();
		List<List<Long>> listOfRowNumbers = new ArrayList<>();
		List<E> sortedListRecords = new ArrayList<>();
		for (ConditionGroup group : conditionGroups) {
			List<Long> rows = iterateGroupCondition(group);
			listOfRowNumbers.add(rows);
			logicalOperator.add(group.getLogicalOperator());
		}

		List<Set<Long>> andGroups = new ArrayList<>();
		Set<Long> currentAndSet = new HashSet<>(listOfRowNumbers.get(0));

		for (int i = 1; i < listOfRowNumbers.size(); i++) {
			if ("AND".equals(logicalOperator.get(i - 1))) {
				currentAndSet.retainAll(listOfRowNumbers.get(i));
			} else {

				andGroups.add(new HashSet<>(currentAndSet));

				currentAndSet = new HashSet<>(listOfRowNumbers.get(i));
			}
		}

		andGroups.add(new HashSet<>(currentAndSet));
		Set<Long> finalResultSet = new HashSet<>();
		for (Set<Long> group : andGroups) {
			finalResultSet.addAll(group);
		}
		List<Long> finalRows = new ArrayList<>(finalResultSet);

		System.out.println("========== final Rowss================  " + finalRows);

		for (Column column : columnsArray) {
			columnName = column.getName();
			if (columnName.equals(orderBy.getColumnName())) {
				datatype = column.getDataType();
			}
		}

		records = getValuesWithMap(finalRows, columnName, datatype);

		boolean isAscending = orderBy.getOrderDirection().equalsIgnoreCase("ASC");
		Map<Long, E> sortedRecords = HashMapSorting.sortByValue(records, isAscending);

		System.out.println("==================  sorted Records========= " + sortedRecords);
		List<Long> listOfRowId = new ArrayList<>();
		for (Long key : sortedRecords.keySet()) {
			listOfRowId.add(key.longValue());
		}

		System.out.println("listOfColumns: " + listOfColumns);
		System.out.println("conditionGroups: " + conditionGroups);

		for (String column : listOfColumns) {
			for (Column column1 : columnsArray) {
				columnName = column1.getName();
				datatype = column1.getDataType();
				if (columnName.equals(column)) {
					System.out.println("------------0000000resulttttt0000---------- "
							+ getValues(listOfRowId, columnName, datatype));

					sortedListRecords.add((E) getValues(listOfRowId, columnName, datatype));
				}
			}
		}

		return sortedListRecords;
	}

	public boolean addColumn(Column column) throws IOException {
		String pathString = directory + tableName;
		File folder = new File(pathString);
		String columnName = columnsArray.get(0).getName();

		long rowCount = 0;

		if (columnsArray.get(0).getDataType().equals("STRING") || columnsArray.get(0).getDataType().equals("BLOB")) {
			if (new File(pathString + "/" + columnName + "_metadata").length() != 0) {
				rowCount = getRowCount(columnName + "_metadata");
			}
		} else {
			if (new File(pathString + "/" + columnName).length() != 0) {
				rowCount = getRowCount(columnName);
			}
		}
		HashMap<String, E> datas = new HashMap<String, E>();
		ArrayList<String> files = new ArrayList<>(
				Arrays.asList(folder.list((dir, name) -> !name.toLowerCase().contains("metadata"))));

		if (files.contains(column.getName())) {
			return false;
		}

		if (column.getName().equals("Metadata") || column.getName().contains("metadata")) {
			return false;
		}
		boolean autCheck = true;
		if (column.getConstraints() != null && !column.getConstraints().isEmpty()) {
			for (Constraint cons : column.getConstraints()) {
				if (cons.getType() != null) {
					if (cons.getType().equals("PK") && rowCount < 2) {

						for (Column col : columnsArray) {
							for (Constraint con : col.getConstraints()) {
								if (con != null) {
									if (con.getType() != null && con.getType().equals("PK")) {
										System.out.println("Column===check2");
										return false;
									}
								}
							}
						}
					} else if (cons.getType().equals("AUT")) {
						autCheck = false;
						if (!column.getDataType().equals("INT") && !column.getDataType().equals("FLOAT")) {
							return false;
						} else {
							for (Constraint con : column.getConstraints()) {
								if (con.getType().equals("PK") || con.getType().equals("UK")) {
									autCheck = true;
									break;
								}
							}
						}
					} else if (cons.getType().equals("FK")) {
						boolean check = false;
						if (cons.getReferenceTable().equals(tableName)
								&& cons.getReferenceColumn().equals(column.getName())) {
							return false;
						}
						loop: for (String tabName : new Database(database.getDabaseName())
								.getTables(user.getUsername())) {
							List<Column> columnsCheck = new TableDAO(user, database.getDabaseName(),
									tabName).columnsArray;
							for (Column col : columnsCheck) {
								for (Constraint constr : col.getConstraints()) {
									if (constr.getType().equals("PK")
											&& col.getDataType().equals(column.getDataType())) {
										check = true;
										break loop;
									}
								}
							}
						}
						if (!check) {
							return false;
						}
					}
				}
			}
		}
		if (!autCheck) {
			return false;
		}
		String filePath = pathString + "/" + column.getName();

		try (FileChannel channel = FileChannel.open(Paths.get(pathString + "/Metadata"), StandardOpenOption.CREATE,
				StandardOpenOption.APPEND)) {

			ByteBuffer buffer = ByteBuffer.allocate(1024);
			writeMetaData(buffer, column.getName()); // Writing column name
			writeMetaData(buffer, column.getDataType()); // Writing column data type
			if (Files.exists(Paths.get(filePath))) {
				System.out.println("Column===check5");

				return false;
			} else {
				try (FileOutputStream fileOut = new FileOutputStream(filePath)) {

				} catch (IOException e) {

					e.printStackTrace();
				}
			}
			if (column.getDataType().equals("STRING") || column.getDataType().equals("BLOB")) {
				try (FileOutputStream fileOut = new FileOutputStream(
						pathString + "/" + column.getName() + "_metadata")) {

				} catch (IOException e) {

					e.printStackTrace();
				}

			}
			// Serialize Constraint object
			try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

				objectOutputStream.writeObject(column.getConstraints()); // Serialize the list
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
			buffer.flip();
			while (buffer.hasRemaining()) {
				channel.write(buffer);
			}
			buffer.clear();

			if (column.getConstraints() != null && !column.getConstraints().isEmpty()) {

				for (Constraint constraint : column.getConstraints()) {

					for (int i = 1; i < rowCount; i++) {
						Map<String, E> datas1 = new HashMap<>(); // New map for each row

						if (constraint.getType().equals("PK") || constraint.getType().equals("NN")) {
							datas1.put(column.getName(), (E) ""); // Empty string for PK/NN
						} else if (constraint.getType().equals(("DEF"))) {
							datas1.put(column.getName(), (E) constraint.getDefault());
						}

						else {
							datas1.put(column.getName(), (E) "NONE"); // Default value for other constraints
						}

						insertValue((HashMap<String, E>) datas1); // Insert the value
					}

				}
			} else {
				for (int i = 1; i < rowCount; i++) {

					System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
					Map<String, E> datas1 = new HashMap<>(); // New map for each row
					datas1.put(column.getName(), (E) "NONE"); // Assign empty value per row
					insertValue((HashMap<String, E>) datas1);
				}
			}

		}

		columnsArray = readMetadata();
		this.table.columns = columnsArray;
		System.out.println(columnsArray);
		return true;
	}

	private void shiftData(FileChannel channel, long position, long extraBytes) throws IOException {
		long fileSize = channel.size();
		long readPos = fileSize;
		long writePos = fileSize + extraBytes;

		ByteBuffer buffer = ByteBuffer.allocate(4096);

		// Shift data backward
		while (readPos > position) {
			int chunkSize = (int) Math.min(4096, readPos - position);
			buffer.clear();
			channel.position(readPos - chunkSize);
			channel.read(buffer);
			buffer.flip();

			channel.position(writePos - chunkSize);
			channel.write(buffer);

			readPos -= chunkSize;
			writePos -= chunkSize;
		}

		channel.truncate(fileSize + extraBytes); // Adjust file size
	}

	private static void writeMetaData(ByteBuffer buffer, String value) {
		byte[] stringBytes = value.getBytes(StandardCharsets.UTF_8);
		buffer.putInt(stringBytes.length);
		buffer.put(stringBytes);
	}

	public boolean changeColumnDataType(String columnName, String newDataType) {
		String oldDatatype = null;
		long rowCount = getRowCount(columnName);

		for (Column col : columnsArray) {
			if (col.getName().equals(columnName)) {
				oldDatatype = col.getDataType();

				if (col.getDataType().equals(newDataType)) {
					return true;
				}
				for (Constraint cons : col.getConstraints()) {
					if (cons.getType() != null && cons.getType().equals("PK")) {
						for (String tabName : database.getTables(user.getUsername())) {
							List<Column> columnsCheck = new TableDAO(user, database.getDabaseName(),
									tabName).columnsArray;
							for (Column cols : columnsCheck) {
								for (Constraint constr : cols.getConstraints()) {
									if (constr.getType().equals("FK") && constr.getReferenceTable().equals(tableName)
											&& constr.getReferenceColumn().equals(columnName)) {
										return false;
									}
								}
							}
						}
					}
				}
			}
		}

		if (oldDatatype != null && newDataType != null
				&& ((rowCount == 0) || (oldDatatype.equals("INT") && newDataType.equals("FLOAT"))
						|| ((oldDatatype.equals("INT") || oldDatatype.equals("FLOAT"))
								&& (newDataType.equals("STRING") || newDataType.equals("BLOB"))))) {
			try (FileChannel channel = FileChannel.open(
					Paths.get(user.getHomeDirectory() + "/" + database.getDabaseName() + "/" + tableName + "/Metadata"),
					StandardOpenOption.READ, StandardOpenOption.WRITE)) {

				ByteBuffer buffer = ByteBuffer.allocate(4096); // Read in chunks
				long position = 0;
				boolean columnUpdated = false;

				while (channel.read(buffer) > 0) {
					buffer.flip(); // Prepare for reading

					while (buffer.hasRemaining()) {
						long columnStartPos = position; // Track start of column
						String existingColumnName = readString(buffer);
						position += existingColumnName.length() + 4; // Move position

						if (existingColumnName.equals(columnName)) {
							// Found the column - Read old datatype
							int oldDataTypeLength = buffer.getInt();
							byte[] oldDataTypeBytes = new byte[oldDataTypeLength];
							buffer.get(oldDataTypeBytes);
							position += 4 + oldDataTypeLength; // Move position

							byte[] newDataTypeBytes = newDataType.getBytes();
							int extraBytes = newDataTypeBytes.length - oldDataTypeLength;

							if (extraBytes != 0) {
								shiftData(channel, position, extraBytes); // Adjust file structure
							}

							// Write updated datatype
							ByteBuffer writeBuffer = ByteBuffer.allocate(4 + newDataTypeBytes.length);
							writeBuffer.putInt(newDataTypeBytes.length);
							writeBuffer.put(newDataTypeBytes);
							writeBuffer.flip();

							channel.position(columnStartPos + existingColumnName.length() + 4);
							channel.write(writeBuffer);

							columnUpdated = true;
							break;
						}

						// Skip the rest of the column's metadata
						int dataTypeLength = buffer.getInt();
						buffer.position(buffer.position() + dataTypeLength);
						int constraintLength = buffer.getInt();
						buffer.position(buffer.position() + constraintLength);

						position += 4 + dataTypeLength + 4 + constraintLength;
						System.out.println("==============");

					}

					if (columnUpdated)
						break;
					buffer.clear();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println(
				"---tttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt----------------------------------------------------------------------");
		try {
			convertDataTypeValues(columnName, oldDatatype, newDataType, rowCount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean convertDataTypeValues(String columnName, String oldDatatype, String newDatatype, long rowCount)
			throws IOException {
		Path filePath = Paths.get(directory, tableName, columnName + "_metadata");
		System.out.println("New DataType: " + newDatatype);
		System.out.println("Old DataType: " + oldDatatype);

		// If the old data type is BLOB or STRING and there are no rows, delete metadata
		// file
		if (oldDatatype != null && (oldDatatype.equals("BLOB") || oldDatatype.equals("STRING")) && rowCount == 0) {
			try {
				Files.deleteIfExists(filePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}

		if (oldDatatype != null && ((oldDatatype.equals("INT") && newDatatype.equals("STRING"))
				|| (oldDatatype.equals("FLOAT") && newDatatype.equals("STRING"))
				|| (oldDatatype.equals("INT") && newDatatype.equals("FLOAT")))) {

			// Read column data as List<Object> instead of <E>
			List<Object> records = (List<Object>) readColumnDataAsList(columnName, oldDatatype);

			// Debugging: Print original records
			System.out.println("Original Records: " + records);

			List<Object> modifiedRecords = new ArrayList<>();
			System.out.println("------------------zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
			if (newDatatype.equalsIgnoreCase("STRING")) {
				modifiedRecords = records.stream().filter(Objects::nonNull) // Remove null values
						.map(Object::toString) // Convert to String
						.collect(Collectors.toList());
				System.out.println("Converted to String: " + modifiedRecords);

			} else if (newDatatype.equalsIgnoreCase("FLOAT")) {
				modifiedRecords = records.stream().filter(Objects::nonNull) // Remove null values
						.map(e -> {
							try {
								return Double.valueOf(e.toString());
							} catch (NumberFormatException ex) {
								System.err.println("Conversion error for FLOAT: " + e);
								return null; // Handle conversion failure
							}
						}).filter(Objects::nonNull) // Remove failed conversions
						.collect(Collectors.toList());
				System.out.println("Converted to Float: " + modifiedRecords);

			} else if (newDatatype.equalsIgnoreCase("INT")) {
				modifiedRecords = records.stream().filter(Objects::nonNull) // Remove null values
						.map(e -> {
							try {
								return Integer.valueOf(e.toString());
							} catch (NumberFormatException ex) {
								System.err.println("Conversion error for INT: " + e);
								return null; // Handle conversion failure
							}
						}).filter(Objects::nonNull) // Remove failed conversions
						.collect(Collectors.toList());
				System.out.println("Converted to Integer: " + modifiedRecords);
			}

			System.out.println("   mmmmmodofeied RRRRwocrds  " + modifiedRecords);
			Path path = Paths.get(directory + "/" + tableName + "/" + columnName);
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (newDatatype.equals("STRING")) {
				try {
					Files.createFile(Paths.get(path + "_metadata"));

				} catch (IOException e) {
				}
			}

			Files.createFile(Paths.get(directory + "/" + tableName + "/" + columnName));

			// Fix HashMap to use Object instead of E
			HashMap<String, Object> result = new HashMap<>();
			for (Object value : modifiedRecords) {
				result.put(columnName, value);
				System.out.println("  resulsteeeeeeeeeeee1 ===  >  " + result);
				insertValue((HashMap<String, E>) result);
			}

			System.out.println("000000000000   " + path);
//	        
//	        

			System.out.println("  resulsteeeeeeeeeeee2 ===  >  " + result);
			// Use a wildcard generic method or modify insertValue signature
			// Ensure insertValue() accepts HashMap<String, Object>

		}

		return true;
	}

	public boolean renameColumn(String oldColumnName, String newColumnName) {

		String pathString = user.getHomeDirectory() + "/" + database.getDabaseName() + "/" + tableName;
		Path metaPath = Paths.get(pathString + "/Metadata");

		try (FileChannel channel = FileChannel.open(metaPath, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			ByteBuffer buffer = ByteBuffer.allocate(4096);
			channel.read(buffer);
			buffer.flip();

			long columnStartPos = -1;

			while (buffer.hasRemaining()) {
				long currentColumnPos = channel.position() - buffer.remaining(); // Track column position
				String currentColumn = readString(buffer);

				int dataTypeLength = buffer.getInt();
				buffer.position(buffer.position() + dataTypeLength); // Skip DataType

				int constraintLength = buffer.getInt();
				buffer.position(buffer.position() + constraintLength); // Skip Constraints

				if (currentColumn.equals(oldColumnName)) {
					columnStartPos = currentColumnPos;
					break;
				}
			}

			if (columnStartPos == -1) {
				return false; // Column not found
			}

			// Move channel to the exact column name position and overwrite
			byte[] newColumnBytes = newColumnName.getBytes(StandardCharsets.UTF_8);
			ByteBuffer writeBuffer = ByteBuffer.allocate(4 + newColumnBytes.length);
			writeBuffer.putInt(newColumnBytes.length);
			writeBuffer.put(newColumnBytes);
			writeBuffer.flip();

			channel.position(columnStartPos);
			channel.write(writeBuffer);

			Path source = Paths.get(pathString + "/" + oldColumnName);
			Path target = Paths.get(pathString + "/" + newColumnName);

			try {
				Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
//					e.printStackTrace();
			}

			for (String tabName : database.getTables(user.getUsername())) {
				List<Column> columnsCheck = new TableDAO(user, database.getDabaseName(), tabName).columnsArray;
				for (Column col : columnsCheck) {
					if (col.getDataType().equals("STRING") || col.getDataType().equals("BLOB")) {
						Path sourceMeta = Paths.get(pathString + "/" + oldColumnName + "_metadata");
						Path targetMeta = Paths.get(pathString + "/" + newColumnName + "_metadata");

						try {
							Files.move(sourceMeta, targetMeta, StandardCopyOption.REPLACE_EXISTING);
						} catch (Exception e) {
//								e.printStackTrace();
						}
					}
					for (Constraint constr : col.getConstraints()) {
						if (constr.getType().equals("FK") && constr.getReferenceTable().equals(tableName)
								&& constr.getReferenceColumn().equals(oldColumnName)) {
							changeReferenceColumnName(tabName, col.getName(), oldColumnName, newColumnName);
						}
					}
				}
			}
			this.columnsArray = readMetadata();
			this.table = new Table(tableName, columnsArray);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	boolean changeReferenceColumnName(String tabName, String columnName, String oldRefColumnName,
			String newRefColumnName) {
		Path metaPath = Paths.get(user.getHomeDirectory(), database.getDabaseName(), tabName, "Metadata");

		try (RandomAccessFile file = new RandomAccessFile(metaPath.toFile(), "rw");
				FileChannel channel = file.getChannel()) {

			ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
			channel.read(buffer);
			buffer.flip();

			long constraintStartPos = -1;
			int constraintLength = -1;
			List<Constraint> constraints = null;

			while (buffer.hasRemaining()) {
				long columnStartPos = channel.position() - buffer.remaining();
				String currentColumn = readString(buffer); // Read column name

				int dataTypeLength = buffer.getInt();
				buffer.position(buffer.position() + dataTypeLength); // Skip DataType

				constraintLength = buffer.getInt();
				constraintStartPos = channel.position();
				constraints = (List<Constraint>) readObject(buffer); // Read Constraint List

				// Check if this is the correct column
				if (currentColumn.equals(columnName) && constraints != null) {
					boolean updated = false;

					for (Constraint constraint : constraints) {
						if (constraint.getReferenceColumn().equals(oldRefColumnName)) {
							constraint.setReferenceColumn(newRefColumnName);
							updated = true;
						}
					}

					if (updated) {
						writeConstraint(channel, constraintStartPos, constraintLength, constraints);
						return true;
					}
					break; // Stop searching after finding the correct column
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean dropColumn(String columnName) {
		System.out.println(columnName + "++++++++++++++++++++====================================asdfghjhxzcvghj"
				+ columnsArray.size());
		if (columnsArray.size() < 2) {
			System.out.println("less size+++++++++++++++++++++=================");
			return false;
		}
		for (Column column : columnsArray) {
			if (column.getName().equals(columnName)) {
				for (Constraint cons : column.getConstraints()) {
					if (cons.getType().equals("PK")) {
						for (String tabName : database.getTables(user.getUsername())) {
							List<Column> columnsCheck = new TableDAO(user, database.getDabaseName(),
									tabName).columnsArray;
							for (Column col : columnsCheck) {
								for (Constraint constr : col.getConstraints()) {
									if (constr != null && constr.getType() != null) {
										if (constr.getType().equals("FK")
												&& constr.getReferenceTable().equals(tableName)
												&& constr.getReferenceColumn().equals(columnName)) {
											return false;
										}
									}
								}
							}
						}
					}
				}
			}
		}

		Path metaPath = Paths
				.get(user.getHomeDirectory() + "/" + database.getDabaseName() + "/" + tableName + "/Metadata");
		Path tempMetaPath = Paths
				.get(user.getHomeDirectory() + "/" + database.getDabaseName() + "/" + tableName + "/Metadata_temp");

		try (FileChannel readChannel = FileChannel.open(metaPath, StandardOpenOption.READ);
				FileChannel writeChannel = FileChannel.open(tempMetaPath, StandardOpenOption.CREATE,
						StandardOpenOption.WRITE)) {

			ByteBuffer buffer = ByteBuffer.allocate(4096);
			boolean columnFound = false;

			while (readChannel.read(buffer) > 0) {
				buffer.flip();

				while (buffer.hasRemaining()) {
					int startPosition = buffer.position(); // Save position before reading
					String currentColumn = readString(buffer);
					int dataTypeLength = buffer.getInt();
					byte[] dataTypeBytes = new byte[dataTypeLength];
					buffer.get(dataTypeBytes); // Read DataType

					int constraintLength = buffer.getInt();
					byte[] constraintBytes = new byte[constraintLength];
					buffer.get(constraintBytes); // Read Constraints

					if (currentColumn.equals(columnName)) {
						columnFound = true;

						continue; // Skip writing this column → effectively deletes it
					}

					// Write column to metadata
					writeString(writeChannel, currentColumn);
					writeInt(writeChannel, dataTypeLength);
					writeChannel.write(ByteBuffer.wrap(dataTypeBytes));
					writeInt(writeChannel, constraintLength);
					writeChannel.write(ByteBuffer.wrap(constraintBytes));
				}
				buffer.clear();
			}

			if (!columnFound) {
				Files.deleteIfExists(tempMetaPath);
				return false; // Column not found, no need to replace the metadata file
			}

			// Replace old metadata file with the new one
			Files.move(tempMetaPath, metaPath, StandardCopyOption.REPLACE_EXISTING);
			this.columnsArray = readMetadata();
			this.table = new Table(tableName, columnsArray);
			Path deleteFilePath = Paths.get(directory + "/" + tableName + "/" + columnName);

			Files.deleteIfExists(deleteFilePath);
			return true;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private void writeConstraint(FileChannel channel, long constraintPos, int oldLength, List<Constraint> constraints)
			throws IOException {
		// Step 1: Serialize the List<Constraint>
		byte[] newConstraintBytes;
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

			objectOutputStream.writeObject(constraints);
			objectOutputStream.flush();
			newConstraintBytes = byteArrayOutputStream.toByteArray();
		}

		// Step 2: Check if we need to shift data
		if (newConstraintBytes.length < oldLength) {
			shiftDataBackward(channel, constraintPos + oldLength, channel.size(),
					oldLength - newConstraintBytes.length);
		} else if (newConstraintBytes.length > oldLength) {
			shiftDataForward(channel, constraintPos + oldLength, channel.size(), newConstraintBytes.length - oldLength);
		}

		// Step 3: Write new constraints
		ByteBuffer writeBuffer = ByteBuffer.allocate(newConstraintBytes.length + 4);
		writeBuffer.putInt(newConstraintBytes.length);
		writeBuffer.put(newConstraintBytes);
		writeBuffer.flip();

		channel.position(constraintPos - 4);
		channel.write(writeBuffer);

		// Step 4: Truncate file if needed
		channel.truncate(channel.size() - (oldLength - newConstraintBytes.length));
	}

//
	private void shiftDataForward(FileChannel channel, long readPos, long fileSize, long shiftAmount)
			throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(8192);
		long writePos = readPos + shiftAmount;

		while (readPos < fileSize) {
			buffer.clear();
			channel.position(readPos);
			int bytesRead = channel.read(buffer);
			if (bytesRead == -1)
				break;

			buffer.flip();
			channel.position(writePos);
			channel.write(buffer);

			readPos += bytesRead;
			writePos += bytesRead;
		}
	}

	private void shiftDataBackward(FileChannel channel, long readPos, long fileSize, long shiftAmount)
			throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(8192);

		while (readPos < fileSize) {
			buffer.clear();
			channel.position(readPos);
			int bytesRead = channel.read(buffer);
			if (bytesRead == -1)
				break;

			buffer.flip();
			channel.position(readPos - shiftAmount);
			channel.write(buffer);

			readPos += bytesRead;
		}
	}

	private void writeString(FileChannel channel, String data) throws IOException {
		byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + bytes.length);
		buffer.putInt(bytes.length);
		buffer.put(bytes);
		buffer.flip();
		channel.write(buffer);
	}

	private void writeInt(FileChannel channel, int value) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.putInt(value);
		buffer.flip();
		channel.write(buffer);
	}

	public boolean addConstraint(String columnName, Constraint newConstraint) {
		Column column = null;
		if (newConstraint.getType().equals("PK")) {
			if (table.columns != null) {
				for (Column col : table.columns) {
					for (Constraint constraint : col.getConstraints()) {
						if (constraint.getType() != null && constraint.getType().equals("PK")) {
							return false;
						} else if (col.getName().equals(columnName)) {
							if (constraint.getType() != null && constraint.getType().equals(newConstraint.getType())) {
								return true;
							}
							column = col;
						}
					}
				}
			}
		}
		System.out.println(table.columns);

		for (Column col : table.columns) {
			if (col.getName().equals(columnName)) {
				column = col;
				for (Constraint constraint : col.getConstraints()) {
					if (constraint != null && constraint.getType() != null
							&& constraint.getType().equals(newConstraint.getType())) {
						return true;
					}
				}
			}
		}

		ArrayList<E> datasArrayList = new ArrayList<E>();

		if (column.getDataType().equals("String")) {
			datasArrayList = readingFile(columnName + "_metadata", columnName);
		} else {
			datasArrayList = (ArrayList<E>) readColumnDataAsList(columnName, column.getDataType());
		}
		if (newConstraint.getType().equals("PK")) {
			if (datasArrayList.size() != new HashSet<E>(datasArrayList).size() && datasArrayList.contains(null)) {
				return false;
			}
		} else if (newConstraint.getType().equals("UK")) {

			if (datasArrayList.size() != new HashSet<E>(datasArrayList).size()) {
				System.out.println("hello");
				return false;
			}
		} else if (newConstraint.getType().equals("NN")) {
			if (datasArrayList.contains(null)) {
				return false;
			}
		} else if (newConstraint.getType().equals("FK")) {
			if (newConstraint.getReferenceTable().equals(tableName)
					&& newConstraint.getReferenceColumn().equals(columnName)) {
				return false;
			}
			List<E> datas = loadPrimaryKey(newConstraint.getReferenceTable() + "/" + newConstraint.getReferenceColumn(),
					column.getDataType());
			for (E childValue : datasArrayList) {
				if (childValue != null && !datas.contains(childValue)) {
					return false; // Foreign key violation
				}
			}
		}

		Path metaPath = Path.of(user.getHomeDirectory(), database.getDabaseName(), tableName, "Metadata");
		Path tempPath = metaPath.resolveSibling("Metadata.tmp"); // Temporary file

		try (FileChannel readChannel = FileChannel.open(metaPath, StandardOpenOption.READ);
				FileChannel writeChannel = FileChannel.open(tempPath, StandardOpenOption.CREATE,
						StandardOpenOption.WRITE)) {

			ByteBuffer buffer = ByteBuffer.allocate((int) readChannel.size());
			readChannel.read(buffer);
			buffer.flip();

			while (buffer.hasRemaining()) {
				long columnStartPos = buffer.position(); // Track start of column entry

				// Read column name length
				if (buffer.remaining() < 4)
					return false;
				int columnNameLength = buffer.getInt();

				// Read column name
				if (buffer.remaining() < columnNameLength)
					return false;
				byte[] columnNameBytes = new byte[columnNameLength];
				buffer.get(columnNameBytes);
				String currentColumn = new String(columnNameBytes, StandardCharsets.UTF_8);

				// Read data type length
				if (buffer.remaining() < 4)
					return false;
				int dataTypeLength = buffer.getInt();

				// Read data type
				if (buffer.remaining() < dataTypeLength)
					return false;
				byte[] dataTypeBytes = new byte[dataTypeLength];
				buffer.get(dataTypeBytes);
				String dataType = new String(dataTypeBytes, StandardCharsets.UTF_8);

				// Read constraint length
				if (buffer.remaining() < 4)
					return false;
				int constraintLength = buffer.getInt();

				// Validate constraint length
				if (constraintLength < 0 || constraintLength > buffer.remaining())
					return false;

				// Read constraint object
				byte[] constraintBytes = new byte[constraintLength];
				buffer.get(constraintBytes);

				List<Constraint> constraints = new ArrayList<>();
				try (ObjectInputStream objectInputStream = new ObjectInputStream(
						new ByteArrayInputStream(constraintBytes))) {
					Object obj = objectInputStream.readObject();
					if (obj instanceof List<?>) {
						constraints = (List<Constraint>) obj;
					}
				} catch (Exception e) {
					System.err.println("Error deserializing constraints: " + e.getMessage());
				}

				// Modify constraints if it's the target column
				if (currentColumn.equals(columnName)) {
					constraints.add(newConstraint);
				}

				// Serialize updated constraints
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
				objectStream.writeObject(constraints);
				objectStream.flush();
				byte[] updatedConstraintBytes = byteStream.toByteArray();

				// Write updated data to temp file
				ByteBuffer writeBuffer = ByteBuffer.allocate(
						4 + columnNameBytes.length + 4 + dataTypeBytes.length + 4 + updatedConstraintBytes.length);
				writeBuffer.putInt(columnNameLength);
				writeBuffer.put(columnNameBytes);
				writeBuffer.putInt(dataTypeLength);
				writeBuffer.put(dataTypeBytes);
				writeBuffer.putInt(updatedConstraintBytes.length);
				writeBuffer.put(updatedConstraintBytes);
				writeBuffer.flip();
				writeChannel.write(writeBuffer);
			}

			// Replace old metadata with new one
			Files.move(tempPath, metaPath, StandardCopyOption.REPLACE_EXISTING);

			return true; // Successfully added constraint

		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(tempPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public boolean dropConstraint(String columnName, String constraintToRemove) {

		Path metaPath = Path.of(user.getHomeDirectory(), database.getDabaseName(), tableName, "Metadata");
		Path tempPath = metaPath.resolveSibling("Metadata.tmp"); // Temporary file

		try (FileChannel readChannel = FileChannel.open(metaPath, StandardOpenOption.READ);
				FileChannel writeChannel = FileChannel.open(tempPath, StandardOpenOption.CREATE,
						StandardOpenOption.WRITE)) {

			ByteBuffer buffer = ByteBuffer.allocate((int) readChannel.size());
			readChannel.read(buffer);
			buffer.flip();

			while (buffer.hasRemaining()) {
				long columnStartPos = buffer.position(); // Track start of column entry

				// Read column name length
				if (buffer.remaining() < 4)
					return false;
				int columnNameLength = buffer.getInt();

				// Read column name
				if (buffer.remaining() < columnNameLength)
					return false;
				byte[] columnNameBytes = new byte[columnNameLength];
				buffer.get(columnNameBytes);
				String currentColumn = new String(columnNameBytes, StandardCharsets.UTF_8);

				// Read data type length
				if (buffer.remaining() < 4)
					return false;
				int dataTypeLength = buffer.getInt();

				// Read data type
				if (buffer.remaining() < dataTypeLength)
					return false;
				byte[] dataTypeBytes = new byte[dataTypeLength];
				buffer.get(dataTypeBytes);
				String dataType = new String(dataTypeBytes, StandardCharsets.UTF_8);

				// Read constraint length
				if (buffer.remaining() < 4)
					return false;
				int constraintLength = buffer.getInt();

				// Validate constraint length
				if (constraintLength < 0 || constraintLength > buffer.remaining())
					return false;

				// Read constraint object
				byte[] constraintBytes = new byte[constraintLength];
				buffer.get(constraintBytes);

				List<Constraint> constraints = new ArrayList<>();
				try (ObjectInputStream objectInputStream = new ObjectInputStream(
						new ByteArrayInputStream(constraintBytes))) {
					Object obj = objectInputStream.readObject();
					if (obj instanceof List<?>) {
						constraints = (List<Constraint>) obj;
					}
				} catch (Exception e) {
					System.err.println("Error deserializing constraints: " + e.getMessage());
				}

				if (constraintToRemove.equals("PK")) {
					for (Constraint cons : constraints) {
						if (cons.getType().equals("PK")) {
							for (String tabName : database.getTables(user.getUsername())) {
								List<Column> columnsCheck = new TableDAO(user, database.getDabaseName(),
										tabName).columnsArray;
								for (Column col : columnsCheck) {
									for (Constraint constr : col.getConstraints()) {
										if (constr.getType().equals("FK")
												&& constr.getReferenceTable().equals(tableName)
												&& constr.getReferenceColumn().equals(columnName)) {
											try {
												Files.deleteIfExists(tempPath);
											} catch (IOException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
											return false;
										}
									}
								}
							}
						}
					}
				}

				// Modify constraints if it's the target column
				if (currentColumn.equals(columnName)) {
					constraints.removeIf(constraint -> constraint.getType().equals(constraintToRemove));
				}

				// Serialize updated constraints
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
				objectStream.writeObject(constraints);
				objectStream.flush();
				byte[] updatedConstraintBytes = byteStream.toByteArray();

				// Write updated data to temp file
				ByteBuffer writeBuffer = ByteBuffer.allocate(
						4 + columnNameBytes.length + 4 + dataTypeBytes.length + 4 + updatedConstraintBytes.length);
				writeBuffer.putInt(columnNameLength);
				writeBuffer.put(columnNameBytes);
				writeBuffer.putInt(dataTypeLength);
				writeBuffer.put(dataTypeBytes);
				writeBuffer.putInt(updatedConstraintBytes.length);
				writeBuffer.put(updatedConstraintBytes);
				writeBuffer.flip();
				writeChannel.write(writeBuffer);
			}

			// Replace old metadata with new one
			Files.move(tempPath, metaPath, StandardCopyOption.REPLACE_EXISTING);

			return true; // ✅ Successfully removed constraint

		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(tempPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

}
