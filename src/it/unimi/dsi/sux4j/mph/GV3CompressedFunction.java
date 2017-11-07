package it.unimi.dsi.sux4j.mph;

/*
 * Sux4J: Succinct data structures for Java
 *
 * Copyright (C) 2017 Sebastiano Vigna
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */


import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import com.martiansoftware.jsap.stringparsers.ForNameStringParser;

import it.unimi.dsi.Util;
import it.unimi.dsi.big.io.FileLinesByteArrayCollection;
import it.unimi.dsi.bits.BitVector;
import it.unimi.dsi.bits.BitVectors;
import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.bits.LongArrayBitVector;
import it.unimi.dsi.bits.TransformationStrategies;
import it.unimi.dsi.bits.TransformationStrategy;
import it.unimi.dsi.fastutil.Size64;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongBigList;
import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.AbstractObject2LongFunction;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.FileLinesCollection;
import it.unimi.dsi.io.LineIterator;
import it.unimi.dsi.io.OfflineIterable;
import it.unimi.dsi.io.OfflineIterable.OfflineIterator;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.sux4j.io.ChunkedHashStore;
import it.unimi.dsi.sux4j.mph.solve.Linear3SystemSolver;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

/*
 * Sux4J: Succinct data structures for Java
 *
 * Copyright (C) 2016 Sebastiano Vigna
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */

public class GV3CompressedFunction<T> extends AbstractObject2LongFunction<T> implements Serializable, Size64 {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(GV3CompressedFunction.class);
	private static final boolean DEBUG = false;
	protected static final int SEED_BITS = 10;
	protected static final int OFFSET_BITS = Long.SIZE - SEED_BITS;

	/**
	 * The local seed is generated using this step, so to be easily embeddable
	 * in {@link #offsetSeed}.
	 */
	private static final long SEED_STEP = 1L << Long.SIZE - SEED_BITS;
	/**
	 * The lowest 48 bits of {@link #offsetSeed} contain the number of
	 * keys stored up to the given chunk.
	 */
	private static final long OFFSET_MASK = -1L >>> SEED_BITS;
	private static final long SEED_MASK = -1L << Long.SIZE - SEED_BITS;
	//private static final long[] INVERSE = new long[] { 0, 4294967296L, 2147483648L, 1431655766, 1073741824, 858993460, 715827886, 613566760, 536870912, 477218592, 429496735, 390451576, 357913945, 330382108, 306783382, 286331154, 268435456, 252645136, 238609298, 226050916, 214748380, 204522256, 195225790, 186737720, 178956986, 171798712, 165191071, 159072884, 153391693, 148102336, 143165592, 138547336, 134217728, 130150528, 126322585, 122713362, 119304651, 116080204, 113025461, 110127388, 107374198, 104755336, 102261130, 99882976, 97612897, 95443748, 93368866, 91382324, 89478501, 87652432, 85899391, 84215046, 82595572, 81037160, 79536453, 78090340, 76695876, 75350328, 74051176, 72796106, 71582804, 70409356, 69273670, 68174088 };

	public static class Builder<T> {
		protected Iterable<? extends T> keys;
		protected TransformationStrategy<? super T> transform;
		protected File tempDir;
		protected ChunkedHashStore<T> chunkedHashStore;
		protected LongIterable values;
		// TODO this is unused
		protected boolean indirect;
		/** Whether {@link #build()} has already been called. */
		protected boolean built;
		protected Codec codec;

		/**
		 * Specifies the keys of the function; if you have specified a
		 * {@link #store(ChunkedHashStore) ChunkedHashStore}, it can be
		 * {@code null}.
		 *
		 * @param keys
		 *            the keys of the function.
		 * @return this builder.
		 */
		public Builder<T> keys(final Iterable<? extends T> keys) {
			this.keys = keys;
			return this;
		}

