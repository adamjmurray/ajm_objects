package ajm.maxsupport;

import java.util.HashMap;
import java.util.Map;

/**
 * A singleton Map of Strings. Used to indirectly pass long messages in Max.
 * 
 * @version 0.8
 * @author Adam Murray (adam@compusition.com)
 */
public class TextBlock {

	private static Map<String, String> textMap = new HashMap<String, String>();

	private TextBlock() {
	}

	public static void set(String name, String text) {
		textMap.put(name, text);
	}

	public static String get(String name) {
		return textMap.get(name);
	}

	public static void remove(String name) {
		textMap.remove(name);
	}
}
