/*
 * Sux4J: Succinct data structures for Java
 *
 * Copyright (C) 2016-2020 Sebastiano Vigna
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

package it.unimi.dsi.sux4j.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;
import com.martiansoftware.jsap.stringparsers.ForNameStringParser;

import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.objects.Object2LongFunction;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.LineIterator;

public class HollowTrieSpeedTest {

	public static void main(final String[] arg) throws NoSuchMethodException, IOException, JSAPException, ClassNotFoundException {

		final SimpleJSAP jsap = new SimpleJSAP(HollowTrieSpeedTest.class.getName(), "Tests the speed of a hollow trie.",
				new Parameter[] {
					new FlaggedOption("bufferSize", JSAP.INTSIZE_PARSER, "64Ki", JSAP.NOT_REQUIRED, 'b',  "buffer-size", "The size of the I/O buffer used to read terms."),
					new FlaggedOption("encoding", ForNameStringParser.getParser(Charset.class), "UTF-8", JSAP.NOT_REQUIRED, 'e', "encoding", "The term file encoding."),
					new Switch("zipped", 'z', "zipped", "The term list is compressed in gzip format."),
					new FlaggedOption("termFile", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'o', "offline", "Read terms from this file (without loading them into core memory) instead of standard input."),
					new UnflaggedOption("trie", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The filename for the serialised hollow trie.")
		});

		final JSAPResult jsapResult = jsap.parse(arg);
		if (jsap.messagePrinted()) return;

		final int bufferSize = jsapResult.getInt("bufferSize");
		final String trieName = jsapResult.getString("trie");
		final String termFile = jsapResult.getString("termFile");
		//final Class<?> tableClass = jsapResult.getClass("class");
		final Charset encoding = (Charset)jsapResult.getObject("encoding");
		final boolean zipped = jsapResult.getBoolean("zipped");

		@SuppressWarnings("unchecked")
		final Object2LongFunction<? extends CharSequence> hollowTrie = (Object2LongFunction<? extends CharSequence>)BinIO.loadObject(trieName);

		Iterator<? extends CharSequence> i;

		for(int k = 10; k-- != 0;) {
			if (termFile == null) i = new LineIterator(new FastBufferedReader(new InputStreamReader(System.in, encoding), bufferSize));
			else i = new LineIterator(new FastBufferedReader(new InputStreamReader(zipped ? new GZIPInputStream(new FileInputStream(termFile)) : new FileInputStream(termFile), encoding), bufferSize));
			long time = -System.currentTimeMillis();
			int j = 0;
			while(i.hasNext()) {
				hollowTrie.getLong(i.next());
				if (j++ % 10000 == 0) System.err.print('.');
			}
			System.err.println();
			time += System.currentTimeMillis();
			System.err.println(time / 1E3 + "s, " + (time * 1E6) / j + " ns/vector");
		}
	}
}
