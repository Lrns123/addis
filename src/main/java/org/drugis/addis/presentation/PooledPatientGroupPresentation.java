package org.drugis.addis.presentation;

import java.beans.PropertyChangeEvent;

import org.drugis.addis.entities.PooledPatientGroup;

import com.jgoodies.binding.value.AbstractValueModel;

//FIXME: there should be separate implementations of this class for each concrete PatientGroup,
//and these should implement the PROPERTY_LABEL, in stead of the PatientGroup itself.

@SuppressWarnings("serial")
public class PooledPatientGroupPresentation extends LabeledPresentationModel<PooledPatientGroup> {
	public static class LabelModel extends AbstractLabelModel<PooledPatientGroup> {
		public LabelModel(PooledPatientGroup bean) {
			super(bean);
		}
		
		@Override
		public String getValue() {
			return calcLabel();
		}
		
		private String calcLabel() {
			return "META " + getBean().getDrug().toString();
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			// IMMUTABLE
		}
	}

	public PooledPatientGroupPresentation(PooledPatientGroup bean) {
		super(bean);
	}

	@Override
	public AbstractValueModel getLabelModel() {
		return new LabelModel(getBean());
	}

}
