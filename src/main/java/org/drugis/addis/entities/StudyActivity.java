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

import java.util.Collections;
import java.util.Set;

import org.drugis.addis.util.EntityUtil;
import org.drugis.addis.util.RebuildableHashSet;
import org.drugis.common.EqualsUtil;

import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.list.ObservableList;

public class StudyActivity extends AbstractNamedEntity<StudyActivity> implements TypeWithNotes {
	public static final String PROPERTY_ACTIVITY = "activity";
	public static final String PROPERTY_USED_BY = "usedBy";

	public static class UsedBy implements Comparable<UsedBy> {
		private final Epoch d_epoch;
		private final Arm d_arm;
		public UsedBy(Arm a, Epoch e) {
			assert(e != null);
			assert(a != null);
			d_epoch = e;
			d_arm = a;
		}
		public Epoch getEpoch() {
			return d_epoch;
		}
		public Arm getArm() {
			return d_arm;
		}
		
		
		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof UsedBy) {
				UsedBy other = (UsedBy) obj;
				return EqualsUtil.equal(other.getEpoch(), getEpoch()) && EqualsUtil.equal(other.getArm(), getArm());
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return 31 * d_epoch.hashCode() + d_arm.hashCode();
		}
		
		public int compareTo(UsedBy o) {
			int armsComp = getArm().getName().compareTo(o.getArm().getName());
			if(armsComp != 0) {
				return armsComp;
			} else {
				return getEpoch().getName().compareTo(o.getEpoch().getName());
			}
		}
	}

	private Activity d_activity;
	// FIXME: the hashCode() of UsedBy is non-static during the AddStudyWizard, so we cannot use this.
	private RebuildableHashSet<UsedBy> d_usedBy = new RebuildableHashSet<UsedBy>();
	private ObservableList<Note> d_notes = new ArrayListModel<Note>();
	
	public StudyActivity(String name, Activity activity) {
		super(name);
		d_activity = activity;
	}


	@Override
	public Set<? extends Entity> getDependencies() {
		return d_activity.getDependencies();
	}

	public void setActivity(Activity activity) {
		Activity oldValue = d_activity;
		d_activity = activity;
		firePropertyChange(PROPERTY_ACTIVITY, oldValue, d_activity);
	}

	public Activity getActivity() {
		return d_activity;
	}

	/**
	 * Set the set of (arm, epoch) pairs that use this StudyActivity.
	 * @param usedBy Set of (arm, epoch) pairs; defensively copied.
	 */
	public void setUsedBy(Set<UsedBy> usedBy) {
		Set<UsedBy> oldValue = d_usedBy;
		d_usedBy = new RebuildableHashSet<UsedBy>(usedBy);
		firePropertyChange(PROPERTY_USED_BY, oldValue, d_usedBy);
	}

	/**
	 * Get the set of (arm, epoch) pairs that use this StudyActivity.
	 * @return unmodifiable set.
	 */
	public Set<UsedBy> getUsedBy() {
		return Collections.unmodifiableSet(d_usedBy);
	}
	
	public void rebuildUsedBy() {
		d_usedBy.rebuild();
	}

	public ObservableList<Note> getNotes() {
		return d_notes ;
	}
	
	public boolean deepEquals(Entity obj) {
		if(!equals(obj)) return false;
		StudyActivity other = (StudyActivity) obj;
		return EntityUtil.deepEqual(other.getActivity(), getActivity()) && other.getUsedBy().equals(getUsedBy()) && 
			EntityUtil.deepEqual(other.getNotes(), getNotes());
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof StudyActivity) {
			return super.equals(obj);
		}
		return false;
	}
	
	@Override
	public StudyActivity clone() {
		StudyActivity cloned = new StudyActivity(getName(), cloneActivity());
		cloned.setUsedBy(getUsedBy()); // setUsedBy already copies the set, and UsedBy do not need cloning because they are immutable
		cloned.d_notes.addAll(d_notes);
		return cloned;
	}


	private Activity cloneActivity() {
		return d_activity instanceof TreatmentActivity ? ((TreatmentActivity) d_activity).clone() : d_activity;
	}

}
