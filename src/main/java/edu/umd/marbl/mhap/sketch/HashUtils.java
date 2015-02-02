package edu.umd.marbl.mhap.sketch;

import java.nio.ByteBuffer;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class HashUtils
{
	private HashUtils()
	{
	}

	public static long[] computeHashes(String item, int numWords, int seed)
	{
		long[] hashes = new long[numWords];
	
		for (int word = 0; word < numWords; word += 2)
		{
			HashFunction hashFunc = Hashing.murmur3_128(seed + word);
			Hasher hasher = hashFunc.newHasher();
			hasher.putUnencodedChars(item);
	
			// get the two longs out
			HashCode hc = hasher.hash();
			ByteBuffer bb = ByteBuffer.wrap(hc.asBytes());
			hashes[word] = bb.getLong(0);
			if (word + 1 < numWords)
				hashes[word + 1] = bb.getLong(8);
		}
	
		return hashes;
	}

	public final static int[] computeHashesInt(Object obj, int numWords, int seed)
	{
		if (obj instanceof Integer)
			return computeHashesIntInt((Integer) obj, numWords, seed);
		if (obj instanceof Long)
			return computeHashesIntLong((Long) obj, numWords, seed);
		if (obj instanceof Double)
			return computeHashesIntDouble((Double) obj, numWords, seed);
		if (obj instanceof Float)
			return computeHashesIntFloat((Float) obj, numWords, seed);
		if (obj instanceof String)
			return computeHashesIntString((String) obj, numWords, seed);
	
		throw new SketchRuntimeException("Cannot hash class type " + obj.getClass().getCanonicalName());
	}

	public final static int[] computeHashesIntDouble(double obj, int numWords, int seed)
	{
		int[] hashes = new int[numWords];
	
		HashFunction hf = Hashing.murmur3_32(seed);
	
		for (int iter = 0; iter < numWords; iter++)
		{
			HashCode hc = hf.newHasher().putDouble(obj).putInt(iter).hash();
	
			hashes[iter] = hc.asInt();
		}
	
		return hashes;
	}

	public final static int[] computeHashesIntFloat(float obj, int numWords, int seed)
	{
		int[] hashes = new int[numWords];
	
		HashFunction hf = Hashing.murmur3_32(seed);
	
		for (int iter = 0; iter < numWords; iter++)
		{
			HashCode hc = hf.newHasher().putFloat(obj).putInt(iter).hash();
	
			hashes[iter] = hc.asInt();
		}
	
		return hashes;
	}

	public final static int[] computeHashesIntInt(int obj, int numWords, int seed)
	{
		int[] hashes = new int[numWords];
	
		HashFunction hf = Hashing.murmur3_32(seed);
	
		for (int iter = 0; iter < numWords; iter++)
		{
			HashCode hc = hf.newHasher().putInt(obj).putInt(iter).hash();
	
			hashes[iter] = hc.asInt();
		}
	
		return hashes;
	}

	public final static int[] computeHashesIntLong(long obj, int numWords, int seed)
	{
		int[] hashes = new int[numWords];
	
		HashFunction hf = Hashing.murmur3_32(seed);
	
		for (int iter = 0; iter < numWords; iter++)
		{
			HashCode hc = hf.newHasher().putLong(obj).putInt(iter).hash();
	
			hashes[iter] = hc.asInt();
		}
	
		return hashes;
	}

	public final static int[] computeHashesIntString(String obj, int numWords, int seed)
	{
		int[] hashes = new int[numWords];
	
		HashFunction hf = Hashing.murmur3_32(seed);
	
		for (int iter = 0; iter < numWords; iter++)
		{
			HashCode hc = hf.newHasher().putUnencodedChars(obj).putInt(iter).hash();
	
			hashes[iter] = hc.asInt();
		}
	
		return hashes;
	}

	public final static long[][] computeNGramHashes(final String seq, final int kmerSize, final int numWords, final int seed)
	{
		final int numberKmers = seq.length()-kmerSize+1;
	
		if (numberKmers < 1)
			throw new SketchRuntimeException("Kmer size bigger than string length.");
	
		// get the rabin hashes
		final long[] rabinHashes = computeSequenceHashesLong(seq, kmerSize, seed);
	
		final long[][] hashes = new long[rabinHashes.length][numWords];
	
		// Random rand = new Random(0);
		for (int iter = 0; iter < rabinHashes.length; iter++)
		{
			// rand.setSeed(rabinHashes[iter]);
			long x = rabinHashes[iter];
	
			for (int word = 0; word < numWords; word++)
			{
				// hashes[iter][word] = rand.nextLong();
	
				// XORShift Random Number Generators
				x ^= (x << 21);
				x ^= (x >>> 35);
				x ^= (x << 4);
				hashes[iter][word] = x;
			}
		}
		
		return hashes;
	}

	public final static long[][] computeNGramHashesExact(final String seq, final int kmerSize, final int numWords, final int seed)
	{
		HashFunction hf = Hashing.murmur3_128(seed);
	
		long[][] hashes = new long[seq.length() - kmerSize + 1][numWords];
		for (int iter = 0; iter < hashes.length; iter++)
		{
			String subStr = seq.substring(iter, iter + kmerSize);
			
			for (int word=0; word<numWords; word++)
			{
				HashCode hc = hf.newHasher().putUnencodedChars(subStr).putInt(word).hash();
				hashes[iter][word] = hc.asLong();
			}
		}
		
		return hashes;
	}

	public final static int[] computeSequenceHashes(final String seq, final int kmerSize)
	{
		// RollingSequenceHash rabinHash = new RollingSequenceHash(kmerSize);
		// final int[] rabinHashes = rabinHash.hashInt(seq);
	
		HashFunction hf = Hashing.murmur3_32(0);
	
		int[] hashes = new int[seq.length() - kmerSize + 1];
		for (int iter = 0; iter < hashes.length; iter++)
		{
			HashCode hc = hf.newHasher().putUnencodedChars(seq.substring(iter, iter + kmerSize)).hash();
			hashes[iter] = hc.asInt();
		}
	
		return hashes;
	}

	public final static long[] computeSequenceHashesLong(final String seq, final int kmerSize, final int seed)
	{
		HashFunction hf = Hashing.murmur3_128(seed);
	
		long[] hashes = new long[seq.length() - kmerSize + 1];
		for (int iter = 0; iter < hashes.length; iter++)
		{
			HashCode hc = hf.newHasher().putUnencodedChars(seq.substring(iter, iter + kmerSize)).hash();
			hashes[iter] = hc.asLong();
		}
	
		return hashes;
	}

}
