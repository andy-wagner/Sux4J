/**
 * Ranking and selection structures.
 *
 * <p>
 * This package provides a number of implementations of <em>rank/select queries</em> for bits
 * vectors. <em>Ranking</em> is counting the number of ones in an initial segment of a bit vector.
 * <em>Selection</em> is finding the position of the <var>r</var>-th one. Both operation can be
 * performed in constant time on an array of <var>n</var> bits using <i>o</i>(<var>n</var>)
 * additional bits, but in practice linear data structures with small constants and theoretically
 * non-constant time work much better. Sux4J proposes a number of new, very efficient implementation
 * of rank and select oriented to 64-bit processors (in other words: they will be fairly slow on
 * 32-bit processors). The implementations are based on <em>broadword programming</em> and described
 * in Sebastiano Vigna, &ldquo;<a href="http://vigna.di.unimi.it/papers.php#VigBIRSQ">Broadword
 * Implementation of Rank/Select Queries</a>&rdquo;, in <i>Proc. of the 7th International Workshop
 * on Experimental Algorithms, WEA 2008</i>, volume 5038 of Lecture Notes in Computer Science, pages
 * 154&minus;168. Springer, 2008.
 *
 * <p>
 * For dense arrays, {@link it.unimi.dsi.sux4j.bits.Rank9} is the basic rank implementation;
 * {@link it.unimi.dsi.sux4j.bits.Rank16} is slightly slower but occupies much less space. Selection
 * can be performed using {@link it.unimi.dsi.sux4j.bits.SimpleSelect} for reasonably uniform bit
 * arrays, or using {@link it.unimi.dsi.sux4j.bits.Select9}, which occupies more space but
 * guarantees practical constant-time evaluation.
 *
 * <p>
 * For sparse arrays (e.g., representation of pointers in a bitstream) we provide
 * {@link it.unimi.dsi.sux4j.bits.SparseRank} and {@link it.unimi.dsi.sux4j.bits.SparseSelect}.
 * Their main feature is that <em>they do not require the original bit array</em>, as they use an
 * {@link it.unimi.dsi.sux4j.util.EliasFanoMonotoneLongBigList} to implement a succint dictionary
 * containing the positions of bits set. If the bit array is sufficiently sparse, such a
 * representation provides significant gains in space occupancy.
 *
 * <p>
 * All structures can be serialized. Since in some cases the original bit vector is stored inside
 * the structure, to avoid saving and loading twice the same vector we suggest to pack all
 * structures into a {@link it.unimi.dsi.sux4j.bits.RankSelect} instance.
 *
 * <p>
 * Note that all methods in this package are considered low-level and do not perform bound checks on
 * their arguments. Bound checks can be enabled, however, by enabling assertions.
 */
package it.unimi.dsi.sux4j.bits;
