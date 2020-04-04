package it.unimi.dsi.sux4j.test;

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


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;

public class ValueStats {
	public static final Logger LOGGER = LoggerFactory.getLogger(ValueStats.class);

	public static void main(final String[] arg) throws JSAPException, IOException {

		final SimpleJSAP jsap = new SimpleJSAP(ValueStats.class.getName(), "Prints statistical data about a binary list of longs.",
				new Parameter[] {
					new UnflaggedOption("input", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The input file.")
		});

		final JSAPResult jsapResult = jsap.parse(arg);
		if (jsap.messagePrinted()) return;

		final String input = jsapResult.getString("input");
		long max = Long.MIN_VALUE;
		long min = Long.MAX_VALUE;
		long tot = 0;
		final Long2LongOpenHashMap freqs = new Long2LongOpenHashMap();

		for(final LongIterator i = BinIO.asLongIterator(input); i.hasNext(); ) {
			final long x = i.nextLong();
			max = Math.max(max, x);
			min = Math.min(min, x);
			freqs.addTo(x, 1);
			tot++;
		}

		System.out.println("Min: " + min);
		System.out.println("Max: " + max);
		double entropy = 0;
		for(final LongIterator iterator = freqs.values().iterator(); iterator.hasNext();) {
			final double p = (double)iterator.nextLong() / tot;
			entropy += -p * Fast.log2(p);
		}
		System.out.println("Entropy: " + entropy);
	}
}
