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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drugis.addis.entities.Arm;
import org.drugis.addis.entities.BasicContinuousMeasurement;
import org.drugis.addis.entities.BasicRateMeasurement;
import org.drugis.addis.entities.DrugSet;
import org.drugis.addis.entities.Entity;
import org.drugis.addis.entities.Indication;
import org.drugis.addis.entities.Measurement;
import org.drugis.addis.entities.OutcomeMeasure;
import org.drugis.addis.entities.Study;
import org.drugis.addis.entities.Variable;
import org.drugis.addis.entities.relativeeffect.NetworkRelativeEffect;
import org.drugis.addis.entities.relativeeffect.RelativeEffect;
import org.drugis.addis.util.EntityUtil;
import org.drugis.common.threading.Task;
import org.drugis.common.threading.ThreadHandler;
import org.drugis.mtc.BasicParameter;
import org.drugis.mtc.ConsistencyModel;
import org.drugis.mtc.ContinuousNetworkBuilder;
import org.drugis.mtc.DefaultModelFactory;
import org.drugis.mtc.DichotomousNetworkBuilder;
import org.drugis.mtc.InconsistencyModel;
import org.drugis.mtc.MCMCModel;
import org.drugis.mtc.MixedTreatmentComparison;
import org.drugis.mtc.Network;
import org.drugis.mtc.NetworkBuilder;
import org.drugis.mtc.NodeSplitModel;
import org.drugis.mtc.Parameter;
import org.drugis.mtc.Treatment;
import org.drugis.mtc.summary.NodeSplitPValueSummary;
import org.drugis.mtc.summary.NormalSummary;
import org.drugis.mtc.summary.QuantileSummary;
import org.drugis.mtc.summary.RankProbabilitySummary;

public class NetworkMetaAnalysis extends AbstractMetaAnalysis implements MetaAnalysis{
	
	transient private InconsistencyModel d_inconsistencyModel;
	transient private ConsistencyModel d_consistencyModel;
	transient private NetworkBuilder<? extends org.drugis.mtc.Measurement> d_builder;
	protected Map<MCMCModel, Map<Parameter, NormalSummary>> d_normalSummaries = 
		new HashMap<MCMCModel, Map<Parameter, NormalSummary>>();
	protected Map<MCMCModel, Map<Parameter, QuantileSummary>> d_quantileSummaries = 
		new HashMap<MCMCModel, Map<Parameter, QuantileSummary>>();
	protected Map<Parameter, NodeSplitPValueSummary> d_nodeSplitPValueSummaries = 
		new HashMap<Parameter, NodeSplitPValueSummary>();

	private boolean d_isContinuous = false;
	private RankProbabilitySummary d_rankProbabilitySummary;
	private Map<BasicParameter, NodeSplitModel> d_nodeSplitModels = new HashMap<BasicParameter, NodeSplitModel>();
	

	public NetworkMetaAnalysis(String name, Indication indication,
			OutcomeMeasure om, List<? extends Study> studies, Collection<DrugSet> drugs,
			Map<Study, Map<DrugSet, Arm>> armMap) throws IllegalArgumentException {
		super(name, indication, om, studies, sortDrugs(drugs), armMap);
	}
	
	private static List<DrugSet> sortDrugs(Collection<DrugSet> drugs) {
		ArrayList<DrugSet> list = new ArrayList<DrugSet>(drugs);
		Collections.sort(list);
		return list;
	}

	public NetworkMetaAnalysis(String name, Indication indication,
			OutcomeMeasure om, Map<Study, Map<DrugSet, Arm>> armMap) throws IllegalArgumentException {
		super(name, indication, om, armMap);
	}

	private InconsistencyModel createInconsistencyModel() {
		InconsistencyModel inconsistencyModel = (DefaultModelFactory.instance()).getInconsistencyModel((Network<? extends org.drugis.mtc.Measurement>) getBuilder().buildNetwork());
		d_normalSummaries.put(inconsistencyModel, new HashMap<Parameter, NormalSummary>());
		d_quantileSummaries.put(inconsistencyModel, new HashMap<Parameter, QuantileSummary>());
		return inconsistencyModel;
	}
	
