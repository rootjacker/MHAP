package com.secret.fastalign.simhash;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import org.apache.lucene.util.OpenBitSet;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.secret.fastalign.data.Sequence;
import com.secret.fastalign.utils.FastAlignRuntimeException;
import com.secret.fastalign.utils.RabinKarpSeqHash;

public final class SequenceSimHash extends AbstractSequenceBitHash 
{
	private final static int NUM_LONG_BITS = 64;
	
	public static long[][] computeHash(Sequence seq, int kmerSize, int numWords)
	{
		String seqString = seq.getString();
		int numberKmers = seqString.length()-kmerSize+1;
		
		if (numWords%2!=0)
			throw new FastAlignRuntimeException("Number of words must be a multiple of 2.");

		long[][] hashes = new long[numberKmers][numWords];

		HashFunction hf = Hashing.murmur3_128(0);
		RabinKarpSeqHash rabinHash = new RabinKarpSeqHash(kmerSize);

		int[] rabinHashes = rabinHash.hashInt(seqString);
		
		for (int iter=0; iter<rabinHashes.length; iter++)
		{
			for (int word128=0; word128<numWords/2; word128++)
			{
				Hasher hasher = hf.newHasher(0);
				HashCode code = hasher.putInt(rabinHashes[iter]).putInt(word128).hash();

				//store the code
				LongBuffer bb = ByteBuffer.wrap(code.asBytes()).asLongBuffer();
				hashes[iter][word128*2+0] = bb.get(0);
				hashes[iter][word128*2+1] = bb.get(1);
			}
		}
		
		return hashes;
	}
	
	public SequenceSimHash(Sequence seq, int kmerSize, int numberWords)
	{
		super(seq, kmerSize);
		
		int subKmerSize = kmerSize;
		
		//compute the hashes
		long[][] hashes = computeHash(seq, kmerSize, numberWords);
		
		recordHashes(hashes, kmerSize, numberWords, subKmerSize);
	}
	
	private void recordHashes(long[][] hashes, int kmerSize, int numWords, int subKmerSize)
	{
		this.bits = new OpenBitSet(NUM_LONG_BITS*numWords);
		int[] counts = new int[this.bits.length()];

		int numerSubKmers = kmerSize-subKmerSize+1;

		//perform count for each kmer
		for (int kmerIndex=0; kmerIndex<hashes.length; kmerIndex++)
		{
			OpenBitSet val = new OpenBitSet(hashes[kmerIndex], hashes[kmerIndex].length);
			
			//for each hash go through all its bits and count
			for (int wordsBitIndex=0; wordsBitIndex<counts.length; wordsBitIndex++)
			{
				for (int subIndex=0; subIndex<numerSubKmers; subIndex++)
				{
					if (val.fastGet(wordsBitIndex*numerSubKmers+subIndex))
						counts[wordsBitIndex]++;
					else
						counts[wordsBitIndex]--;
				}
			}
		}
		
		for (int bitIndex=0; bitIndex<counts.length; bitIndex++)
		{
			if (counts[bitIndex]>0)
				this.bits.fastSet(bitIndex);
		}		
	}
}
