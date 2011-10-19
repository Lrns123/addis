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

package org.drugis.addis.entities.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.drugis.addis.entities.AbstractEntity;
import org.drugis.addis.entities.Arm;
import org.drugis.addis.entities.DrugSet;
import org.drugis.addis.entities.Entity;
import org.drugis.addis.entities.Indication;
import org.drugis.addis.entities.OutcomeMeasure;
import org.drugis.addis.entities.Study;
import org.drugis.addis.util.EntityUtil;
import org.drugis.common.EqualsUtil;

public abstract class AbstractMetaAnalysis extends AbstractEntity implements MetaAnalysis {
	
	private static class ArmMap extends HashMap<Study, Map<DrugSet, Arm>> {
		private static final long serialVersionUID = -8579169115557701584L;

		public ArmMap() {
			super();
		}
		
		public ArmMap(Map<Study, Map<DrugSet, Arm>> other) {
			super(other);
		}
	}
	
	protected OutcomeMeasure d_outcome;
	protected Indication d_indication;
	protected List<? extends Study> d_studies;
	protected List<DrugSet> d_drugs;
	protected String d_name = "";
	protected int d_totalSampleSize;
	protected ArmMap d_armMap;
	
	protected AbstractMetaAnalysis() {
		d_armMap = new ArmMap();
	}
	
	public AbstractMetaAnalysis(String name, 
			Indication indication, OutcomeMeasure om,
			List<? extends Study> studies, List<DrugSet> drugs, Map<Study, Map<DrugSet, Arm>> armMap) 
	throws IllegalArgumentException {
		checkDataConsistency(studies, indication, om);

		d_drugs = drugs;
		d_studies = studies;
		d_indication = indication;
		d_outcome = om;
		d_name = name;
		d_armMap = new ArmMap(armMap);
		
		setSampleSize();
	}
	
	public AbstractMetaAnalysis(String name, Indication indication, OutcomeMeasure om, Map<Study, Map<DrugSet, Arm>> armMap) { 
		this(name, indication, om, calculateStudies(armMap), calculateDrugs(armMap), armMap);
	}

	private void setSampleSize() {
		for (Study s : d_studies) {
			d_totalSampleSize += s.getSampleSize();
		}
	}
	
	@Override
	public String toString() {
		return getName();
	}

	public void setName(String name) {
		String oldName = d_name;
		d_name = name;
		firePropertyChange(PROPERTY_NAME, oldName, d_name);
	}

	protected void checkDataConsistency(List<? extends Study> studies, Indication indication, OutcomeMeasure om)
	throws IllegalArgumentException {
		if (studies.isEmpty())
			throw new IllegalArgumentException("studylist empty");

		for (int i = 0; i < studies.size(); i++) {
			if (!studies.get(i).getOutcomeMeasures().contains(om))
				throw new IllegalArgumentException("Not all studies are comparing OutcomeMeasure " + om);
			if (!studies.get(i).getIndication().equals(indication))
				throw new IllegalArgumentException("Not all studies measure indication " + indication);
		}
	}
	

	public String getName() {
		return d_name;
	}

	public int getSampleSize() {
		return d_totalSampleSize;
	}

	public List<Study> getIncludedStudies() {
		return Collections.unmodifiableList(d_studies);
	}

	public OutcomeMeasure getOutcomeMeasure() {
		return d_outcome;
	}

	public int getStudiesIncluded() {
		return d_studies.size();
	}

	@Override
	public Set<Entity> getDependencies() {
		HashSet<Entity> deps = new HashSet<Entity>();
		deps.addAll(EntityUtil.flatten(getIncludedDrugs()));
		deps.add(getIndication());
		deps.add(getOutcomeMeasure());
		deps.addAll(getIncludedStudies());
		return deps;
	}

	public Indication getIndication() {
		return d_indication;
	}
	
	@Override
	public boolean equals(Object o) { 
		if (o instanceof AbstractMetaAnalysis) {
			AbstractMetaAnalysis other = (AbstractMetaAnalysis)o;
			return (other.getClass() == getClass()) && other.getName().equals(getName());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	public int compareTo(MetaAnalysis o) {
		return getName().compareTo(o.getName());
	}
	
	public List<DrugSet> getIncludedDrugs() {
		return Collections.unmodifiableList(new ArrayList<DrugSet>(d_drugs));
	}
	
	public Arm getArm(Study s, DrugSet d) {
		return d_armMap.get(s).get(d);
	}
	
	public List<Arm> getArmList(){
		List <Arm>armList = new ArrayList<Arm>();
		for(Study s : d_armMap.keySet()){
			for(DrugSet d : d_armMap.get(s).keySet()){
				armList.add(d_armMap.get(s).get(d));
			}
		}
		return armList;
	}

	private static List<DrugSet> calculateDrugs(Map<Study, Map<DrugSet, Arm>> armMap) {
		SortedSet<DrugSet> drugs = new TreeSet<DrugSet>();
		for (Map<DrugSet, Arm> entry : armMap.values()) {
			drugs.addAll(entry.keySet());
		}
		return new ArrayList<DrugSet>(drugs);
	}

	private static List<Study> calculateStudies(Map<Study, Map<DrugSet, Arm>> armMap) {
		ArrayList<Study> studies = new ArrayList<Study>(armMap.keySet());
		Collections.sort(studies);
		return studies;
	}
	
	@Override
	public boolean deepEquals(Entity other) {
		if(!equals(other)) {
			return false;
		}
		AbstractMetaAnalysis o = (AbstractMetaAnalysis) other;
		return 
			EqualsUtil.equal(getType(), o.getType()) &&
			EntityUtil.deepEqual(getIncludedStudies(), o.getIncludedStudies()) &&
			EntityUtil.deepEqual(getIncludedDrugs(), o.getIncludedDrugs()) &&
			EqualsUtil.equal(getSampleSize(), o.getSampleSize()) &&
			EntityUtil.deepEqual(getOutcomeMeasure(), o.getOutcomeMeasure()) &&
			EntityUtil.deepEqual(getIndication(), o.getIndication());
	}
}