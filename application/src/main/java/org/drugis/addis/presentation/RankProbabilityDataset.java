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

package org.drugis.addis.presentation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.drugis.mtc.Treatment;
import org.drugis.mtc.summary.RankProbabilitySummary;
import org.jfree.data.category.DefaultCategoryDataset;

@SuppressWarnings("serial")
public class RankProbabilityDataset extends DefaultCategoryDataset {
	private RankProbabilitySummary d_summary;

	public RankProbabilityDataset(RankProbabilitySummary rankProbabilitySummary) {
		d_summary = rankProbabilitySummary;
		PropertyChangeListener listener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent arg0) {
				fireDatasetChanged();
			}
		};
		d_summary.addPropertyChangeListener(listener);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int getRowIndex(Comparable key) {
		if (!(key instanceof String)) {
			return -1;
		}
		String str = (String) key;
		int idx = Integer.parseInt(str.substring(5)) - 1;
		if (idx < 0 || idx >= d_summary.getTreatments().size()) {
			return -1;
		}
		return idx;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int getColumnIndex(Comparable key) {
		if (!(key instanceof Treatment)) {
			return -1;
		}
		Treatment treatment = (Treatment) key;
		return d_summary.getTreatments().indexOf(treatment);
	}
	
	@Override
	public String getRowKey(int row) {
		return "Rank " + (row + 1);
	}
	
	@Override
	public Treatment getColumnKey(int column) {
		return d_summary.getTreatments().get(column); 
	}
	
	@Override
	public List<String> getRowKeys() {
		List<String> keys = new ArrayList<String>();
		for (int i = 0; i < d_summary.getTreatments().size(); ++i) {
			keys.add("Rank " + (i+1));
		}
		return keys;
	}
	
	@Override
	public List<Treatment> getColumnKeys() {
		List<Treatment> keys = new ArrayList<Treatment>();
		for (Treatment t : d_summary.getTreatments()) {
			keys.add(t);
		}
		return keys;
	}
	
	@Override
	public int getRowCount() {
		return d_summary.getTreatments().size();
	}

	@Override
	public int getColumnCount() {
		return d_summary.getTreatments().size();
	}
	
	@Override
	public Number getValue(int row, int column) {
		return d_summary.getValue(d_summary.getTreatments().get(column), row + 1);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Number getValue(Comparable rowKey, Comparable columnKey) {
		return getValue(getRowIndex(rowKey), getColumnIndex(columnKey));
	}
}
