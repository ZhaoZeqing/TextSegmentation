package training;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;

/**
 * 训练状态转移矩阵
 * 
 * @author zhaozeqing
 *
 */
public class StatusTransMatrix {

	// 已分好词的训练语料，语料格式：“ 一点 外语 知识 、 数理化 知识 也 没有 ， 还 攀 什么 高峰 ”
	private final static String CORPUS = "/Users/zhaozeqing/Documents/msr_training.utf8";

	// 存储 B M E S 到 0 1 2 3 的映射
	private final static HashMap<Character, Integer> map = new HashMap<Character, Integer>();
	// 存储 0 1 2 3 到 B M E S 的映射
	private final static HashMap<Integer, Character> remap = new HashMap<Integer, Character>();
	static {
		map.put('B', 0);
		map.put('M', 1);
		map.put('E', 2);
		map.put('S', 3);
		remap.put(0, 'B');
		remap.put(1, 'M');
		remap.put(2, 'E');
		remap.put(3, 'S');
	}

	// 存储语料中 B M E S 分别出现的次数
	private long countSingle[] = new long[4];
	// 存储语料中 BB BM BE BS MB MM ME MS EB EM EE ES SB SM SE SS 分别出现的次数
	private double countDouble[][] = new double[4][4];

	// 规范化句子
	// 输入：一点 外语 知识 、 数理化 知识 也 没有 ， 还 攀 什么 高峰
	// 输出：BEBEBEBMEBESBESSBEBE
	private String encode(String content) {
		if (content == null || "".equals(content.trim()))
			return null;

		// 去掉标点，将多个空格替换成一个空格
		content = content.replaceAll("[\\pP\\p{Punct}]", " ").replaceAll(" +", " ");

		if (content == null || "".equals(content.trim()))
			return null;

		Stack<String> stack = new Stack<String>();
		int contentLen = content.length();
		int spaceLen = 0;
		int wordLen = 0;
		
		// 如果句子开头没有空格，加一个空格
		if (!Character.isWhitespace(content.charAt(0))) {
			content = " " + content;
			contentLen++;
		}
		// 如果句子结尾没有空格，加一个空格
		if (!Character.isWhitespace(content.charAt(contentLen-1))) {
			content = content + " ";
			contentLen++;
		}
		
		for (int i = 0; i < contentLen; i++) {
			// 结尾
			if (i == contentLen - 1) {
				if (stack.peek().equals("M")) {
					stack.pop();
					stack.push("E");
				}
				if (stack.peek().equals("B") && wordLen == 1) {
					stack.pop();
					stack.push("S");
				}
			}
			// 遇到空格
			else if (Character.isWhitespace(content.charAt(i))) {
				spaceLen++;
			}
			// 开头
			else if (spaceLen == 1 && wordLen == 0) {
				stack.push("B");
				wordLen++;
			}
			// 两个词之间
			else if (spaceLen == 2 && wordLen != 1) {
				stack.pop();
				stack.push("E");
				stack.push("B");
				wordLen = spaceLen = 1;
			}
			// 单字成词
			else if (spaceLen == 2 && wordLen == 1) {
				if (!stack.peek().equals("S")) {
					stack.pop();
				}
				stack.push("S");
				stack.push("B");
				spaceLen = 1;
			}
			// 词中间
			else if (spaceLen == 1) {
				stack.push("M");
				wordLen++;
			}
		}

		StringBuilder sb = new StringBuilder();
		for (String s : stack) {
			sb.append(s);
		}

		return sb.toString();
	}

	// 计算 countSingle[] 和 countDouble[][]
	private void count(String line) {
		int length = line.length();
		int j = 0;
		for (int i = 0; i < length - 1; i++) {
			countSingle[map.get(line.charAt(i))]++;
			j = i + 1;
			countDouble[map.get(line.charAt(i))][map.get(line.charAt(j))]++;
		}
		countSingle[map.get(line.charAt(length - 1))]++;
	}

	// 规范化语料，统计并返回状态转移矩阵
	private double[][] getTransProbMatrix(FileReader fr) {
		try {
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringBuilder sb = new StringBuilder();

			while ((line = br.readLine()) != null) {
				System.out.println(line);
				// 规范化句子
				line = encode(line);
				System.out.println(line);
				if (line == null)
					continue;
				// 统计频率
				count(line);
				sb.append(line);
			}

			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					// Prob(Cj | Ci) = Prob(Ci, Cj) / Prob(Ci) =
					// countDouble[i][j] / countSingle[i]
					countDouble[i][j] = countDouble[i][j] / countSingle[i];
				}
			}

			return countDouble;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	// 打印出状态转移矩阵
	private static void print(double[][] A) {
		int i, j;
		char[] chs = { 'B', 'M', 'E', 'S' };
		System.out.println("\t\t" + "B" + "\t\t\t" + "M" + "\t\t\t" + "E" + "\t\t\t" + "S");

		for (i = 0; i < 4; i++) {
			System.out.print(chs[i] + "\t");
			for (j = 0; j < 4; j++) {
				System.out.format("%.12f\t\t", A[i][j]);
			}
			System.out.println();
		}
	}

	public static void main(String[] args) {
		try {
			double[][] transProbMatrix = new StatusTransMatrix().getTransProbMatrix(new FileReader(CORPUS));
			print(transProbMatrix);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
