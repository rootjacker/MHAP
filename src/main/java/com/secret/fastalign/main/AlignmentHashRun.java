package com.secret.fastalign.main;
import jaligner.Alignment;
import jaligner.matrix.Matrix;
import jaligner.matrix.MatrixLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.LogManager;

import com.secret.fastalign.general.FastaData;
import com.secret.fastalign.general.MatchResult;
import com.secret.fastalign.general.Sequence;
import com.secret.fastalign.minhash.MinHashSearch;

public class AlignmentHashRun 
{	

	private static final int DEFAULT_NUM_WORDS = 256;

	private static final int DEFAULT_KMER_SIZE = 14;

	private static final double DEFAULT_DATA_ERROR = 0.15;

	private static final int DEFAULT_SKIP = 10;
	private static final int DEFAULT_THRESHOLD = 1;
	
	public static void main(String[] args) throws Exception {
		String inFile = null;
		int kmerSize = DEFAULT_KMER_SIZE;
		int threshold = DEFAULT_THRESHOLD;
		int numWords = DEFAULT_NUM_WORDS; 
		int maxSkip = DEFAULT_SKIP;

		for (int i = 0; i < args.length; i++) {
			if (args[i].trim().equalsIgnoreCase("-k")) {
				kmerSize = Integer.parseInt(args[++i]);
			} else if (args[i].trim().equalsIgnoreCase("-s")) {
				inFile = args[++i];
			} else if (args[i].trim().equalsIgnoreCase("--num-hashes")) {
				numWords = Integer.parseInt(args[++i]);
			} else if (args[i].trim().equalsIgnoreCase("--threshold")) {
				threshold = Integer.parseInt(args[++i]);
			} else if (args[i].trim().equalsIgnoreCase("--max-skip")) {
				maxSkip = Integer.parseInt(args[++i]);
			}
		}
		if (inFile == null) {
			printUsage("Error: no input fasta file specified");
		}

		System.err.println("Running with input fasta: " + inFile);
		System.err.println("kmer size:\t" + kmerSize);
		System.err.println("threshold:\t" + threshold);
		System.err.println("num hashes\t" + numWords);
		System.err.println("max skip\t" + maxSkip);

		LogManager.getLogManager().reset();
		
		double kmerError = MinHashSearch.probabilityKmerMatches(DEFAULT_DATA_ERROR, kmerSize);
		
		System.out.println("Probability of shared kmer in equal string: "+kmerError);
		
		// read and index the kmers
		long startTime = System.nanoTime();

		FastaData data = new FastaData(inFile, kmerSize);
		
		System.out.println("Read in "+data.size()+" sequences.");

		System.err.println("Time (s) to read: " + (System.nanoTime() - startTime)*1.0e-9);
		
		//SimHashSearch hashSearch = new SimHashSearch(kmerSize, numWords);
		MinHashSearch hashSearch = new MinHashSearch(kmerSize, numWords);

		hashSearch.addData(data);

		System.err.println("Time (s) to hash: " + (System.nanoTime() - startTime)*1.0e-9);

		// now that we have the hash constructed, go through all sequences to recompute their min and score their matches
		startTime = System.nanoTime();

		//find out the scores
		/*
		ArrayList<MatchResult> results = new ArrayList<MatchResult>();
		for (Sequence seq : data.getSequences())
		{
			results.addAll(simHash.findMatches(seq, 0.0));
		}
		*/
		
		//ArrayList<MatchResult> results = hashSearch.findMatches(Double.NEGATIVE_INFINITY);
		ArrayList<MatchResult> results = hashSearch.findMatches(0.07);
		
		System.err.println("Time (s) to score: " + (System.nanoTime() - startTime)*1.0e-9);
		
		//sort to get the best scores on top
		ArrayList<MatchResult> mixedResults = new ArrayList<MatchResult>();
		
		Collections.sort(results);		
		mixedResults.addAll(results.subList(0, Math.min(results.size(), 100)));
		//Collections.shuffle(results);
		mixedResults.addAll(results.subList(Math.max(0,results.size()-50), results.size()));
		
		//Collections.shuffle(mixedResults);

		System.out.println("Found "+results.size()+" matches:");
		
		Matrix matrix = MatrixLoader.load("/Users/kberlin/Dropbox/Projects/fast-align/src/test/resources/com/secret/fastalign/matrix/score_matrix.txt");
		
		//output result
		int count = 0;
		double mean = 0;
		for (MatchResult match : mixedResults)
		{
			//this already computes the reverse compliment
			Sequence s1 = data.getSequence(match.getFromId());
			Sequence s2 = data.getSequence(match.getToId());
			
			//compute the actual match
			double score = computeAlignment(s1, s2, matrix);
			
			//System.out.format("Sequence match (%s - %s) with identity score %f (SW=%f).\n", match.getFromId(), match.getToId(), match.getScore(), score);
			System.out.format("%f %f %s %s %d\n", match.getScore(), score, match.getFromId().toStringInt(), match.getToId().toStringInt(), match.getFromShift());
			
			mean += match.getScore();
			
			count++;
			
			if (count>200)
				break;
		}
		
		mean = mean/count;
		
		System.out.println("Mean: "+mean);		
	}
	
