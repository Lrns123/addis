/*
 * This file is part of ADDIS (Aggregate Data Drug Information System).
 * ADDIS is distributed from http://drugis.org/.
 * Copyright (C) 2009  Gert van Valkenhoef and Tommi Tervonen.
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

package org.drugis.addis.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.drugis.addis.entities.Arm;
import org.drugis.addis.entities.BasicMeasurement;
import org.drugis.addis.entities.Domain;
import org.drugis.addis.entities.Study;
import org.drugis.addis.gui.builder.StudyAddEndpointView;
import org.drugis.common.gui.OkCancelDialog;

import com.jgoodies.binding.PresentationModel;

@SuppressWarnings("serial")
public class StudyAddEndpointDialog extends OkCancelDialog {
	private Main d_main;
	private Domain d_domain;
	private Study d_study;
	private OutcomeMeasureHolder d_newEndpoint;
	private Map<Arm, BasicMeasurement> d_measurements;
	
	public StudyAddEndpointDialog(Main frame, Domain domain, Study study) {
		super(frame, "Add Endpoint to Study");
		this.setModal(true);
		d_main = frame;
		d_domain = domain;
		d_study = study;
		d_measurements = new HashMap<Arm, BasicMeasurement>();
		d_newEndpoint = new OutcomeMeasureHolder();
		final StudyAddEndpointView view = new StudyAddEndpointView(d_domain, d_study,
				new PresentationModel<OutcomeMeasureHolder>(d_newEndpoint), d_measurements,
				d_okButton, frame.getPresentationModelFactory());
		
		
		d_newEndpoint.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				initPanel(view);
			}
		});
		initPanel(view);
	}

	private void initPanel(StudyAddEndpointView view) {
		d_measurements.clear();
		getUserPanel().removeAll();
		getUserPanel().add(view.buildPanel());
		pack();
	}

	protected void cancel() {
		setVisible(false);
	}

	protected void commit() {
		setVisible(false);
		
		addEndpointToStudy();
		addMeasurementsToStudy();
		d_main.leftTreeFocusOnStudy(d_study);
	}

	private void addMeasurementsToStudy() {
		for (Entry<Arm, BasicMeasurement> entry : d_measurements.entrySet()) {
			d_study.setMeasurement(d_newEndpoint.getEndpoint(), entry.getKey(), entry.getValue());
		}
	}

	private void addEndpointToStudy() {
		d_study.addOutcomeMeasure(d_newEndpoint.getEndpoint());
	}
}
