package Controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class UserSocket {
	
	String userDirectory= "/home/naga-zstk392/ZBase/";	
		
	public UserSocket(String userDirectoryName) {
		this.userDirectory+=userDirectoryName;
	}
	
	public   HashMap<String, ArrayList<String>> getFolderStructure() {
        HashMap<String, ArrayList<String>> map = new HashMap<>();

        File parentFolder = new File(userDirectory);
        if (parentFolder.exists() && parentFolder.isDirectory()) {
            File[] folders = parentFolder.listFiles(File::isDirectory);

            if (folders != null) {
                for (File folder : folders) {
                    ArrayList<String> subfoldersList = new ArrayList<>();

                    // Get subfolders inside this folder
                    File[] subfolders = folder.listFiles(File::isDirectory);
                    if (subfolders != null) {
                        for (File subfolder : subfolders) {
                            subfoldersList.add(subfolder.getName()); // Store subfolder name
                        }
                    }
                    map.put(folder.getName(), subfoldersList);
                }
            }
        }

        return map;
    }
	
	
}