	public static double computeAlignment(Sequence s1, Sequence s2, Matrix matrix)
	{		
		//compute the actual match
		Alignment alignment = jaligner.SmithWatermanGotoh.align(new jaligner.Sequence(s1.getString()), new jaligner.Sequence(s2.getString()), matrix, 5, 3);
		
		//double score = alignment.getScore();
		double score = getScoreWithNoTerminalGaps(alignment);
		
		//score = score/(double)Math.min(s1.length(), s2.length());
		
		return score;
	}
	
	public static float getScoreWithNoTerminalGaps(Alignment alignment)
	{
		char[] sequence1 = alignment.getSequence1();
		char[] sequence2 = alignment.getSequence2();
		char GAP = '-';
		//float extend = alignment.getExtend();
		//float open = alignment.getOpen();
		//Matrix matrix = alignment.getMatrix();

		// The calculated score
		float calcScore = 0;

		// In the previous step there was a gap in the first sequence
		boolean previous1wasGap = false;

		// In the previous step there was a gap in the second sequence
		boolean previous2wasGap = false;

		int start = 0;
		int end = sequence1.length - 1;

		if (sequence1[start] == GAP)
		{
			while (sequence1[start] == GAP)
			{
				start++;
			}
		}
		else if (sequence2[start] == GAP)
		{
			while (sequence2[start] == GAP)
			{
				start++;
			}
		}

		if (sequence1[end] == GAP)
		{
			while (sequence1[end] == GAP)
			{
				end--;
			}
		}
		else if (sequence2[end] == GAP)
		{
			while (sequence2[end] == GAP)
			{
				end--;
			}
		}

		char c1, c2; // the next character
		for (int i = start; i <= end; i++)
		{
			c1 = sequence1[i];
			c2 = sequence2[i];
			// the next character in the first sequence is a gap
			if (c1 == GAP)
			{
				if (previous1wasGap)
				{
					//calcScore -= extend;
					calcScore -= 0;
				}
				else
				{
					//calcScore -= open;
					calcScore -= 0;
				}
				previous1wasGap = true;
				previous2wasGap = false;
			}
			// the next character in the second sequence is a gap
			else if (c2 == GAP)
			{
				if (previous2wasGap)
				{
					//calcScore -= extend;
					calcScore -= 0;
				}
				else
				{
					//calcScore -= open;
					calcScore -= 0;
				}
				previous1wasGap = false;
				previous2wasGap = true;
			}
			// the next characters in boths sequences are not gaps
			else
			{
				//calcScore += matrix.getScore(c1, c2);
				calcScore += 1;
				previous1wasGap = false;
				previous2wasGap = false;
			}
		}
		
		calcScore = calcScore/(float)(end-start+1);
		
		return calcScore;
	}

	public static void printUsage(String error) {
		if (error != null) {
			System.err.println(error);
		}
		System.err.println("Usage buildMulti <-s fasta file>");
		System.err.println("Options: ");
		System.err.println("\t -k [int merSize], default: " + DEFAULT_KMER_SIZE);
		System.err.println("\t  --num-hashes [int # hashes], default: " + DEFAULT_NUM_WORDS);
		System.err.println("\t  --threshold [int threshold for % matching minimums], default: " + DEFAULT_THRESHOLD);
		System.err.println("\t --max-skip [int bp maximum distance to nearest minimum value when guessing overlap positions], default: " + DEFAULT_SKIP);
		System.exit(1);
	}
}
