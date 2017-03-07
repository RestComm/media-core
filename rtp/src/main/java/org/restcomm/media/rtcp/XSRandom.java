package org.restcomm.media.rtcp;

import java.util.Random;

public class XSRandom extends Random {

	private static final long serialVersionUID = -668678943206857357L;

	private long seed;

	public XSRandom() {
		this.seed = System.nanoTime();
	}

	/**
	 * Implementation of George Marsaglia's elegant Xorshift random generator
	 * 30% faster and better quality than the built-in {@link Random}
	 */
	@Override
	protected int next(int nbits) {
		long x = this.seed;
		x ^= (x << 21);
		x ^= (x >>> 35);
		x ^= (x << 4);
		this.seed = x;
		x &= ((1L << nbits) - 1);
		return (int) x;
	}

}
