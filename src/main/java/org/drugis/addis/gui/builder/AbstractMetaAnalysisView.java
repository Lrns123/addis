/*
 * This file is part of ADDIS (Aggregate Data Drug Information System).
 * ADDIS is distributed from http://drugis.org/.
 * Copyright (C) 2009  Gert van Valkenhoef and Tommi Tervonen.
 * Copyright (C) 2010  Gert van Valkenhoef, Tommi Tervonen, Tijs Zwinkels,
 * Maarten Jacobs and Hanno Koeslag.
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

package org.drugis.addis.gui.builder;

import javax.swing.JComponent;
import javax.swing.JTextPane;

import org.drugis.addis.entities.analysis.MetaAnalysis;
import org.drugis.addis.entities.analysis.NetworkMetaAnalysis;
import org.drugis.addis.entities.analysis.RandomEffectsMetaAnalysis;
import org.drugis.addis.gui.GUIFactory;
import org.drugis.addis.gui.Main;
import org.drugis.addis.presentation.AbstractMetaAnalysisPresentation;
import org.drugis.addis.util.HtmlWordWrapper;
import org.drugis.common.gui.OneWayObjectFormat;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class AbstractMetaAnalysisView<T extends AbstractMetaAnalysisPresentation<?>> {

	protected T d_pm;
	protected Main d_parent;

	public AbstractMetaAnalysisView(T model, Main main) {
		d_pm = model;
		d_parent = main;
	}

	protected JComponent buildStudiesPart() {
		FormLayout layout = new FormLayout("pref:grow:fill","p");
		
		PanelBuilder builder = new PanelBuilder(layout);
		CellConstraints cc =  new CellConstraints();
				
		builder.add(GUIFactory.buildStudyPanel(d_pm, d_parent), cc.xy(1, 1));
		
		return builder.getPanel();
	}

	protected JComponent buildOverviewPart() {
		FormLayout layout = new FormLayout(
				"pref, 3dlu, pref:grow:fill",
				"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
		
		PanelBuilder builder = new PanelBuilder(layout);
		CellConstraints cc =  new CellConstraints();
		
		builder.addLabel("ID:", cc.xy(1, 1));
		builder.add(BasicComponentFactory.createLabel(d_pm.getModel(RandomEffectsMetaAnalysis.PROPERTY_NAME)),
				cc.xy(3, 1));
		
		builder.addLabel("Type:", cc.xy(1, 3));
		builder.add(BasicComponentFactory.createLabel(d_pm.getModel(MetaAnalysis.PROPERTY_TYPE), new OneWayObjectFormat()), cc.xy(3, 3));
				
		builder.addLabel("Indication:", cc.xy(1, 5));
		builder.add(BasicComponentFactory.createLabel(d_pm.getIndicationModel().getLabelModel()),
				cc.xy(3, 5));
	
		builder.addLabel("Endpoint:", cc.xy(1, 7));
		builder.add(BasicComponentFactory.createLabel(d_pm.getOutcomeMeasureModel().getLabelModel()),
				cc.xy(3, 7));
		
		builder.addLabel("Included drugs:", cc.xy(1, 9));
		builder.add(BasicComponentFactory.createLabel(d_pm.getModel(MetaAnalysis.PROPERTY_INCLUDED_DRUGS), new OneWayObjectFormat()),
				cc.xy(3, 9));

		if(d_pm.getBean() instanceof NetworkMetaAnalysis){
			String paneText =  "<html>Network Meta-Analysis (or Mixed Treatment Comparison, MTC) is a technique to meta-analyze more than <br>two drugs at the same time. Using a full Bayesian evidence network, all indirect comparisons are taken <br>into account to arrive at a single, integrated, estimate of the effect of all included treatments based on all <br>included studies. This software is meant as a demonstration of the sort of analysis enabled by MTCs. <br><br>The functionality provided is not yet sufficient to do a full MTC analysis, as concerns such as the assessment <br>of convergence of the Bayesian model and the adequacy of the run-length cannot be addressed. If you are <br>interested in doing such an analysis, you can find more information at <a href=http://drugis.org/mtc>http://drugis.org/mtc</a>, <br>or contact Gert van Valkenhoef, the corresponding author.</html>";
			JTextPane generalPane = HtmlWordWrapper.createHtmlPane(paneText);
			
			builder.add(generalPane, cc.xyw(1, 11, 3));
		}
		
		return builder.getPanel();
	}

}
