package nl.rug.escher.addis.entities;

import java.beans.PropertyChangeEvent;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;


@PersistenceCapable(detachable="true")
public class BasicContinuousMeasurement extends BasicMeasurement implements ContinuousMeasurement {
	private Double d_mean;
	private Double d_stdDev;
	
	public BasicContinuousMeasurement() {
		
	}
	
	public BasicContinuousMeasurement(Endpoint e) {
		super(e);
		d_mean = 0.0; // FIXME
		d_stdDev = 0.0; // FIXME
	}
	
	@Persistent
	public Double getMean() {
		return d_mean;
	}
	
	public void setMean(Double mean) {
		String oldLabel = getLabel();
		Double oldVal = d_mean;
		d_mean = mean;
		firePropertyChange(new PropertyChangeEvent(this, PROPERTY_MEAN, oldVal, d_mean));
		firePropertyChange(new PropertyChangeEvent(this, PROPERTY_LABEL, oldLabel, getLabel()));
	}
	
	@Persistent
	public Double getStdDev() {
		return d_stdDev;
	}
	
	public void setStdDev(Double stdDev) {
		String oldLabel = getLabel();
		Double oldVal = d_stdDev;
		d_stdDev = stdDev;
		firePropertyChange(new PropertyChangeEvent(this, PROPERTY_STDDEV, oldVal, d_stdDev));
		firePropertyChange(new PropertyChangeEvent(this, PROPERTY_LABEL, oldLabel, getLabel()));
	}
	
	public String getLabel() {
		return toString();
	}

	@Override
	public String toString() {
		if (d_mean == null || d_stdDev == null) {
			return "INCOMPLETE"; 
		}
		return d_mean.toString() + " \u00B1 " + d_stdDev.toString();
	}
}