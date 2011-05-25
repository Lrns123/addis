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

package org.drugis.addis.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drugis.addis.entities.StudyActivity.UsedBy;
import org.drugis.addis.util.EntityUtil;
import org.drugis.addis.util.RebuildableHashMap;
import org.drugis.addis.util.comparator.OutcomeComparator;
import org.drugis.common.DateUtil;
import org.drugis.common.EqualsUtil;

import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.list.ObservableList;

public class Study extends AbstractEntity implements Comparable<Study>, Entity, TypeWithName {

	public static class MeasurementKey extends AbstractEntity implements Entity {

		private Variable d_variable;
		private Arm d_arm;

		public MeasurementKey(Variable v, Arm a)  {
			if (v == null) {
				throw new NullPointerException("Variable may not be null");
			}
			if (v instanceof OutcomeMeasure && a == null) {
				throw new NullPointerException("Arm may not be null for Endpoints/ADEs");
			}
			d_variable = v;
			d_arm = a;
		}

		public Variable getVariable() {
			return d_variable;
		}
		
		public Arm getArm() {
			return d_arm;
		}
		
		@Override
		public String toString() {
			return "<" + d_variable + ", " + d_arm + ">";
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof MeasurementKey) { 
				MeasurementKey other = (MeasurementKey)o;
				return d_variable.equals(other.d_variable) && EqualsUtil.equal(d_arm, other.d_arm);
			}
			return false;
		}

		@Override
		public int hashCode() {
			int code = 1;
			code = code * 31 + d_variable.hashCode();
			code = code * 31 + (d_arm == null ? 0 : d_arm.hashCode());
			return code;
		}

