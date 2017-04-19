package training;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * 将语料中出现的所有汉字编码
 * 
 * @author zena
 *
 */
public class ChineseCharacterEncoding {

	private static BufferedReader br;

	public static HashMap<Character, Integer> getCCE(String corpus) {
		
		HashMap<Character, Integer> ccemap = new HashMap<Character, Integer>();

		try {
			br = new BufferedReader(new FileReader(corpus));
			String line = null;

			int i = 0;
			while ((line = br.readLine()) != null) {
				line = line.replaceAll("[\\pP\\p{Punct}]", "").replaceAll(" +", "");
				// System.out.println(line);
				for (int j = 0; j < line.length(); j++) {
					char c = line.charAt(j);
					if (!ccemap.containsKey(c)) {
						ccemap.put(c, i);
						i++;
					}
				}
			}

			// System.out.println("ccemap length = " + ccemap.size());

			return ccemap;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