	private ConsistencyModel createConsistencyModel() {
		ConsistencyModel consistencyModel = (DefaultModelFactory.instance()).getConsistencyModel(getBuilder().buildNetwork());
		d_normalSummaries.put(consistencyModel, new HashMap<Parameter, NormalSummary>());
		d_quantileSummaries.put(consistencyModel, new HashMap<Parameter, QuantileSummary>());
		return consistencyModel;
	}
	
	private NodeSplitModel createNodeSplitModel(BasicParameter node) {
		NodeSplitModel nodeSplitModel = (DefaultModelFactory.instance()).getNodeSplitModel(getBuilder().buildNetwork(), node);
		d_normalSummaries.put(nodeSplitModel, new HashMap<Parameter, NormalSummary>());
		d_quantileSummaries.put(nodeSplitModel, new HashMap<Parameter, QuantileSummary>());
		d_nodeSplitPValueSummaries.put(node, new NodeSplitPValueSummary(nodeSplitModel.getResults(), 
				nodeSplitModel.getDirectEffect(), nodeSplitModel.getIndirectEffect()));
		return nodeSplitModel;
	}

	private NetworkBuilder<? extends org.drugis.mtc.Measurement> createBuilder(List<? extends Study> studies, List<DrugSet> drugs, Map<Study, Map<DrugSet, Arm>> armMap) {
		for(Study s : studies){
			for (DrugSet d : drugs) {
				if(!s.getDrugs().contains(d))
					continue;
				for (Variable v : s.getOutcomeMeasures()) {
					if (!v.equals(d_outcome))
						continue;
					Arm a = armMap.get(s).get(d);
					Measurement m = s.getMeasurement(v, a);
					if(m instanceof BasicRateMeasurement) {
						BasicRateMeasurement brm = (BasicRateMeasurement)m;	
						((DichotomousNetworkBuilder) getTypedBuilder(brm)).add(s.getName(), s.getDrugs(a).getLabel(),
																			   brm.getRate(), brm.getSampleSize());
					} else if (m instanceof BasicContinuousMeasurement) {
						BasicContinuousMeasurement cm = (BasicContinuousMeasurement) m;
						((ContinuousNetworkBuilder) getTypedBuilder(cm)).add(s.getName(), s.getDrugs(a).getLabel(),
																	           cm.getMean(), cm.getStdDev(), cm.getSampleSize());
					}
				}
				
        	}
        }
		return d_builder;
	}
	
	private NetworkBuilder <? extends org.drugis.mtc.Measurement> getTypedBuilder(Measurement m) {
		if(d_builder != null)
			return d_builder;
		else if (m instanceof BasicRateMeasurement)
			return d_builder = new DichotomousNetworkBuilder();
		else if (m instanceof BasicContinuousMeasurement){ 
			d_isContinuous = true;
			return d_builder = new ContinuousNetworkBuilder();
		} else 
			throw new IllegalStateException("Unknown type of measurement: "+m);	
	}

	public String getType() {
		return "Markov Chain Monte Carlo Network Meta-Analysis";
	}

	public synchronized InconsistencyModel getInconsistencyModel() {
		if (d_inconsistencyModel == null) {
			d_inconsistencyModel = createInconsistencyModel();
		}
		return d_inconsistencyModel;
	}
	
	public synchronized ConsistencyModel getConsistencyModel() {
		if (d_consistencyModel == null) {
			d_consistencyModel = createConsistencyModel();
		}
		return d_consistencyModel;
	}

	public NetworkBuilder<? extends org.drugis.mtc.Measurement> getBuilder() {
		if (d_builder == null) {
			d_builder = createBuilder(d_studies, getIncludedDrugs(), d_armMap);
		}
		return d_builder;
	}
	
	public Network<?> getNetwork() {
		return d_builder.buildNetwork();
	}
	
	public void run() {
		List<Task> tasks = new ArrayList<Task>();
		if (!getConsistencyModel().isReady()) {
			tasks.add(getConsistencyModel().getActivityTask());
		}
		if (!getInconsistencyModel().isReady()) {
			tasks.add(getInconsistencyModel().getActivityTask());
		}
		ThreadHandler.getInstance().scheduleTasks(tasks);
	}