		/**
		 * Specifies the transformation strategy for the
		 * {@linkplain #keys(Iterable) keys of the function}; the strategy can
		 * be {@linkplain TransformationStrategies raw}.
		 *
		 * @param transform
		 *            a transformation strategy for the
		 *            {@linkplain #keys(Iterable) keys of the function}.
		 * @return this builder.
		 */
		public Builder<T> transform(final TransformationStrategy<? super T> transform) {
			this.transform = transform;
			return this;
		}

		/**
		 * Specifies a temporary directory for the
		 * {@link #store(ChunkedHashStore) ChunkedHashStore}.
		 *
		 * @param tempDir
		 *            a temporary directory for the
		 *            {@link #store(ChunkedHashStore) ChunkedHashStore} files,
		 *            or {@code null} for the standard temporary directory.
		 * @return this builder.
		 */
		public Builder<T> tempDir(final File tempDir) {
			this.tempDir = tempDir;
			return this;
		}

		/**
		 * Specifies a chunked hash store containing the keys.
		 *
		 * <p>
		 * Note that if you specify a store, it is your responsibility that it
		 * conforms to the rest of the data: it must contain ranks if you do not
		 * specify {@linkplain #values(LongIterable) values} or if you use
		 * the {@linkplain #indirect() indirect} feature, values otherwise.
		 *
		 * @param chunkedHashStore
		 *            a chunked hash store containing the keys associated with their
		 *            values and counting value frequencies, or {@code null}; the
		 *            store can be unchecked, but in this case you must
		 *            specify {@linkplain #keys(Iterable) keys} and a
		 *            {@linkplain #transform(TransformationStrategy) transform}
		 *            (otherwise, in case of a hash collision in the store an
		 *            {@link IllegalStateException} will be thrown).
		 * @return this builder.
		 */
		public Builder<T> store(final ChunkedHashStore<T> chunkedHashStore) {
			this.chunkedHashStore = chunkedHashStore;
			return this;
		}

		/**
		 * Specifies the values assigned to the {@linkplain #keys(Iterable)
		 * keys}; the output width of the function will be the minimum width
		 * needed to represent all values.
		 *
		 * @param values
		 *            values to be assigned to each element, in the same order
		 *            of the {@linkplain #keys(Iterable) keys}.
		 * @return this builder.
		 */
		public Builder<T> values(final LongIterable values) {
			this.values = values;
			return this;
		}

		/**
		 * Specifies that the function construction must be indirect: a provided
		 * {@linkplain #store(ChunkedHashStore) store} contains indices that
		 * must be used to access the {@linkplain #values(LongIterable)
		 * values}.
		 *
		 * <p>
		 * If you specify this option, the provided values <strong>must</strong>
		 * be a {@link LongList} or a {@link LongBigList}.
		 *
		 * @return this builder.
		 */
		public Builder<T> indirect() {
			this.indirect = true;
			return this;
		}

		public Builder<T> codec(Codec codec) {
			this.codec = codec;
			return this;
		}

		/**
		 * Builds a new function.
		 *
		 * @return a {@link GOV3Function} instance with the specified
		 *         parameters.
		 * @throws IllegalStateException
		 *             if called more than once.
		 */
		public GV3CompressedFunction<T> build() throws IOException {
			if (built) throw new IllegalStateException("This builder has been already used");
			built = true;
			if (transform == null) {
				if (chunkedHashStore != null) transform = chunkedHashStore.transform();
				else throw new IllegalArgumentException("You must specify a TransformationStrategy, either explicitly or via a given ChunkedHashStore");
			}
			return new GV3CompressedFunction<>(keys, transform, values, indirect, tempDir, chunkedHashStore, codec);
		}
	}

