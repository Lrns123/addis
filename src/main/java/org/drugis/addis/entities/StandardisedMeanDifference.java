package org.drugis.addis.entities;

import java.util.Set;

import org.drugis.common.Interval;
import org.drugis.common.StudentTTable;

public class StandardisedMeanDifference extends AbstractEntity implements RelativeEffectContinuous {
	private static final long serialVersionUID = -8753337320258281527L;
	/*
	 * The Standardised Mean Difference is calculated through Cohen's d and adjusted with J(degrees of freedom)
	 * to result in Hedges g. All formulas are based on The Handbook of Research Synthesis and Meta-Analysis 
	 * by Cooper et al. 2nd Edition pages 225-230
	 */
	
	private ContinuousMeasurement d_subject;
	private ContinuousMeasurement d_baseline;

	public StandardisedMeanDifference(ContinuousMeasurement baseline,
			ContinuousMeasurement subject) {
			d_subject = subject;
			d_baseline = baseline;
	}

	public Interval<Double> getConfidenceInterval() {
		double t = StudentTTable.getT(getDegreesOfFreedom());

		return new Interval<Double>(getRatio() - t * getError(), getRatio() + t * getError());
	}

	public ContinuousMeasurement getDenominator() {
		return d_baseline;
	}

	public Endpoint getEndpoint() {
		return d_subject.getEndpoint();
	}

	public ContinuousMeasurement getNumerator() {
		return d_subject;
	}

	public Double getRatio() {
		return getCorrectionJ() * getCohenD();
	}
	
	public Double getError() {
		return Math.sqrt(square(getCorrectionJ()) * getCohenVariance());
	}

	public Integer getSampleSize() {
		return d_subject.getSampleSize() + d_baseline.getSampleSize();
	}
	
	private Double square(double x) {
		return x*x;
	}

	@Override
	public Set<Entity> getDependencies() {
		return null;
	}
	
	// Package access only:
	double getCohenD() {
		return (d_subject.getMean() - d_baseline.getMean()) / getPooledStdDev();
	}

	double getCohenVariance() {
		double frac1 = getSampleSize() / (d_subject.getSampleSize() * d_baseline.getSampleSize());
		double frac2 = square(getCohenD()) / (2 * getSampleSize());
		return frac1 + frac2;
	}
	
	double getCorrectionJ() {
		return (1 - (3 / (4 * getDegreesOfFreedom() - 1)));
	}
	
	private double getPooledStdDev() {
		double numerator = (d_subject.getSampleSize() - 1) * square(d_subject.getStdDev()) 
							+ (d_baseline.getSampleSize() - 1) * square(d_baseline.getStdDev());
		return Math.sqrt(numerator/(double) getDegreesOfFreedom());
	}
	
	private int getDegreesOfFreedom() {
		return getSampleSize() - 2;
	}
}
