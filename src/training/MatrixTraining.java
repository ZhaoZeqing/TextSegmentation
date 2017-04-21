package training;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;

/**
 * 训练状态转移矩阵和混淆矩阵
 * 
 * @author zhaozeqing
 *
 */
public class MatrixTraining {

	// 已分好词的训练语料，语料格式：“ 一点 外语 知识 、 数理化 知识 也 没有 ， 还 攀 什么 高峰 ”
	private final static String CORPUS = "/Users/zhaozeqing/Documents/msr_training.utf8";

	// 存储 B M E S 到 0 1 2 3 的映射
	final static HashMap<Character, Integer> map = new HashMap<Character, Integer>();
	// 存储 0 1 2 3 到 B M E S 的映射
	final static HashMap<Integer, Character> remap = new HashMap<Integer, Character>();
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

	// 获取汉字编码
	final static HashMap<Character, Integer> ccemap = ChineseCharacterEncoding.getCCE(CORPUS);
	private final static int ccesize = ccemap.size();

	// 存储语料中 B M E S 分别出现的次数
	private long countStatus[] = new long[4];
	// 存储语料中 BB BM BE BS MB MM ME MS EB EM EE ES SB SM SE SS 分别出现的频率
	private double freqStatus[][] = new double[4][4];

	// 存储语料中每个汉字和B M E S分别同时出现的频率
	private double freqMix[][] = new double[4][ccesize];

	// 构造函数，new的时候开始训练
	MatrixTraining() {
		try {
			readFile(new FileReader(CORPUS));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public double[][] getFreqStatus() {
		return freqStatus;
	}

	public double[][] getFreqMix() {
		return freqMix;
	}

	// 规范化句子
	// 输入：一点 外语 知识 、 数理化 知识 也 没有 ， 还 攀 什么 高峰
	// 输出：["BEBEBEBMEBESBESSBEBE", "一B点E外B语E知B识E数B理M化E知B识E也S没B有E还S攀S什B么E高B峰E"]
	private String[] encode(String content) {
		// 存放返回结果
		String[] result = new String[2];

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
		if (!Character.isWhitespace(content.charAt(contentLen - 1))) {
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

		// 生成状态序列
		StringBuilder statusStr = new StringBuilder();
		for (String s : stack) {
			statusStr.append(s);
		}
		result[0] = statusStr.toString();

		// 生成状态汉字混合序列
		StringBuilder mixStr = new StringBuilder();
		content = content.replaceAll(" +", "");
		for (int i = 0; i < content.length(); i++) {
			mixStr.append(content.charAt(i));
			mixStr.append(statusStr.charAt(i));
		}
		result[1] = mixStr.toString();

		return result;
	}

	// 计算 countStatus[] 和 freqStatus[][]
	private void countStatus(String line) {
		int length = line.length();
		int j = 0;
		for (int i = 0; i < length - 1; i++) {
			countStatus[map.get(line.charAt(i))]++;
			j = i + 1;
			freqStatus[map.get(line.charAt(i))][map.get(line.charAt(j))]++;
		}
		countStatus[map.get(line.charAt(length - 1))]++;
	}

	// 计算状态转移矩阵
	private void calStatusTransferMatrix() {
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				// Prob(Cj | Ci) = Prob(Ci, Cj) / Prob(Ci) =
				// freqStatus[i][j] / countStatus[i]
				freqStatus[i][j] = freqStatus[i][j] / countStatus[i];
			}
		}
	}

	// 计算 freqMix[][]
	private void countMix(String line) {
		int length = line.length();
		if (length % 2 != 0)
			return;

		int j;
		Integer characterNo, statusNo;
		for (int i = 0; i < length - 1; i++) {
			j = i + 1;
			characterNo = ccemap.get(line.charAt(i));
			statusNo = map.get(line.charAt(j));
			if (characterNo == null && statusNo == null) {
				continue;
			}
			freqMix[statusNo][characterNo]++;
		}
	}

	// 计算混淆矩阵
	private void calMixMatrix() {

		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < ccesize; j++) {
				freqMix[i][j] = freqMix[i][j] / countStatus[i];
			}
		}
	}

	private void readFile(FileReader fr) {
		try {
			BufferedReader br = new BufferedReader(fr);
			String line = null, statusStr = null, mixStr = null;

			while ((line = br.readLine()) != null) {
				// System.out.println(line);
				String[] encodeResult = encode(line);
				if (encodeResult == null)
					continue;

				// 规范化句子
				statusStr = encodeResult[0];
				// System.out.println(statusStr);
				mixStr = encodeResult[1];
				// System.out.println(mixStr);

				// 统计状态频率
				countStatus(statusStr);
				// 统计混淆频率
				countMix(mixStr);
			}

			// 计算状态转移矩阵
			calStatusTransferMatrix();
			// 计算混淆矩阵
			calMixMatrix();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 打印矩阵
	private void printMatrix(double[][] A) {
		int i, j;
		char[] chs = { 'B', 'M', 'E', 'S' };

		int row = A.length;
		int column = A[0].length;

		if (column == 4) {
			System.out.println("\t\t" + "B" + "\t\t\t" + "M" + "\t\t\t" + "E" + "\t\t\t" + "S");
		} else {
			column = 4; // 只打印前4个汉字
		}

		for (i = 0; i < row; i++) {
			System.out.print(chs[i] + "\t");
			for (j = 0; j < column; j++) {
				System.out.format("%.12f\t\t", A[i][j]);
			}
			System.out.println();
		}
	}

	public static void main(String[] args) {

		MatrixTraining mt = new MatrixTraining();
		System.out.println("状态转移矩阵矩阵：");
		mt.printMatrix(mt.freqStatus);
		System.out.println("混淆矩阵：");
		mt.printMatrix(mt.freqMix);
	}
}