	public final static double DELTA = 1.10;
	public final static int DELTA_TIMES_256 = (int) Math.floor(DELTA * 256);
	/** The logarithm of the desired chunk size. */
	public final static int LOG2_CHUNK_SIZE = 10;
	/** The shift for chunks. */
	private final int chunkShift;
	/** The number of keys. */
	protected final long n;
	/** Length of longest codeword **/
	protected final int globalMaxCodewordLength;
	/** The seed used to generate the initial hash triple. */
	protected long globalSeed;
	/**
	 * A long containing three values per chunk:
	 * <ul>
	 * <li>the top {@link #SEED_BITS} bits contain the seed (note that it must
	 * not be shifted right);
	 * <li>the remaining lower bits contain the starting position in
	 * {@link #data} of the bits associated with the chunk.
	 * </ul>
	 */
	protected final long[] offsetSeed;

	protected final LongArrayBitVector data;
	/**
	 * The transformation strategy to turn objects of type <code>T</code> into
	 * bit vectors.
	 */
	protected final TransformationStrategy<? super T> transform;
	protected final Codec.Decoder decoder;

	/**
	 * Creates a new function for the given keys and values.
	 *
	 * @param keys
	 *            the keys in the domain of the function, or {@code null}.
	 * @param transform
	 *            a transformation strategy for the keys.
	 * @param values
	 *            values to be assigned to each element, in the same order of
	 *            the iterator returned by <code>keys</code>; if {@code null},
	 *            the assigned value will the the ordinal number of each
	 *            element.
	 * @param indirect
	 *            if true, <code>chunkedHashStore</code> contains ordinal
	 *            positions, and <code>values</code> is a {@link LongIterable}
	 *            that must be accessed to retrieve the actual values.
	 * @param tempDir
	 *            a temporary directory for the store files, or {@code null} for
	 *            the standard temporary directory.
	 * @param chunkedHashStore
	 *            a chunked hash store containing the keys associated with their
	 *            values and counting value frequencies, or {@code null}; the
	 *            store can be unchecked, but in this case <code>keys</code> and <code>transform</code> must be
	 *            non-{@code null}.
	 */
	@SuppressWarnings("resource")
	protected GV3CompressedFunction(final Iterable<? extends T> keys, final TransformationStrategy<? super T> transform, final LongIterable values, final boolean indirect, final File tempDir, ChunkedHashStore<T> chunkedHashStore, final Codec codec) throws IOException {
		Objects.requireNonNull(codec, "Null codec");
		this.transform = transform;
		final ProgressLogger pl = new ProgressLogger(LOGGER);
		pl.displayLocalSpeed = true;
		pl.displayFreeMemory = true;
		final XoRoShiRo128PlusRandom r = new XoRoShiRo128PlusRandom();
		pl.itemsName = "keys";
		final boolean givenChunkedHashStore = chunkedHashStore != null;
		if (!givenChunkedHashStore) {
			if (keys == null) throw new IllegalArgumentException("If you do not provide a chunked hash store, you must provide the keys");
			chunkedHashStore = new ChunkedHashStore<>(transform, tempDir, -1, pl);
			chunkedHashStore.reset(r.nextLong());
			if (values == null || indirect) chunkedHashStore.addAll(keys.iterator());
			else chunkedHashStore.addAll(keys.iterator(), values.iterator());
		}
		n = chunkedHashStore.size();
		defRetValue = -1;
		if (n == 0) {
			this.globalSeed = chunkShift = globalMaxCodewordLength = 0;
			data = null;
			offsetSeed = null;
			decoder = null;
			if (!givenChunkedHashStore) chunkedHashStore.close();
			return;
		}
		final Long2LongOpenHashMap frequencies = chunkedHashStore.value2FrequencyMap();
		final Codec.Coder coder = codec.getCoder(frequencies);
		globalMaxCodewordLength = coder.maxCodewordLength();
		decoder = coder.getDecoder();

		final int log2NumChunks = Math.max(0, Fast.mostSignificantBit(n >> LOG2_CHUNK_SIZE));
		chunkShift = chunkedHashStore.log2Chunks(log2NumChunks);
		final int numChunks = 1 << log2NumChunks;
		LOGGER.debug("Number of chunks: " + numChunks);
		offsetSeed = new long[numChunks + 1];
		int unsolvable = 0;
		final int[] failedcodeword = new int[64];
		final int[] totalcodeword = new int[64];
		final OfflineIterable<BitVector, LongArrayBitVector> offlineData = new OfflineIterable<>(BitVectors.OFFLINE_SERIALIZER, LongArrayBitVector.getInstance());
		int duplicates = 0;
		for (;;) {
			pl.expectedUpdates = numChunks;
			pl.itemsName = "chunks";
			pl.start("Analysing chunks... ");
			try {
				int q = 0;
				final LongArrayBitVector dataBitVector = LongArrayBitVector.getInstance();
				long peeledSumUnsolvable = 0;
				long totalNodesUnsolvable = 0;
				long totalNodesSolvable = 0;
				long peeledSumSolved = 0;
				for (final ChunkedHashStore.Chunk chunk : chunkedHashStore) {
					long seed = 0;
					Linear3SystemSolver solver = null;

					int sumOfLengths = 0;
					int longestInChunkCodeword = 0;
					for(final long[] t : chunk) {
						final int l = coder.codewordLength(t[3]);
						sumOfLengths += l;
						longestInChunkCodeword = Math.max(longestInChunkCodeword, l);
					}

					final int numEquations = sumOfLengths;
					final int numVariables = (numEquations * DELTA_TIMES_256 >>> 8) + globalMaxCodewordLength;
					for (;;) {
						totalcodeword[longestInChunkCodeword]++;
						// We add the length of the longest keyword to avoid wrapping up indices
						solver = new Linear3SystemSolver(numVariables, numEquations);
						final boolean solved = solver.generateAndSolve(chunk, seed, coder, numVariables - globalMaxCodewordLength, globalMaxCodewordLength, DELTA >= 1.23);

						if (solved) {
							offsetSeed[q + 1] = offsetSeed[q] + numVariables;
							peeledSumSolved += solver.numPeeled;
							totalNodesSolvable += numVariables;
							break;
						}
						failedcodeword[longestInChunkCodeword]++;
						peeledSumUnsolvable += solver.numPeeled;
						totalNodesUnsolvable += numVariables;
						unsolvable += solver.unsolvable;
						seed += SEED_STEP;
						if (seed == 0) throw new AssertionError("Exhausted local seeds");
					}
					assert numVariables == (offsetSeed[q + 1] & OFFSET_MASK) - (offsetSeed[q] & OFFSET_MASK);
					this.offsetSeed[q] |= seed;
					dataBitVector.fill(false);
					dataBitVector.length(numVariables);
					q++;
					/* We assign values. */
					final long[] solution = solver.solution;
					for (int i = 0; i < solution.length; i++) dataBitVector.set(i, (int)solution[i]);
					offlineData.add(dataBitVector);
					pl.update();
				}
				LOGGER.info("Unsolvable systems: " + unsolvable + "/" + numChunks + " (" + Util.format(100.0 * unsolvable / numChunks) + "%)");
				LOGGER.info("Mean node peeled for solved systems: " + Util.format((double) peeledSumSolved / totalNodesSolvable * 100) + "%");

				if (unsolvable == 0) {
					LOGGER.info("Mean node peeled for unsolved systems: " + 0 + "%");
				} else {
					LOGGER.info("Mean node peeled for unsolved systems: " + Util.format((double) peeledSumUnsolvable / totalNodesUnsolvable * 100) + "%");

				}
				pl.done();
				break;
			} catch (final ChunkedHashStore.DuplicateException e) {
				if (keys == null) throw new IllegalStateException("You provided no keys, but the chunked hash store was not checked");
				if (duplicates++ > 3) throw new IllegalArgumentException("The input list contains duplicates");
				LOGGER.warn("Found duplicate. Recomputing triples...");
				chunkedHashStore.reset(r.nextLong());
				pl.itemsName = "keys";
				if (values == null || indirect) chunkedHashStore.addAll(keys.iterator());
				else chunkedHashStore.addAll(keys.iterator(), values.iterator());
			}
		}

		if (DEBUG) {
			System.out.println("MaxCodeword: " + globalMaxCodewordLength);
			System.out.println("Offsets: " + Arrays.toString(offsetSeed));
		}
		globalSeed = chunkedHashStore.seed();
		final LongArrayBitVector dataBitVector = LongArrayBitVector.getInstance();
		this.data = dataBitVector;
		final OfflineIterator<BitVector, LongArrayBitVector> iterator = offlineData.iterator();
		while (iterator.hasNext())
			dataBitVector.append(iterator.next());
		iterator.close();
		offlineData.close();
		LOGGER.info("Completed.");

		LOGGER.info("Actual bit cost per element: " + (double) numBits() / n);
		if (!givenChunkedHashStore) chunkedHashStore.close();
	}

