package training;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Stack;

/**
 * 训练状态转移矩阵
 * 
 * @author zena
 *
 */
public class TransProbMatrix {

	// 训练语料（已分好词）
	// 语料格式：“一点 外语 知识 、 数理化 知识 也 没有 ， 还 攀 什么 高峰”
	private final static String CORPUS = "msr_training.utf8";

	// 规范化句子
	// 输入：一点 外语 知识 、 数理化 知识 也 没有 ， 还 攀 什么 高峰
	// 输出：BEBEBEBMEBESBESSBEBE
	private String encode(String content) {
		if (content == null || "".equals(content.trim()))
			return null;

		// 去掉标点，将多个空格替换成一个空格
		content = content.replaceAll("[\\pP\\p{Punct}]", " ").replaceAll(" +", " ");

		Stack<String> stack = new Stack<String>();
		int contentLen = content.length();
		int spaceLen = 0;
		int wordLen = 0;
		for (int i = 0; i < contentLen; i++) {
			// 结尾
			if (i == contentLen-1) {
				stack.pop();
				stack.push("E");
			}
			//遇到空格
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

	//规范化语料
	private String normCorpus(FileReader fr) {
		try {
			BufferedReader br = new BufferedReader(fr);
			String s = null;
			StringBuilder sb = new StringBuilder();
			
			while ((s = br.readLine()) != null) {
				sb.append(encode(s));
			}
			
			return sb.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		try {
			new TransProbMatrix().normCorpus(new FileReader(CORPUS));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
