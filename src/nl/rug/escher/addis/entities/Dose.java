package nl.rug.escher.addis.entities;

import com.jgoodies.binding.beans.Model;

public class Dose extends Model {
	private static final long serialVersionUID = -8789524312421940513L;
	private SIUnit d_unit;
	private Double d_quantity;
	
	public static final String PROPERTY_UNIT = "unit";
	public static final String PROPERTY_QUANTITY = "quantity";
	
	@Deprecated
	public Dose() {
		
	}
	
	public Dose(double quantity, SIUnit unit) {
		d_quantity = quantity;
		d_unit = unit;
	}
	
	public SIUnit getUnit() {
		return d_unit;
	}
	
	public void setUnit(SIUnit unit) {
		SIUnit oldVal = d_unit;
		d_unit = unit;
		firePropertyChange(PROPERTY_UNIT, oldVal, d_unit);
	}
	
	public Double getQuantity() {
		return d_quantity;
	}
	
	public void setQuantity(Double quantity) {
		Double oldVal = d_quantity;
		d_quantity = quantity;
		firePropertyChange(PROPERTY_QUANTITY, oldVal, d_quantity);
	}
	
	public String toString() {
		if (d_quantity == null || d_unit == null) {
			return "INCOMPLETE";
		}
		return d_quantity.toString() + " " + d_unit.toString();
	}
}