	@Override
	@SuppressWarnings("unchecked")
	public long getLong(final Object o) {
		if (n == 0) return defRetValue;
		final int[] e = new int[3];
		final long[] h = new long[3];
		Hashes.spooky4(transform.toBitVector((T) o), globalSeed, h);
		final int chunk = chunkShift == Long.SIZE ? 0 : (int) (h[0] >>> chunkShift);
		final long olc = offsetSeed[chunk];
		final long chunkOffset = olc & OFFSET_MASK;
		final long nextChunkOffset = offsetSeed[chunk + 1] & OFFSET_MASK;
		final long chunkSeed = olc & SEED_MASK;
		final int w = globalMaxCodewordLength;
		Linear3SystemSolver.tripleToEquation(h, chunkSeed, (int)(nextChunkOffset - chunkOffset - w), e);
		if (e[0] == -1) return defRetValue;
		final long e0 = e[0] + chunkOffset, e1 = e[1] + chunkOffset, e2 = e[2] + chunkOffset;
		return decoder.decode(data.getLong(e0, e0 + w) ^ data.getLong(e1, e1 + w) ^ data.getLong(e2, e2 + w));
	}

	/**
	 * Returns the number of keys in the function domain.
	 *
	 * @return the number of the keys in the function domain.
	 */
	@Override
	public long size64() {
		return n;
	}

