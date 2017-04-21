package training;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationInteger;
import be.ac.ulg.montefiore.run.jahmm.OpdfInteger;
import be.ac.ulg.montefiore.run.jahmm.OpdfIntegerFactory;
import be.ac.ulg.montefiore.run.jahmm.ViterbiCalculator;

/**
 * 
 * 训练HMM
 * 
 * @author zhaozeqing
 *
 */
public class TextSegmentation {

	private double[][] statusTransferMatrix;
	private double[][] mixMatrix;
	private int statusNo, characterNo;

	private HashMap<Integer, Character> remap;
	private HashMap<Character, Integer> ccemap;

	// 初始状态：M和E不可能出现在句子的首位
	private double[] Pi = { 0.5, 0.0, 0.0, 0.5 };
	// HMM模型
	private Hmm<ObservationInteger> hmm;

	TextSegmentation() {
		MatrixTraining mt = new MatrixTraining();
		this.statusTransferMatrix = mt.getFreqStatus();
		this.mixMatrix = mt.getFreqMix();
		this.statusNo = mixMatrix.length;
		this.characterNo = mixMatrix[0].length;
		this.remap = MatrixTraining.remap;
		this.ccemap = MatrixTraining.ccemap;

		hmm = buildHMM();
	}

	// 训练HMM模型
	private Hmm<ObservationInteger> buildHMM() {
		Hmm<ObservationInteger> hmm = new Hmm<ObservationInteger>(statusNo, new OpdfIntegerFactory(characterNo));
		int i, j;
		for (i = 0; i < statusNo; i++) {
			hmm.setPi(i, Pi[i]);
		}
		for (i = 0; i < statusNo; i++) {
			for (j = 0; j < statusNo; j++) {
				hmm.setAij(i, j, statusTransferMatrix[i][j]);
			}
			hmm.setOpdf(i, new OpdfInteger(mixMatrix[i]));
		}
		return hmm;
	}

	// 对观察字符进行编码
	private List<ObservationInteger> getOseq(String content) {
		List<ObservationInteger> oseq = new ArrayList<ObservationInteger>();
		int length = content.length();
		for (int i = 0; i < length; i++) {
			oseq.add(new ObservationInteger(ccemap.get(content.charAt(i))));
		}
		return oseq;
	}

	// 对文本分词后的文本进行解码
	private String decode(String content, int[] seqrs) {
		StringBuilder sb = new StringBuilder();
		char ch;
		for (int i = 0; i < content.length(); i++) {
			sb.append(content.charAt(i));
			ch = remap.get(seqrs[i]);
			if (ch == 'E' || ch == 'S')
				sb.append("/");
		}
		return sb.toString();
	}

	// 分词
	public String segmentation(String content) {
		List<ObservationInteger> oseq = getOseq(content);
		ViterbiCalculator vc = new ViterbiCalculator(oseq, hmm);
		int[] segrs = vc.stateSequence();
		return decode(content, segrs);
	}
	
	public static void main(String[] args) {
		TextSegmentation ts = new TextSegmentation();
		String s = "今天天气真好啊";
		System.out.println(s + "\n" + ts.segmentation(s));
	}
}
