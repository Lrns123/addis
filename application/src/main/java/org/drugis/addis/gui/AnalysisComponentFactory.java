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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.drugis.addis.FileNames;
import org.drugis.addis.entities.mtcwrapper.MCMCModelWrapper;
import org.drugis.common.gui.task.TaskProgressBar;
import org.drugis.common.threading.Task;
import org.drugis.common.threading.ThreadHandler;
import org.drugis.common.threading.activity.ActivityTask;
import org.drugis.common.threading.status.ActivityTaskInPhase;
import org.drugis.common.threading.status.TaskStartableModel;
import org.drugis.common.threading.status.TaskTerminatedModel;
import org.drugis.common.validation.BooleanAndModel;
import org.drugis.common.validation.BooleanNotModel;
import org.drugis.mtc.MCMCModel;
import org.drugis.mtc.MCMCModel.ExtendSimulation;

import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class AnalysisComponentFactory {
	
	public static JPanel createSimulationControls(
			final MCMCPresentation model, 
			final JFrame parent,
			boolean withSeparator,
			JButton ... buttons) {

		final FormLayout layout = new FormLayout(
				"pref, 3dlu, fill:0:grow, 3dlu, pref",
				"p, 3dlu, p, 3dlu, p, 3dlu, p");
		CellConstraints cc = new CellConstraints();
		PanelBuilder panelBuilder = new PanelBuilder(layout);
		int panelRow = 1;
		if (withSeparator) {
			panelBuilder.addSeparator(model.toString(), cc.xyw(1, panelRow, 5));
			panelRow += 2;
		}
		
		createProgressBarRow(model, parent, cc, panelBuilder, panelRow, true, buttons);
		panelRow += 2;
		if(!model.hasSavedResults() && hasConvergence(model)) { 
			panelBuilder.add(questionPanel(model), cc.xyw(1, panelRow, 3));
		}
		return panelBuilder.getPanel();
	}

	private static JPanel questionPanel(final MCMCPresentation model) {
		final FlowLayout flowLayout = new FlowLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		final JButton extendSimulationButton = createExtendSimulationButton(model);
		final JButton stopButton = createStopButton(model.getModel().getActivityTask(), model);
		
		ValueModel inAssessConvergence = new ActivityTaskInPhase(model.getModel().getActivityTask(), MCMCModel.ASSESS_CONVERGENCE_PHASE);
		ValueModel notTaskFinished = new BooleanNotModel(new TaskTerminatedModel(model.getModel().getActivityTask()));
		
		ValueModel shouldAssessConvergence = new BooleanAndModel(inAssessConvergence, notTaskFinished);
		
		Bindings.bind(extendSimulationButton, "enabled", shouldAssessConvergence);
		Bindings.bind(stopButton, "enabled", shouldAssessConvergence);

		
		JPanel questionPanel = new JPanel(flowLayout);
		questionPanel.add(new JLabel("Has the simulation converged?"));
		questionPanel.add(stopButton);
		questionPanel.add(extendSimulationButton);
		return questionPanel;
	}

	private static void createProgressBarRow(final MCMCPresentation model,
			JFrame main, CellConstraints cc,
			PanelBuilder panelBuilder, int panelRow, boolean hasConvergence, JButton[] buttons) {
		JButton startButton = createStartButton(model);
		
		JPanel bb = new JPanel();
		bb.add(startButton);	
		for(JButton b : buttons) { 
			bb.add(b);
		}
		panelBuilder.add(bb, cc.xy(1, panelRow));

		if(hasConvergence) { 
			panelBuilder.add(new TaskProgressBar(model.getProgressModel()), cc.xy(3, panelRow));
			panelBuilder.add(createShowConvergenceButton(main, model), cc.xy(5, panelRow));
		} else {
			panelBuilder.add(new TaskProgressBar(model.getProgressModel()), cc.xyw(3, panelRow, 3));
		}
	}

	public static JButton createStartButton(final MCMCPresentation model) {
		final JButton button = new JButton(Main.IMAGELOADER.getIcon(FileNames.ICON_RUN));
		button.setToolTipText("Run simulation");
		
		if (model.getModel() == null) {
			button.setEnabled(false);
			return button;
		}
		
		ValueModel buttonEnabledModel = new TaskStartableModel(model.getModel().getActivityTask());
		Bindings.bind(button, "enabled", buttonEnabledModel);

		final ActivityTask task = model.getModel().getActivityTask();
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ThreadHandler.getInstance().scheduleTask(task);
			}
		});
		
		return button;
	}
	

	
	public static JButton createStopButton(final Task task, final MCMCPresentation presentation) {
		final JButton button = new JButton(Main.IMAGELOADER.getIcon(FileNames.ICON_TICK));
		button.setText("Yes, finish");
		button.setToolTipText("Finish the simulation");
		
		final MCMCModelWrapper wrapper = presentation.getWrapper();
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(task.isStarted()) { 
					wrapper.getModel().setExtendSimulation(ExtendSimulation.FINISH);
				}
			}
		});
		return button;
	}	

	public static JButton createExtendSimulationButton(final MCMCPresentation presentation) {
		JButton button = new JButton(Main.IMAGELOADER.getIcon(FileNames.ICON_RESTART));
		button.setText("No, extend");
		button.setToolTipText("Extend the simulation");
		
		final MCMCModelWrapper wrapper = presentation.getWrapper();
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				wrapper.getModel().setExtendSimulation(ExtendSimulation.EXTEND);
			}
		});
		return button;
	}

	public static JButton createShowConvergenceButton(final JFrame main, final MCMCPresentation presentation) {
		JButton button = new JButton(Main.IMAGELOADER.getIcon(FileNames.ICON_CURVE_CHART));
		button.setText("Show convergence");
		final MCMCModelWrapper wrapper = presentation.getWrapper();
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog convergence = new ConvergenceSummaryDialog(main, wrapper, presentation.isModelConstructed(), presentation.toString());
				convergence.setVisible(true);
			}
		});
		return button;
	}

	private static boolean hasConvergence(MCMCPresentation model) {
		return true;
	}
}
