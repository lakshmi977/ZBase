package Model;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HashMapSorting {
	public static <K, V> Map<K, V> sortByValue(Map<K, V> records, boolean ascending) {
	    List<Map.Entry<K, V>> list = new ArrayList<>(records.entrySet());

	    list.sort((e1, e2) -> {
	        Comparable<V> value1 = (Comparable<V>) e1.getValue();
	        Comparable<V> value2 = (Comparable<V>) e2.getValue();
	        return ascending ? value1.compareTo((V) value2) : value2.compareTo((V) value1);
	    });

	    Map<K, V> sortedMap = new LinkedHashMap<>();
	    for (Map.Entry<K, V> entry : list) {
	        sortedMap.put(entry.getKey(), entry.getValue());
	    }
	    return sortedMap;
	}

}