	public List<Parameter> getInconsistencyFactors(){
		return getInconsistencyModel().getInconsistencyFactors();
	}
	
	public QuantileSummary getQuantileSummary(MixedTreatmentComparison networkModel, Parameter ip) {
		QuantileSummary summary = d_quantileSummaries.get(networkModel).get(ip);
		if (summary == null) {
			summary = new QuantileSummary(networkModel.getResults(), ip);
			d_quantileSummaries.get(networkModel).put(ip, summary);
		}
		return summary;
	}
	
	public NormalSummary getNormalSummary(MixedTreatmentComparison networkModel, Parameter ip){
		NormalSummary summary = d_normalSummaries.get(networkModel).get(ip);
		if (summary == null) {
			summary = new NormalSummary(networkModel.getResults(), ip);
			d_normalSummaries.get(networkModel).put(ip, summary);
		}
		return summary;
	}
	
	public NodeSplitPValueSummary getNodesNodeSplitPValueSummary(Parameter p) {
		NodeSplitPValueSummary summary = d_nodeSplitPValueSummaries.get(p);
		if(summary == null) {
			NodeSplitModel m = getNodeSplitModel((BasicParameter) p);
			Parameter dir = m.getDirectEffect();
			Parameter indir = m.getIndirectEffect();
			summary = new NodeSplitPValueSummary(m.getResults(), dir, indir);
			d_nodeSplitPValueSummaries.put(p, summary);
		}
		return summary;
	}
	
	public RankProbabilitySummary getRankProbabilities() {
		if (d_rankProbabilitySummary == null) {
			d_rankProbabilitySummary = new RankProbabilitySummary(d_consistencyModel.getResults(), getTreatments());
		}
		return d_rankProbabilitySummary;
	}

	public boolean isContinuous() {
		return d_isContinuous;
	}

	public NetworkRelativeEffect<? extends Measurement> getRelativeEffect(DrugSet d1, DrugSet d2, Class<? extends RelativeEffect<?>> type) {
		
		if(!getConsistencyModel().isReady())
			return new NetworkRelativeEffect<Measurement>(); // empty relative effect.
		
		ConsistencyModel consistencyModel = getConsistencyModel();
		Parameter param = consistencyModel.getRelativeEffect(new Treatment(d1.getLabel()), new Treatment(d2.getLabel()));
		NormalSummary estimate = getNormalSummary(consistencyModel, param);
		
		if (isContinuous()) {
			return NetworkRelativeEffect.buildMeanDifference(estimate.getMean(), estimate.getStandardDeviation());
		} else {
			return NetworkRelativeEffect.buildOddsRatio(estimate.getMean(), estimate.getStandardDeviation());
		}
	}
	
	public List<Treatment> getTreatments() {
		List<Treatment> treatments = new ArrayList<Treatment>();
		for (DrugSet d : d_drugs) {
			treatments.add(getTreatment(d));
		}
		return treatments;
	}

	public Treatment getTreatment(DrugSet d) {
		return getBuilder().getTreatment(d.getLabel());
	}

	public List<BasicParameter> getSplitParameters() {
		return DefaultModelFactory.instance().getSplittableNodes(getBuilder().buildNetwork());
	}

	public NodeSplitModel getNodeSplitModel(BasicParameter p) {
		if (!d_nodeSplitModels.containsKey(p)) {
			d_nodeSplitModels.put(p, createNodeSplitModel(p));
		}
		return d_nodeSplitModels.get(p);
	}
	
	@Override
	public boolean deepEquals(Entity other) {
		if (!super.deepEquals(other)) {
			return false;
		}
		NetworkMetaAnalysis o = (NetworkMetaAnalysis) other;
		for (DrugSet d : o.getIncludedDrugs()) {
			for (Study s : o.getIncludedStudies()) {
				if (!EntityUtil.deepEqual(getArm(s, d), o.getArm(s, d))) {
					return false;
				}
			}
		}
		return true;
	}
	
}