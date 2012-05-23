/*
 * This file is part of ADDIS (Aggregate Data Drug Information System).
 * ADDIS is distributed from http://drugis.org/.
 * Copyright (C) 2009 Gert van Valkenhoef, Tommi Tervonen.
 * Copyright (C) 2010 Gert van Valkenhoef, Tommi Tervonen, 
 * Tijs Zwinkels, Maarten Jacobs, Hanno Koeslag, Florin Schimbinschi, 
 * Ahmad Kamal, Daniel Reid.
 * Copyright (C) 2011 Gert van Valkenhoef, Ahmad Kamal, 
 * Daniel Reid, Florin Schimbinschi.
 * Copyright (C) 2012 Gert van Valkenhoef, Daniel Reid, 
 * Joël Kuiper, Wouter Reckman.
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

import org.drugis.addis.entities.OutcomeMeasure;
import org.drugis.addis.entities.mtcwrapper.MCMCModelWrapper;
import org.drugis.addis.presentation.ValueHolder;
import org.drugis.common.gui.task.TaskProgressModel;
import org.drugis.common.threading.NullTask;
import org.drugis.mtc.MCMCModel;

public abstract class MCMCPresentation implements Comparable<MCMCPresentation> {
	private MCMCModelWrapper d_wrapper;
	private final String d_name;
	protected final OutcomeMeasure d_om;
	private TaskProgressModel d_taskProgressModel;
	
	public MCMCPresentation(final MCMCModelWrapper wrapper, final OutcomeMeasure om, final String name) { 
		d_wrapper = wrapper;
		d_om = om;
		MCMCModel model = d_wrapper.getModel();
		d_taskProgressModel = model != null ? new TaskProgressModel(model.getActivityTask()) : new TaskProgressModel(new NullTask() {
			public boolean isFinished() {
				return true;
			}
			public boolean isStarted() {
				return true;
			}
		});
		d_name = name;
	}
	
	public TaskProgressModel getProgressModel() {
		return d_taskProgressModel;
	}
	
	public MCMCModelWrapper getWrapper() {
		return d_wrapper;
	}
	
	public MCMCModel getModel() {
		return d_wrapper.getModel();
	}

	public abstract ValueHolder<Boolean> isModelConstructed();	

	public OutcomeMeasure getOutcomeMeasure() {
		return d_om;
	}
	
	public abstract boolean hasSavedResults();
	
	@Override
	public String toString() { 
		return d_name;
	}
}
