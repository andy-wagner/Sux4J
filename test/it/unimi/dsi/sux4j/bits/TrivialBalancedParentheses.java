/*
 * Sux4J: Succinct data structures for Java
 *
 * Copyright (C) 2010-2020 Sebastiano Vigna
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

package it.unimi.dsi.sux4j.bits;

import it.unimi.dsi.bits.BitVector;

public class TrivialBalancedParentheses implements BalancedParentheses {
	private static final long serialVersionUID = 1L;
	private final BitVector v;

	public TrivialBalancedParentheses(final BitVector v) {
		this.v = v;
	}

	@Override
	public BitVector bitVector() {
		return v;
	}

	@Override
	public long enclose(final long pos) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long findClose(long pos) {
		if (! v.getBoolean(pos)) throw new IllegalArgumentException();
		int c = 1;
		while(++pos < v.length()) {
			if (! v.getBoolean(pos)) c--;
			else c++;
			if (c == 0) return pos;
		}

		throw new IllegalArgumentException();
	}

	@Override
	public long findOpen(long pos) {
		if (v.getBoolean(pos)) throw new IllegalArgumentException();

		int c = 1;
		while(--pos >= 0) {
			if (! v.getBoolean(pos)) c++;
			else c--;
			if (c == 0) return pos;
		}

		throw new IllegalArgumentException();
	}

	@Override
	public long numBits() {
		return 0;
	}
}
