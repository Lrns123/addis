/*
 * This file is part of ADDIS (Aggregate Data Drug Information System).
 * ADDIS is distributed from http://drugis.org/.
 * Copyright (C) 2009 Gert van Valkenhoef, Tommi Tervonen.
 * Copyright (C) 2010 Gert van Valkenhoef, Tommi Tervonen, 
 * Tijs Zwinkels, Maarten Jacobs, Hanno Koeslag, Florin Schimbinschi, 
 * Ahmad Kamal, Daniel Reid.
 * Copyright (C) 2011 Gert van Valkenhoef, Ahmad Kamal, 
 * Daniel Reid, Florin Schimbinschi.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.drugis.addis.entities.relativeeffect;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.TDistribution;
import org.apache.commons.math.distribution.TDistributionImpl;
import org.drugis.common.beans.AbstractObservable;

public abstract class TransformedStudentTBase extends AbstractObservable implements	Distribution {

	protected final double d_mu;
	protected final double d_sigma;
	protected final int d_degreesOfFreedom;
	protected final TDistribution d_dist;

	public TransformedStudentTBase(double mu, double sigma, int degreesOfFreedom) {
		if (Double.isNaN(mu)) throw new IllegalArgumentException("mu may not be NaN");
		if (Double.isNaN(sigma)) throw new IllegalArgumentException("sigma may not be NaN");
		if (sigma < 0.0) throw new IllegalArgumentException("sigma must be >= 0.0");
		if (degreesOfFreedom < 1) throw new IllegalArgumentException("degreesOfFreedom must be >= 1");
		d_mu = mu;
		d_sigma = sigma;
		d_degreesOfFreedom = degreesOfFreedom;
		d_dist = new TDistributionImpl(getDegreesOfFreedom());
	}

	protected double calculateQuantile(double p) {
		try {
			return d_dist.inverseCumulativeProbability(p) * d_sigma + d_mu;
		} catch (MathException e) {
			throw new RuntimeException(e);
		}
	}

	protected double calculateCumulativeProbability(double x) {
		try {
			return d_dist.cumulativeProbability((x - d_mu) / d_sigma);
		} catch (MathException e) {
			throw new RuntimeException(e);
		}
	}

	public double getMu() {
		return d_mu;
	}

	public double getSigma() {
		return d_sigma;
	}

	public int getDegreesOfFreedom() {
		return d_degreesOfFreedom;
	}


	@Override
	public boolean equals(Object o) {
		if(o instanceof TransformedStudentTBase) {
			TransformedStudentTBase other = (TransformedStudentTBase) o;
			return other.d_mu == d_mu && other.d_sigma == d_sigma && other.d_degreesOfFreedom == d_degreesOfFreedom && other.getClass().equals(getClass());
		}
		return false;
	}
}