package org.mobicents.media.server.testsuite.general;

import java.math.BigDecimal;

/**
 * Class encapsulatin std dev algorithm. See
 * http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
 * #On-line_algorithm
 * 
 * @author baranowb
 * 
 */
public class Deviation {

	private static final BigDecimal _INC_ = new BigDecimal(1);

	private BigDecimal n = new BigDecimal(0);
	private double mu = 0.0;
	private double sq = 0.0;

	public void update(double x) {
		n = n.add(_INC_);

		double muNew = mu
				+ new BigDecimal((x - mu)).divide(n, BigDecimal.ROUND_FLOOR)
						.doubleValue();
		sq += (x - mu) * (x - muNew);
		mu = muNew;
	}

	public double getMean() {
		return mu;
	}

	public double getVariance() {
		return new BigDecimal(sq).divide(n, BigDecimal.ROUND_FLOOR)
				.doubleValue();
	}

}