		@Override
		public Set<? extends Entity> getDependencies() {
			return null;
		}
	}
	
	@SuppressWarnings("serial")
	public static class StudyOutcomeMeasure<T extends Variable> extends ObjectWithNotes<T> {
		public StudyOutcomeMeasure(T obj) {
			super(obj);
		}
		
		@Override
		public StudyOutcomeMeasure<T> clone() {
			StudyOutcomeMeasure<T> clone = new StudyOutcomeMeasure<T>(getValue());
			clone.getNotes().addAll(getNotes());
			return clone;
		}
	}

	public final static String PROPERTY_INDICATION = "indication";
	public final static String PROPERTY_CHARACTERISTICS = "characteristics";
	
	public final static String PROPERTY_ENDPOINTS = "endpoints";
	public final static String PROPERTY_ADVERSE_EVENTS = "adverseEvents";
	public final static String PROPERTY_POPULATION_CHARACTERISTICS = "populationCharacteristics";
	
	public final static String PROPERTY_ARMS = "arms";
	public final static String PROPERTY_EPOCHS = "epochs";
	public final static String PROPERTY_STUDY_ACTIVITIES = "studyActivities";
	
	private ObjectWithNotes<String> d_name;
	private ObjectWithNotes<Indication> d_indication;
	private CharacteristicsMap d_chars = new CharacteristicsMap();
	
	private List<StudyOutcomeMeasure<Endpoint>> d_endpoints = new ArrayList<StudyOutcomeMeasure<Endpoint>>();
	private List<StudyOutcomeMeasure<AdverseEvent>> d_adverseEvents = new ArrayList<StudyOutcomeMeasure<AdverseEvent>>();
	private List<StudyOutcomeMeasure<PopulationCharacteristic>> d_populationChars = new ArrayList<StudyOutcomeMeasure<PopulationCharacteristic>>();

	private ObservableList<Arm> d_arms = new ArrayListModel<Arm>();
	private ObservableList<Epoch> d_epochs = new ArrayListModel<Epoch>();
	private ObservableList<StudyActivity> d_studyActivities = new ArrayListModel<StudyActivity>(); 

	private RebuildableHashMap<MeasurementKey, BasicMeasurement> d_measurements = new RebuildableHashMap<MeasurementKey, BasicMeasurement>();
	
	public Study() {
		d_indication = new ObjectWithNotes<Indication>(null);
		d_name = new ObjectWithNotes<String>(null);
	}

	@Override
	public Study clone() {
		Study newStudy = new Study();
		newStudy.d_name = d_name.clone();
		newStudy.d_indication = d_indication.clone();

		newStudy.d_arms = cloneArms();

		newStudy.d_endpoints = cloneStudyOutcomeMeasures(d_endpoints);
		newStudy.d_adverseEvents = cloneStudyOutcomeMeasures(d_adverseEvents);
		newStudy.d_populationChars = cloneStudyOutcomeMeasures(d_populationChars);

		// Copy measurements _AFTER_ the outcomes, since setEndpoints() etc removes orphan measurements from the study.
		newStudy.setMeasurements(cloneMeasurements(newStudy.getArms()));
		
		newStudy.setCharacteristics(cloneCharacteristics());
		
		for(Epoch e: getEpochs()) {
			newStudy.getEpochs().add(e.clone());
		}

		for(StudyActivity sa: getStudyActivities()) {
			newStudy.getStudyActivities().add(cloneStudyActivity(sa, newStudy.getArms(), newStudy.getEpochs()));
		}
		
		return newStudy;
	}

	private StudyActivity cloneStudyActivity(StudyActivity sa, ObservableList<Arm> newArms, ObservableList<Epoch> newEpochs) {
		StudyActivity newSA = sa.clone();
		Set<UsedBy> newUsedBys = new HashSet<UsedBy>();
		for(UsedBy ub: sa.getUsedBy()) {
			newUsedBys.add(fixUsedBy(ub, newArms, newEpochs));
		}
		newSA.setUsedBy(newUsedBys);
		return newSA;
	}

	private UsedBy fixUsedBy(UsedBy ub, ObservableList<Arm> newArms, ObservableList<Epoch> newEpochs) {
		return new UsedBy(newArms.get(newArms.indexOf(ub.getArm())), newEpochs.get(newEpochs.indexOf(ub.getEpoch()))) ;
	}

	private <T extends Variable> List<StudyOutcomeMeasure<T>> cloneStudyOutcomeMeasures(List<StudyOutcomeMeasure<T>> soms) {
		List<StudyOutcomeMeasure<T>> list = new ArrayList<StudyOutcomeMeasure<T>>();
		for (StudyOutcomeMeasure<T> som : soms) {
			list.add(som.clone());
		}
		return list;
	}
	
	private CharacteristicsMap cloneCharacteristics() {
		CharacteristicsMap cm = new CharacteristicsMap();
		for(Characteristic c : d_chars.keySet()){
			cm.put(c, d_chars.get(c).clone());
		}
		return cm;
	}

	private Map<MeasurementKey, BasicMeasurement> cloneMeasurements(List<Arm> newArms) {
		HashMap<MeasurementKey, BasicMeasurement> hashMap = new HashMap<MeasurementKey, BasicMeasurement>();
		for(MeasurementKey key : d_measurements.keySet()) {
			hashMap.put(fixKey(key, newArms), d_measurements.get(key).clone());
		}
		return hashMap;
	}

	private MeasurementKey fixKey(MeasurementKey key, List<Arm> newArms) {
		if (key.getArm() == null) {
			return key;
		}
		int idx = getArms().indexOf(key.getArm());
		return new MeasurementKey(key.getVariable(), newArms.get(idx));
	}

	private ObservableList<Arm> cloneArms() {
		ObservableList<Arm> newList = new ArrayListModel<Arm>();
		for(Arm a : getArms()) {
			newList.add(a.clone());
		}
		return newList;
	}

	public Study(String id, Indication i) {
		d_name = new ObjectWithNotes<String>(id);
		d_indication = new ObjectWithNotes<Indication>(i);
		d_arms = new ArrayListModel<Arm>();
		setCharacteristic(BasicStudyCharacteristic.CREATION_DATE, DateUtil.getCurrentDateWithoutTime());
		setCharacteristic(BasicStudyCharacteristic.TITLE, "");
		setCharacteristic(BasicStudyCharacteristic.PUBMED, new PubMedIdList());
	}

	public ObservableList<Arm> getArms() {
		return d_arms;
	}

	@Deprecated
	public void addArm(Arm arm) {
		getArms().add(arm);
	}
	
	public ObservableList<Epoch> getEpochs() {
		return d_epochs;
	}

	public ObservableList<StudyActivity> getStudyActivities() {
		return d_studyActivities;
	}
	
	/**
	 * Set a particular studyActivity as being used by an (arm, epoch) pair.
	 * Constraint: At most one StudyActivity exists for each (arm, epoch) pair; any previous entry will be overwritten.
	 * @param arm
	 * @param epoch
	 * @param activity A StudyActivity or null; when null, clears any activity at that (arm, epoch) pair.
	 */
	public void setStudyActivityAt(Arm arm, Epoch epoch, StudyActivity activity) {
		assertContains(d_arms, arm);
		assertContains(d_epochs, epoch);
		
		if (activity == null) {
			clearStudyActivityAt(arm, epoch);
		} else {
			assertContains(d_studyActivities, activity);
			activity = d_studyActivities.get(d_studyActivities.indexOf(activity)); // ensure we have the *same* object, not just an *equal* one.
			clearStudyActivityAt(arm, epoch);
			Set<UsedBy> usedBy = new HashSet<UsedBy>(activity.getUsedBy());
			usedBy.add(new UsedBy(arm, epoch));
			activity.setUsedBy(usedBy);
		}
	}

	public StudyActivity getStudyActivityAt(Arm arm, Epoch epoch) {
		UsedBy coordinate = new UsedBy(arm, epoch);
		for (StudyActivity activity : d_studyActivities) {
			if (activity.getUsedBy().contains(coordinate)) {
				return activity;
			}
		}
		return null;
	}
	
	private <E> void assertContains(ObservableList<E> list, E item) {
		if (!list.contains(item)) {
			throw new IllegalArgumentException("The " + item.getClass().getSimpleName() + " <" + item + " > does not exist in this study");
		}
	}

	private void clearStudyActivityAt(Arm arm, Epoch epoch) {
		UsedBy coordinate = new UsedBy(arm, epoch);
		for (StudyActivity activity : d_studyActivities) {
			if (activity.getUsedBy().contains(coordinate)) {
				Set<UsedBy> usedBy = new HashSet<UsedBy>(activity.getUsedBy());
				usedBy.remove(coordinate);
				activity.setUsedBy(usedBy);
			}
		}
	}

	public Set<Drug> getDrugs() {
		Set<Drug> drugs = new HashSet<Drug>();
		for (Arm a : getArms()) {
			if (getDrug(a) != null) {
				drugs.add(getDrug(a));
			}
		}
		return drugs;
	}

	public Indication getIndication() {
		return d_indication.getValue();
	}

	public void setIndication(Indication indication) {
		Indication oldInd = d_indication.getValue();
		d_indication.setValue(indication);
		firePropertyChange(PROPERTY_INDICATION, oldInd, indication);
	}

	@Override
	public Set<Entity> getDependencies() {
		HashSet<Entity> dep = new HashSet<Entity>(getDrugs());
		dep.addAll(getOutcomeMeasures());
		dep.addAll(getPopulationCharacteristics());
		dep.add(d_indication.getValue());
		return dep;
	}
	
	public Object getCharacteristic(Characteristic c) {
		return d_chars.get(c) != null ? d_chars.get(c).getValue() : null;
	}
	
	public void setCharacteristic(BasicStudyCharacteristic c, Object val) {
		ObjectWithNotes<?> charVal = getCharacteristicWithNotes(c);
		if (charVal != null) {
			charVal.setValue(val);
			setCharacteristicWithNotes(c, charVal); //FIXME: this is a hack because d_chars is exposed & also firing events.
		} else {
			setCharacteristicWithNotes(c, new ObjectWithNotes<Object>(val));
		}
	}
	
	public ObjectWithNotes<?> getCharacteristicWithNotes(Characteristic c) {
		return d_chars.get(c);
	}
	
	public void setCharacteristicWithNotes(BasicStudyCharacteristic c, ObjectWithNotes<?> val) {
		d_chars.put(c, val);
		firePropertyChange(PROPERTY_CHARACTERISTICS, c, c);
	}

	public void setCharacteristics(CharacteristicsMap m) {
		d_chars = m;
	}

	public CharacteristicsMap getCharacteristics() {
		return d_chars;
	}

	public String getName() {
		return d_name.getValue();
	}

	public void setName(String name) {
		String oldVal = d_name.getValue();
		d_name.setValue(name);
		firePropertyChange(PROPERTY_NAME, oldVal, name);
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Study) {
			Study other = (Study)o;
			if (other.getName() == null) {
				return getName() == null;
			}
			return other.getName().equals(getName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getName() == null ? 0 : getName().hashCode();
	}

	public int compareTo(Study other) {
		return getName().compareTo(other.getName());
	}

	public BasicMeasurement getMeasurement(Variable v, Arm a) {
		return d_measurements.get(new MeasurementKey(v, a));
	}

	public BasicMeasurement getMeasurement(Variable v) {
		return getMeasurement(v, null);
	}

	private void forceLegalArguments(OutcomeMeasure e, Arm a, Measurement m) {
		if (!getArms().contains(a)) {
			throw new IllegalArgumentException("Arm " + a + " not part of this study.");
		}
		if (!getOutcomeMeasures().contains(e)) {
			throw new IllegalArgumentException("Outcome " + e + " not measured by this study.");
		}
		if (!m.isOfType(e.getType())) {
			throw new IllegalArgumentException("Measurement does not conform with outcome");
		}
	}

	public void setMeasurement(OutcomeMeasure e, Arm a, BasicMeasurement m) {
		forceLegalArguments(e, a, m);
		d_measurements.put(new MeasurementKey(e, a), m);
	}

	/**
	 * Set population characteristic measurement on arm.
	 * @param v
	 * @param a
	 * @param m
	 */
	public void setMeasurement(Variable v, Arm a, BasicMeasurement m) {
		forceLegalArguments(v, a, m);
		d_measurements.put(new MeasurementKey(v, a), m);
	}

	/**
	 * Set population characteristic measurement on study.
	 * @param v
	 * @param m
	 */
	public void setMeasurement(Variable v, BasicMeasurement m) {
		forceLegalArguments(v, null, m);
		setMeasurement(v, null, m);
	}

	private void forceLegalArguments(Variable v, Arm a, Measurement m) {
		if (!getPopulationCharacteristics().contains(v)) {
			throw new IllegalArgumentException("Variable " + v + " not in study");
		}
		if (a != null && !d_arms.contains(a)) {
			throw new IllegalArgumentException("Arm " + a + " not in study");
		}
		if (!m.isOfType(v.getType())) {
			throw new IllegalArgumentException("Measurement does not conform with outcome");
		}
	}

	public List<OutcomeMeasure> getOutcomeMeasures() {
		List<OutcomeMeasure> sortedList = new ArrayList<OutcomeMeasure>(getEndpoints());
		sortedList.addAll(getAdverseEvents());
		Collections.sort(sortedList, new OutcomeComparator());
		return sortedList;
	}

	public List<Endpoint> getEndpoints() {
		return extractVariables(d_endpoints);
	}

	private <T  extends Variable> List<T> extractVariables(List<StudyOutcomeMeasure<T>> soms) {
		List<T> vars = new ArrayList<T>();
		for (StudyOutcomeMeasure<T> som : soms) {
			vars.add(som.getValue());
		}
		return vars;
	}

	public List<AdverseEvent> getAdverseEvents() {
		return extractVariables(d_adverseEvents);
	}

	public List<PopulationCharacteristic> getPopulationCharacteristics() {
		return extractVariables(d_populationChars);
	}

	public List<? extends Variable> getVariables(Class<? extends Variable> type) {
		if (type == Endpoint.class) {
			return getEndpoints();
		} else if (type == AdverseEvent.class){
			return getAdverseEvents();
		} else if (type == OutcomeMeasure.class) {
			return Collections.unmodifiableList(getOutcomeMeasures());
		}
		return getPopulationCharacteristics(); 
	}

	private <T extends Variable> List<StudyOutcomeMeasure<T>> wrapVariables(List<T> vars) {
		List<StudyOutcomeMeasure<T>> soms = new ArrayList<StudyOutcomeMeasure<T>>();
		for (T v : vars) {
			soms.add(new StudyOutcomeMeasure<T>(v));
		}
		return soms;
	}
	
	public void setEndpoints(List<Endpoint> endpoints) {
		setEndpointsWithNotes(wrapVariables(endpoints));
	}

	private void setEndpointsWithNotes(List<StudyOutcomeMeasure<Endpoint>> endpoints) {
		List<Endpoint> oldVal = getEndpoints();
		d_endpoints = endpoints;
		firePropertyChange(PROPERTY_ENDPOINTS, oldVal, getEndpoints());
	}

	public void setAdverseEvents(List<AdverseEvent> adverseEvents) {
		setAdverseEventsWithNotes(wrapVariables(adverseEvents));
	}
	
	private void setAdverseEventsWithNotes(List<StudyOutcomeMeasure<AdverseEvent>> adverseEvents) {
		List<AdverseEvent> oldVal = getAdverseEvents();
		d_adverseEvents = adverseEvents;
		firePropertyChange(PROPERTY_ADVERSE_EVENTS, oldVal, getAdverseEvents());
	}

	public void setPopulationCharacteristics(List<PopulationCharacteristic> chars) {
		setPopulationCharacteristicsWithNotes(wrapVariables(chars));
	}

	private void setPopulationCharacteristicsWithNotes(List<StudyOutcomeMeasure<PopulationCharacteristic>> chars) {
		List<? extends Variable> oldVal = getVariables(PopulationCharacteristic.class);
		d_populationChars = chars;
		firePropertyChange(PROPERTY_POPULATION_CHARACTERISTICS, oldVal, getPopulationCharacteristics());
	}

	public void addAdverseEvent(AdverseEvent ade) {
		if (ade == null) 
			throw new NullPointerException("Cannot add a NULL outcome measure");

		List<StudyOutcomeMeasure<AdverseEvent>> newList = new ArrayList<StudyOutcomeMeasure<AdverseEvent>>(d_adverseEvents);
		newList.add(new StudyOutcomeMeasure<AdverseEvent>(ade));
		setAdverseEventsWithNotes(newList);
	}

	public void addEndpoint(Endpoint om) {
		if (om == null) 
			throw new NullPointerException("Cannot add a NULL outcome measure");

		List<StudyOutcomeMeasure<Endpoint>> newVal = new ArrayList<StudyOutcomeMeasure<Endpoint>>(d_endpoints);
		newVal.add(new StudyOutcomeMeasure<Endpoint>(om));
		setEndpointsWithNotes(newVal);
	}
	
	public void addPopulationCharacteristic(PopulationCharacteristic pc) {
		if (pc == null) 
			throw new NullPointerException("Cannot add a NULL outcome measure");

		List<StudyOutcomeMeasure<PopulationCharacteristic>> newVal = new ArrayList<StudyOutcomeMeasure<PopulationCharacteristic>>(d_populationChars);
		newVal.add(new StudyOutcomeMeasure<PopulationCharacteristic>(pc));
		setPopulationCharacteristicsWithNotes(newVal);
	}
	
	public void addVariable(Variable om) {
		if (om instanceof Endpoint)
			addEndpoint((Endpoint) om);
		else if (om instanceof AdverseEvent) {
			addAdverseEvent((AdverseEvent) om);
		} else if (om instanceof PopulationCharacteristic) {
			addPopulationCharacteristic((PopulationCharacteristic) om);
		} else {
			throw new IllegalStateException("Illegal OutcomeMeasure type " + om.getClass());
		}
	}

	// FIXME: use in arms/outcomeMeasures listeners
	private void removeOrphanMeasurements() {
		for (MeasurementKey k : new HashSet<MeasurementKey>(d_measurements.keySet())) {
			if (orphanKey(k)) {
				d_measurements.remove(k);
			}
		}
	}

	public BasicMeasurement buildDefaultMeasurement(Variable v, Arm a) {
		return v.buildMeasurement(a == null ? getSampleSize() : a.getSize());
	}

	private boolean orphanKey(MeasurementKey k) {
		// OutcomeMeasure measurement
		if (k.d_variable instanceof OutcomeMeasure) {
			if (!getOutcomeMeasures().contains(k.d_variable)) {
				return true;
			}
			if (!d_arms.contains(k.d_arm)){
				return true;
			}
			return false;
		}
		// PopulationChar measurements
		if (k.d_variable instanceof Variable) {
			if (!getVariables(Variable.class).contains(k.d_variable)) {
				return true;
			}
			if (k.d_arm != null && !d_arms.contains(k.d_arm)) {
				return true;
			}
			return false;
		}

		throw new IllegalStateException(k + " is not a valid measurement key");
	}

	public int getSampleSize() {
		int s = 0;
		for (Arm pg : d_arms)
			s += pg.getSize();
		return s;
	}

	public void removeEndpoint(int i) {
		ArrayList<StudyOutcomeMeasure<Endpoint>> newVal = new ArrayList<StudyOutcomeMeasure<Endpoint>>(d_endpoints);
		newVal.remove(i);
		setEndpointsWithNotes(newVal);
	}

	public Map<MeasurementKey, BasicMeasurement> getMeasurements() {
		return d_measurements;
	}

	private void setMeasurements(Map<MeasurementKey, BasicMeasurement> m) {
		d_measurements = new RebuildableHashMap<MeasurementKey, BasicMeasurement>(m);
	}

	public void setMeasurement(MeasurementKey key, BasicMeasurement value) {
		d_measurements.put(key, value);
	}

	public List<StudyOutcomeMeasure<Endpoint>> getStudyEndpoints() {
		return d_endpoints;
	}
	
	public List<StudyOutcomeMeasure<AdverseEvent>> getStudyAdverseEvents() {
		return d_adverseEvents;
	}
	
	public List<StudyOutcomeMeasure<PopulationCharacteristic>> getStudyPopulationCharacteristics() {
		return d_populationChars;
	}

	public ObjectWithNotes<?> getNameWithNotes() {
		return d_name;
	}
	
	public ObjectWithNotes<?> getIndicationWithNotes() {
		return d_indication;
	}

	@SuppressWarnings("unchecked")
	public void addStudyOutcomeMeasure(StudyOutcomeMeasure<?> value) {
		if (value.getValue() instanceof Endpoint) {
			d_endpoints.add((StudyOutcomeMeasure<Endpoint>) value);
		} else if (value.getValue() instanceof AdverseEvent) {
			d_adverseEvents.add((StudyOutcomeMeasure<AdverseEvent>) value);
		} else if (value.getValue() instanceof PopulationCharacteristic) {
			d_populationChars.add((StudyOutcomeMeasure<PopulationCharacteristic>) value);
		} else {
			throw new IllegalArgumentException("Unknown StudyOutcomeMeasure type: " + value.getValue());
		}
	}
	
	public TreatmentActivity getTreatment(Arm arm) {
		assertContains(d_arms, arm);
		if (d_epochs.isEmpty()) {
			return null;
		}
		Epoch epoch = getTreatmentEpoch();
		if (epoch == null) {
			return null;
		}
		return (TreatmentActivity)getStudyActivityAt(arm, epoch).getActivity();
	}
	
	private Epoch getTreatmentEpoch() {
		for (Epoch epoch : d_epochs) {
			if (isTreatmentEpoch(epoch)) return epoch;
		}
		return null;
	}

	private boolean isTreatmentEpoch(Epoch epoch) {
		for (Arm arm : d_arms) {
			StudyActivity sa = getStudyActivityAt(arm, epoch);
			if (sa == null || !(sa.getActivity() instanceof TreatmentActivity)) {
				return false;
			}
		}
		return true;
	}
	
	public Drug getDrug(Arm arm) {
		return getTreatment(arm) == null ? null : getTreatment(arm).getDrug();
	}
	
	public AbstractDose getDose(Arm arm) {
		return getTreatment(arm) == null ? null : getTreatment(arm).getDose();
	}

	public void setDrug(Arm arm, Drug drug) {
		getTreatment(arm).setDrug(drug);
	}

	public boolean deepEquals(Entity obj) {
		if (!equals(obj)) {
			return false;
		} 
		Study other = (Study)obj;
		return 	EntityUtil.deepEqual(other.getIndication(), getIndication()) &&
				EqualsUtil.equal(other.getCharacteristics(), getCharacteristics()) &&
				EntityUtil.deepEqual(other.getEndpoints(), getEndpoints()) &&
				EntityUtil.deepEqual(other.getAdverseEvents(), getAdverseEvents()) &&
				EntityUtil.deepEqual(other.getPopulationCharacteristics(), getPopulationCharacteristics()) &&
				EntityUtil.deepEqual(other.getArms(), getArms()) &&
				EntityUtil.deepEqual(other.getEpochs(), getEpochs()) &&
				EntityUtil.deepEqual(other.getStudyActivities(), getStudyActivities()) &&
				EntityUtil.deepEqual(other.getMeasurements(), getMeasurements()) && 
				EqualsUtil.equal(other.getNameWithNotes().getNotes(), getNameWithNotes().getNotes());
	}

	public Arm findArm(String armName) {
		for(Arm a : d_arms) {
			if(a.getName().equals(armName)) { 
				return a;
			}
		}
		return null;
	}

	public Epoch findEpoch(String epochName) {
		for(Epoch e : d_epochs) {
			if(e.getName().equals(epochName)) { 
				return e;
			}
		}
		return null;
	}

	public StudyActivity findStudyActivity(String activityName) {
		for(StudyActivity sa : d_studyActivities) {
			if(sa.getName().equals(activityName)) { 
				return sa;
			}
		}
		return null;
	}
	
	/**
	 * Creates an Arm, adds it to the Study and creates an appropriate TreatmentActivity in the last Epoch.
	 * @param name Name of the arm to be created.
	 * @param size Number of subjects in the arm to be created.
	 * @param drug The drug administered.
	 * @param dose The dose administered.
	 * @return The created arm, already added and embedded in the study structure.
	 */
	public Arm createAndAddArm(String name, Integer size, Drug drug, AbstractDose dose) {
		Arm arm = new Arm(name, size);
		getArms().add(arm);
		StudyActivity studyActivity = new StudyActivity(name + " treatment", new TreatmentActivity(drug, dose));
		getStudyActivities().add(studyActivity);
		if (getEpochs().isEmpty()) {
			getEpochs().add(new Epoch("Main phase", null));
		}
		Epoch epoch = getEpochs().get(getEpochs().size() - 1);
		this.setStudyActivityAt(arm, epoch, studyActivity);
		return arm;
	}

	public void rehashMeasurements() {
		d_measurements.rebuild();
	}

	/**
	 * @return The Drugs that have at least one Arm with a complete measurement for the Variable v.
	 */
	public Set<Drug> getMeasuredDrugs(Variable v) {
		Set<Drug> drugs = new HashSet<Drug>();
		for (Drug d : getDrugs()) {
			if (isMeasured(v, d)) {
				drugs.add(d);
			}
		}
		return drugs;
	}
	
	public List<Arm> getMeasuredArms(Variable v, Drug d) {
		List<Arm> arms = new ArrayList<Arm>();
		for (Arm a : getArms(d)) {
			if (isMeasured(v, a)) {
				arms.add(a);
			}
		}
		return arms;
	}

	private boolean isMeasured(Variable v, Drug d) {
		for (Arm a : getArms(d)) {
			if (isMeasured(v, a)) {
				return true;
			}
		}
		return false;
	}

	private boolean isMeasured(Variable v, Arm a) {
		return getMeasurement(v, a) != null && getMeasurement(v, a).isComplete();
	}

	private List<Arm> getArms(Drug d) {
		List<Arm> arms = new ArrayList<Arm>();
		for (Arm a : getArms()) {
			if (getDrug(a).equals(d)) {
				arms.add(a);
			}
		}
		return arms;
	}

}