	@Override
	@Deprecated
	public int size() {
		return (int) Math.min(n,  Integer.MAX_VALUE);
	}

	/**
	 * Returns the number of bits used by this structure.
	 *
	 * @return the number of bits used by this structure.
	 */
	public long numBits() {
		if (n == 0) return 0;
		return data.size64() + offsetSeed.length * (long) Long.SIZE + decoder.numBits();
	}

	@Override
	public boolean containsKey(final Object o) {
		return true;
	}

	public static void main(final String[] arg) throws NoSuchMethodException, IOException, JSAPException {

		final SimpleJSAP jsap =
			new SimpleJSAP(GV3CompressedFunction.class.getName(), "Builds a GOV function mapping a newline-separated list" + " of strings to their ordinal position, or to specific values.",
					new Parameter[] {
							new FlaggedOption("encoding", ForNameStringParser.getParser(Charset.class), "UTF-8", JSAP.NOT_REQUIRED, 'e', "encoding", "The string file encoding."),
							new FlaggedOption("tempDir", FileStringParser.getParser(), JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'T', "temp-dir", "A directory for temporary files."),
							new Switch("iso", 'i', "iso", "Use ISO-8859-1 coding internally (i.e., just use the lower eight bits of each character)."),
							new Switch("utf32", JSAP.NO_SHORTFLAG, "utf-32", "Use UTF-32 internally (handles surrogate pairs)."),
							new Switch("byteArray", 'b', "byte-array", "Create a function on byte arrays (no character encoding)."),
							new Switch("zipped", 'z', "zipped", "The string list is compressed in gzip format."),
							new FlaggedOption("codec", JSAP.STRING_PARSER, "HUFFMAN", JSAP.NOT_REQUIRED, 'C', "codec", "The name of the codec to use (UNARY, BINARY, GAMMA, HUFFMAN, LLHUFFMAN)."),
							new FlaggedOption("limit", JSAP.INTEGER_PARSER, "20", JSAP.NOT_REQUIRED, 'l', "limit", "Decoding-table length limit for the LLHUFFMAN codec."),
							new FlaggedOption("values", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'v', "values", "A binary file in DataInput format containing a long for each string (otherwise, the values will be the ordinal positions of the strings)."),
							new UnflaggedOption("function", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The filename for the serialised GOV function."),
							new UnflaggedOption("stringFile", JSAP.STRING_PARSER, "-", JSAP.NOT_REQUIRED, JSAP.NOT_GREEDY, "The name of a file containing a newline-separated list of strings, or - for standard input; in the first case, strings will not be loaded into core memory."),
			});

		final JSAPResult jsapResult = jsap.parse(arg);
		if (jsap.messagePrinted()) return;

		final String functionName = jsapResult.getString("function");
		final String stringFile = jsapResult.getString("stringFile");
		final Charset encoding = (Charset) jsapResult.getObject("encoding");
		final File tempDir = jsapResult.getFile("tempDir");
		final boolean byteArray = jsapResult.getBoolean("byteArray");
		final boolean zipped = jsapResult.getBoolean("zipped");
		final boolean iso = jsapResult.getBoolean("iso");
		final boolean utf32 = jsapResult.getBoolean("utf32");
		final int limit = jsapResult.getInt("limit");

		Codec codec = null;
		switch (jsapResult.getString("codec")) {
		case "UNARY":
			codec = new Codec.Unary();
			break;
		case "BINARY":
			codec = new Codec.Binary();
			break;
		case "GAMMA":
			codec = new Codec.Gamma();
			break;
		case "HUFFMAN":
			codec = new Codec.Huffman();
			break;
		case "LLHUFFMAN":
			codec = new Codec.Huffman(limit);
			break;
		default:
			throw new IllegalArgumentException("Unknown codec \"" + jsapResult.getString("codec") + "\"");
		}

		final LongIterable values = jsapResult.userSpecified("values") ? BinIO.asLongIterable(jsapResult.getString("values")) : null;

		if (byteArray) {
			if ("-".equals(stringFile)) throw new IllegalArgumentException("Cannot read from standard input when building byte-array functions");
			if (iso || utf32 || jsapResult.userSpecified("encoding")) throw new IllegalArgumentException("Encoding options are not available when building byte-array functions");
			final Collection<byte[]> collection = new FileLinesByteArrayCollection(stringFile, zipped);if (jsapResult.userSpecified("values"))
			BinIO.storeObject(new GV3CompressedFunction<>(collection, TransformationStrategies.rawByteArray(), values, false, tempDir, null, codec), functionName);

		} else {
			final Collection<MutableString> collection;
			if ("-".equals(stringFile)) {
				final ProgressLogger pl = new ProgressLogger(LOGGER);
				pl.displayLocalSpeed = true;
				pl.displayFreeMemory = true;
				pl.start("Loading strings...");
				collection = new LineIterator(new FastBufferedReader(new InputStreamReader(zipped ? new GZIPInputStream(System.in) : System.in, encoding)), pl).allLines();
				pl.done();
			} else collection = new FileLinesCollection(stringFile, encoding.toString(), zipped);
			final TransformationStrategy<CharSequence> transformationStrategy = iso ? TransformationStrategies.rawIso() : utf32 ? TransformationStrategies.rawUtf32() : TransformationStrategies.rawUtf16();

			BinIO.storeObject(new GV3CompressedFunction<>(collection, transformationStrategy, values, false, tempDir, null, codec), functionName);
		}
		LOGGER.info("Completed.");
	}
}
