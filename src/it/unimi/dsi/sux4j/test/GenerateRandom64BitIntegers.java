package it.unimi.dsi.sux4j.test;

/*
 * Sux4J: Succinct data structures for Java
 *
 * Copyright (C) 2016-2019 Sebastiano Vigna
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


import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.util.XoRoShiRo128PlusRandomGenerator;

public class GenerateRandom64BitIntegers {
	public static final Logger LOGGER = LoggerFactory.getLogger(GenerateRandom64BitIntegers.class);

	public static void main(final String[] arg) throws JSAPException, IOException {

		final SimpleJSAP jsap = new SimpleJSAP(GenerateRandom64BitIntegers.class.getName(), "Generates a list of sorted 64-bit random integers in DataOutput format.",
				new Parameter[] {
					new FlaggedOption("gap", JSAP.INTSIZE_PARSER, "1", JSAP.NOT_REQUIRED, 'g', "gap", "Impose a minimum gap."),
					new UnflaggedOption("n", JSAP.LONG_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The number of integers (too small values might cause overflow)."),
					new UnflaggedOption("output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The output file.")
		});

		final JSAPResult jsapResult = jsap.parse(arg);
		if (jsap.messagePrinted()) return;

		final long n = jsapResult.getLong("n");
		final int gap = jsapResult.getInt("gap");
		final String output = jsapResult.getString("output");

		final RandomGenerator r = new XoRoShiRo128PlusRandomGenerator();

		final ProgressLogger pl = new ProgressLogger(LOGGER);
		pl.expectedUpdates = n;
		pl.start("Generating... ");

		BigInteger l = BigInteger.ZERO;
		final BigInteger limit = BigInteger.valueOf(256).pow(8);
		final long incr = (long)Math.floor(1.99 * (limit.divide(BigInteger.valueOf(n)).longValue())) - 1;

		@SuppressWarnings("resource")
		final DataOutputStream dos = new DataOutputStream(new FastBufferedOutputStream(new FileOutputStream(output)));

		LOGGER.info("Increment: " + incr);

		for(long i = 0; i < n; i++) {
			l = l.add(BigInteger.valueOf((r.nextLong() & 0x7FFFFFFFFFFFFFFFL) % incr + gap));
			if (l.compareTo(limit) > 0) throw new AssertionError(Long.toString(i));
			dos.writeLong(l.longValue());
			pl.lightUpdate();
		}


		pl.done();
		dos.close();

		LOGGER.info("Last/limit: " + (l.doubleValue() / limit.doubleValue()));
	}
}
