package MyClasses;

import java.util.Map.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class PrettyPrintingMap<K, V> {
    private Map<K, V> map;
    private int tabsNum = 1;

    public PrettyPrintingMap(Map<K, V> map) {
        this.map = map;
    }

    public PrettyPrintingMap(Map<K, V> map, int tabsNum) {
        this.map = map;
        this.tabsNum = tabsNum;
    }

    // for debugging purposes (not currently used)
    public String toString() {

        StringBuilder sb = new StringBuilder();
        Iterator<Entry<K, V>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<K, V> entry = iter.next();
            sb.append(entry.getKey());
            sb.append(": ").append('"');
            String curEntryValueString = entry.getValue().toString();
            if (curEntryValueString.length() > 2 && curEntryValueString.charAt(0) == '{'
                    && curEntryValueString.charAt(curEntryValueString.length() - 1) == '}') { // if it is a map, handle it accordingly
                String[] keyValuePairs = curEntryValueString.substring(1, curEntryValueString.length() - 1).split(","); //split the string to create key-value pairs
                Map<String, String> tempMap = new HashMap<String, String>();
                for (String pair : keyValuePairs) // iterate over the pairs
                {
                    String[] tempEntry = pair.split("="); // split the pairs to get key and value 
                    tempMap.put(tempEntry[0].trim(), tempEntry[1].trim()); // add them to the hashmap and trim whitespaces
                }
                sb.append("\n\t\t").append(new PrettyPrintingMap(tempMap, 2)).append("\n\t\t\"");
            } else { // not a map
                sb.append(entry.getValue());
                sb.append('"');
            }

            if (iter.hasNext() && !(entry.getKey() instanceof String && entry.getValue() instanceof ClassMaps)) {
                sb.append('\n').append('\t');
                if (tabsNum == 2) {
                    sb.append('\t');
                }
            }
        }
        return sb.toString();
    }
}