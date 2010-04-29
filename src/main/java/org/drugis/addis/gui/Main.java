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

package org.drugis.addis.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.drugis.addis.AppInfo;
import org.drugis.addis.FileNames;
import org.drugis.addis.entities.AdverseEvent;
import org.drugis.addis.entities.ContinuousPopulationCharacteristic;
import org.drugis.addis.entities.DependentEntitiesException;
import org.drugis.addis.entities.Domain;
import org.drugis.addis.entities.DomainEvent;
import org.drugis.addis.entities.DomainListener;
import org.drugis.addis.entities.DomainManager;
import org.drugis.addis.entities.Drug;
import org.drugis.addis.entities.Endpoint;
import org.drugis.addis.entities.Entity;
import org.drugis.addis.entities.Indication;
import org.drugis.addis.entities.PopulationCharacteristic;
import org.drugis.addis.entities.Study;
import org.drugis.addis.entities.Variable;
import org.drugis.addis.entities.metaanalysis.MetaAnalysis;
import org.drugis.addis.entities.metaanalysis.RandomEffectsMetaAnalysis;
import org.drugis.addis.gui.builder.EntitiesNodeView;
import org.drugis.addis.gui.builder.StudiesNodeView;
import org.drugis.addis.gui.builder.StudyView;
import org.drugis.addis.gui.builder.ViewFactory;
import org.drugis.addis.gui.builder.wizard.AddStudyWizard;
import org.drugis.addis.gui.components.LinkLabel;
import org.drugis.addis.gui.components.StudiesTablePanel;
import org.drugis.addis.gui.wizard.MetaAnalysisWizard;
import org.drugis.addis.gui.wizard.NetworkMetaAnalysisWizard;
import org.drugis.addis.presentation.DefaultStudyListPresentationModel;
import org.drugis.addis.presentation.PresentationModelFactory;
import org.drugis.addis.presentation.StudyPresentationModel;
import org.drugis.addis.presentation.wizard.AddStudyWizardPresentation;
import org.drugis.addis.presentation.wizard.MetaAnalysisWizardPresentation;
import org.drugis.addis.presentation.wizard.NetworkMetaAnalysisWizardPM;
import org.drugis.common.ImageLoader;
import org.drugis.common.gui.GUIHelper;
import org.drugis.common.gui.ViewBuilder;
import org.pietschy.wizard.Wizard;
import org.pietschy.wizard.WizardFrameCloser;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder2;

@SuppressWarnings("serial")
public class Main extends JFrame {
	private static final String DOMAIN_DEFAULT_FILENAME = "domain-"
			+ AppInfo.getAppVersion() + ".xml";
	private JComponent d_leftPanel;
	private JScrollPane d_rightPanel;
	private ViewBuilder d_rightPanelBuilder;

	private DomainManager d_domain;
	private DomainTreeModel d_domainTreeModel;
	private JTree d_leftPanelTree;
	private JMenuItem d_editMenuDeleteItem;

	private PresentationModelFactory d_pmManager;

	public PresentationModelFactory getPresentationModelFactory() {
		return d_pmManager;
	}

	public Main() {
		super(AppInfo.getAppName() + " v" + AppInfo.getAppVersion());
		ImageLoader.setImagepath("/org/drugis/addis/gfx/");
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt) {
				quitApplication();
			}
		});
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent arg0) {
				setRightPanelViewSize();
			}
		});

		setPreferredSize(new Dimension(1020, 764));
		GUIHelper.initializeLookAndFeel();
		UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
		ToolTipManager.sharedInstance().setInitialDelay(0);

		initializeDomain();
		d_pmManager = new PresentationModelFactory(getDomain());

	}

	protected void quitApplication() {
		try {
			saveDomainToXMLFile(DOMAIN_DEFAULT_FILENAME);
			System.exit(0);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error saving domain",
					"Error saving domain", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	private void saveDomainToXMLFile(String fileName) throws IOException {
		File f = new File(fileName);
		if (f.exists()) {
			f.delete();
		}

		FileOutputStream fos = new FileOutputStream(f);
		d_domain.saveXMLDomain(fos);
	}

	private void initializeDomain() {
		d_domain = new DomainManager();

		try {
			loadDomainFromXMLFile(DOMAIN_DEFAULT_FILENAME);
		} catch (Exception e) {
			try {
				loadDomainFromXMLResource("defaultData.xml");
			} catch (Exception e2) {
				JOptionPane.showMessageDialog(this,
						"Error loading default data: " + e2.toString(), "Error loading domain",
						JOptionPane.ERROR_MESSAGE);
			}
		}

		getDomain().addListener(new MainListener());
	}

	public Domain getDomain() {
		return d_domain.getDomain();
	}
	
	private void loadDomainFromXMLFile(String fileName) throws IOException,
	ClassNotFoundException {
		File f = new File(fileName);
		if (f.exists() && f.isFile()) {
			FileInputStream fis = new FileInputStream(f);
			d_domain.loadXMLDomain(fis);
		} else {
			throw new FileNotFoundException(fileName + " not found");
		}
	}
	
	private void loadDomainFromXMLResource(String fileName) throws IOException, ClassNotFoundException {
		InputStream fis = Main.class.getResourceAsStream("/org/drugis/addis/" + fileName);
		d_domain.loadXMLDomain(fis);
	}

	private void initMenu() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		menuBar.add(createEditMenu());
		menuBar.add(createAddMenu());
		menuBar.add(Box.createHorizontalGlue());
		menuBar.add(createHelpMenu());
		setJMenuBar(menuBar);
	}

	private JMenu createHelpMenu() {
		JMenu menu = new JMenu("Help");
		menu.setMnemonic('h');
		menu.add(createAboutItem());
		return menu;
	}

	private JMenuItem createAboutItem() {
		JMenuItem item = new JMenuItem("About");
		item.setMnemonic('a');
		item.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				showAboutDialog();
			}
		});
		return item;
	}

	private void showAboutDialog() {
		final AboutDialog dlg = new AboutDialog(this);
		GUIHelper.centerWindow(dlg, this);
		dlg.setVisible(true);
	}

	private JMenu createAddMenu() {
		JMenu addMenu = new JMenu("Add");
		addMenu.setMnemonic('a');
		addMenu.add(createAddIndicationMenuItem());
		addMenu.add(createAddDrugMenuItem());
		addMenu.add(createAddEndpointMenuItem());
		addMenu.add(createAddAdverseEventMenuItem());
		addMenu.add(createAddPopulationCharacteristicMenuItem());

		addMenu.add(createAddStudyMenuItem());
		addMenu.add(createAddMetaAnalysisMenuItem());
		addMenu.add(createAddNetworkMetaAnalysisMenuItem());
		return addMenu;
	}

	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('f');

		fileMenu.add(createNewItem());
		fileMenu.add(createImportXMLItem());
		fileMenu.add(createExportXMLItem());
		fileMenu.add(createExitItem());

		return fileMenu;
	}

	
	private JMenu createEditMenu() {
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('e');
		d_editMenuDeleteItem = createDeleteItem();
		d_editMenuDeleteItem.setEnabled(false);
		d_editMenuDeleteItem.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_DELETE, 0));
		editMenu.add(d_editMenuDeleteItem);
		return editMenu;
	}

	private JMenuItem createDeleteItem() {
		JMenuItem item = new JMenuItem("Delete", ImageLoader
				.getIcon(FileNames.ICON_DELETE));
		item.setMnemonic('d');
		item.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				deleteMenuAction();
			}
		});

		return item;
	}

	protected void deleteMenuAction() {
		Object selected = d_leftPanelTree.getSelectionPath()
				.getLastPathComponent();

		if (selected instanceof Entity) {
			deleteEntity((Entity) selected);
		}
	}

	public void deleteEntity(Entity selected) {
		String selectedType = "";
		if (selected instanceof Drug) {
			selectedType = "drug";
		} else if (selected instanceof Endpoint) {
			selectedType = "endpoint";
		} else if (selected instanceof AdverseEvent) {
			selectedType = "adverse drug event";
		} else if (selected instanceof MetaAnalysis) {
			selectedType = "meta-analysis";
		} else if (selected instanceof Study) {
			selectedType = "study";
		} else if (selected instanceof Indication) {
			selectedType = "indication";
		} else if (selected instanceof Variable)  {
			selectedType = "variable";
		}

		int conf = JOptionPane.showConfirmDialog(this,
				"Do you really want to delete " + selectedType + " " + selected
						+ " ?", "Confirm deletion", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, ImageLoader
						.getIcon(FileNames.ICON_DELETE));
		if (conf != JOptionPane.YES_OPTION) {
			return;
		}
		try {
			if (selected instanceof Drug) {
				getDomain().deleteEntity((Drug) selected);
				leftTreeFocus(d_domainTreeModel.getDrugsNode());
			} else if (selected instanceof Endpoint) {
				getDomain().deleteEntity((Endpoint) selected);
				leftTreeFocus(d_domainTreeModel.getEndpointsNode());
			} else if (selected instanceof AdverseEvent) {
				getDomain().deleteEntity((AdverseEvent) selected);
				leftTreeFocus(d_domainTreeModel.getAdverseEventsNode());
			} else if (selected instanceof PopulationCharacteristic) {
				getDomain().deleteEntity((Variable) selected);
				leftTreeFocus(d_domainTreeModel.getPopulationCharacteristicsNode());
			} else if (selected instanceof Study) {
				getDomain().deleteEntity((Study) selected);
				leftTreeFocus(d_domainTreeModel.getStudiesNode());
			} else if (selected instanceof MetaAnalysis) {
				getDomain().deleteEntity(
						(MetaAnalysis) selected);
				leftTreeFocus(d_domainTreeModel.getAnalysesNode());
			} else if (selected instanceof Indication) {
				getDomain().deleteEntity((Indication) selected);
				leftTreeFocus(d_domainTreeModel.getIndicationsNode());
			}
		} catch (DependentEntitiesException e) {
			String str = new String(selected + " is used by: ");
			for (Entity en : e.getDependents()) {
				str += "\n\t" + en;
			}
			str += "\n - delete these first.";
			JTextArea text = new JTextArea(str);
			text.setWrapStyleWord(true);
			text.setLineWrap(true);
			text.setMargin(new Insets(5, 5, 5, 5));
			JScrollPane sp = new JScrollPane(text);
			sp.setPreferredSize(new Dimension(300, 200));
			JOptionPane.showMessageDialog(this, sp, "Error deleting "
					+ selected, JOptionPane.ERROR_MESSAGE);
		}
	}

	private JMenuItem createAddEndpointMenuItem() {
		JMenuItem item = new JMenuItem("Endpoint", ImageLoader
				.getIcon(FileNames.ICON_ENDPOINT));
		item.setMnemonic('e');
		item.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				showAddEndpointDialog(null);
			}
		});

		return item;
	}

	private JMenuItem createAddAdverseEventMenuItem() {
		JMenuItem item = new JMenuItem("Adverse drug event",
				ImageLoader.getIcon(FileNames.ICON_ADVERSE_EVENT));
		item.setMnemonic('a');
		item.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				showAddAdverseEventDialog(null);
			}
		});

		return item;
	}
	
	private JMenuItem createAddPopulationCharacteristicMenuItem() {
		JMenuItem item = new JMenuItem("Population characteristic",
				ImageLoader.getIcon(FileNames.ICON_POPULATION_CHAR));
		item.setMnemonic('p');
		item.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				showAddPopulationCharacteristicDialog(null);
			}
		});

		return item;
	}

	private JMenuItem createAddStudyMenuItem() {
		JMenuItem item = new JMenuItem("Study", ImageLoader
				.getIcon(FileNames.ICON_STUDY_NEW));
		item.setMnemonic('s');
		item.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				showAddStudyWizard();
			}
		});

		return item;
	}

	private JMenuItem createAddMetaAnalysisMenuItem() {
		JMenuItem item = new JMenuItem("Meta-Analysis", ImageLoader
				.getIcon(FileNames.ICON_METASTUDY_NEW));
		item.setMnemonic('m');
		item.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				showMetaAnalysisWizard();
			}
		});

		return item;
	}
	
	private JMenuItem createAddNetworkMetaAnalysisMenuItem() {
		JMenuItem item = new JMenuItem("Network Meta-Analysis", ImageLoader
				.getIcon(FileNames.ICON_NETWMETASTUDY_NEW));
		item.setMnemonic('n');
		item.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				showNetworkMetaAnalysisWizard();
			}
		});

		return item;
	}

	private JMenuItem createAddIndicationMenuItem() {
		JMenuItem item = new JMenuItem("Indication", ImageLoader
				.getIcon(FileNames.ICON_INDICATION));
		item.setMnemonic('i');
		item.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				showAddIndicationDialog(null);
			}

		});

		return item;
	}

	private JMenuItem createAddDrugMenuItem() {
		JMenuItem item = new JMenuItem("Drug", ImageLoader
				.getIcon(FileNames.ICON_DRUG));
		item.setMnemonic('d');
		item.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				showAddDrugDialog(null);
			}

		});

		return item;
	}

	public void showAddIndicationDialog(ValueModel selectionModel) {
		AddIndicationDialog dialog = new AddIndicationDialog(this, getDomain(),
				selectionModel);
		GUIHelper.centerWindow(dialog, this);
		dialog.setVisible(true);
	}

	public void showAddEndpointDialog(ValueModel selectionModel) {
		AddVariableDialog dialog = new AddVariableDialog(this, getDomain(),
				new Endpoint("", Variable.Type.RATE), selectionModel);
		GUIHelper.centerWindow(dialog, this);
		dialog.setVisible(true);
	}

	public void showAddAdverseEventDialog(ValueModel selectionModel) {
		AddVariableDialog dialog = new AddVariableDialog(this, getDomain(),
				new AdverseEvent("", Variable.Type.RATE), selectionModel);
		GUIHelper.centerWindow(dialog, this);
		dialog.setVisible(true);
	}
	
	public void showAddPopulationCharacteristicDialog(ValueModel selectionModel) {
		AddVariableDialog dialog = new AddVariableDialog(this, getDomain(),
				new ContinuousPopulationCharacteristic(""), selectionModel);
		GUIHelper.centerWindow(dialog, this);
		dialog.setVisible(true);
	}

	private void showAddStudyWizard() {
		JDialog dialog = new JDialog((Frame) this, "Add Study", true);
		AddStudyWizard wizardBuilder = new AddStudyWizard(
				new AddStudyWizardPresentation(getDomain(),
						getPresentationModelFactory(), this), this, dialog);
		Wizard wizard = wizardBuilder.buildPanel();
		dialog.getContentPane().add(wizard);
		dialog.pack();
		WizardFrameCloser.bind(wizard, dialog);
		dialog.setVisible(true);

	}

	public void showAddDrugDialog(ValueModel selectionModel) {
		AddDrugDialog dialog = new AddDrugDialog(this, getDomain(),
				selectionModel);
		GUIHelper.centerWindow(dialog, this);
		dialog.setVisible(true);
	}
	
	private JMenuItem createNewItem() {
		JMenuItem newItem = new JMenuItem("New", ImageLoader
				.getIcon(FileNames.ICON_NEWFILE));
		newItem.setMnemonic('n');
		newItem.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				d_domain.getDomain().clearDomain();
			}
		});
		return newItem;
	}
	
	private JMenuItem createImportXMLItem() {
		JMenuItem openItem = new JMenuItem("Load XML", ImageLoader
				.getIcon(FileNames.ICON_OPENFILE));
		openItem.setMnemonic('l');
		openItem.addActionListener(new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				final JFileChooser fileChooser = new JFileChooser();
				int returnVal = fileChooser.showOpenDialog(Main.this);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						loadDomainFromXMLFile(fileChooser.getSelectedFile()
								.getAbsolutePath());
					} catch (Exception e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(Main.this,
								"Couldn't open file "
										+ fileChooser.getSelectedFile()
												.getAbsolutePath() + " .");
					}
				}
			}
		});
		return openItem;
	}
	
	private JMenuItem createExportXMLItem() {
		JMenuItem saveItem = new JMenuItem("Save XML", ImageLoader
				.getIcon(FileNames.ICON_SAVEFILE));
		saveItem.setMnemonic('s');
		saveItem.addActionListener(new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				final JFileChooser fileChooser = new JFileChooser();
				int returnVal = fileChooser.showSaveDialog(Main.this);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						saveDomainToXMLFile(fileChooser.getSelectedFile()
								.getAbsolutePath());
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(Main.this,
								"Couldn't save file "
										+ fileChooser.getSelectedFile()
												.getAbsolutePath() + " .");
						e1.printStackTrace();
					}
				}
			}
		});
		return saveItem;
	}

	private JMenuItem createExitItem() {
		JMenuItem exitItem = new JMenuItem("Exit", ImageLoader
				.getIcon(FileNames.ICON_STOP));
		exitItem.setMnemonic('e');
		exitItem.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				quitApplication();
			}
		});
		return exitItem;
	}

	public void initComponents() {
		initMenu();
		initPanel();
		initToolbar();
	}

	private void initToolbar() {
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setLayout(new BorderLayout());

		JButton topAddStudyButton = new JButton("Add study", ImageLoader
				.getIcon(FileNames.ICON_STUDY_NEW));
		topAddStudyButton.setToolTipText("Add study");
		topAddStudyButton.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				showAddStudyWizard();
			}
		});

		JButton topAddMetaStudyButton = new JButton("Create meta-analysis",
				ImageLoader.getIcon(FileNames.ICON_METASTUDY_NEW));
		topAddMetaStudyButton.setToolTipText("Create meta-analysis");
		topAddMetaStudyButton.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				showMetaAnalysisWizard();
			}
		});
		
		JButton topAddNetworkMetaStudyButton = new JButton("Create network meta-analysis",
				ImageLoader.getIcon(FileNames.ICON_NETWMETASTUDY_NEW));
		topAddNetworkMetaStudyButton.setToolTipText("Create network meta-analysis");
		topAddNetworkMetaStudyButton.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				showNetworkMetaAnalysisWizard();
			}
		});		

		ButtonBarBuilder2 builder = new ButtonBarBuilder2();
		builder.addButton(topAddStudyButton);
		builder.addButton(topAddMetaStudyButton);
		builder.addButton(topAddNetworkMetaStudyButton);
		builder.addGlue();

		String latestVersion = AppInfo.getLatestVersion();
		if (latestVersion != null) {
			LinkLabel linkLabel = new LinkLabel(
					"<font color=\"red\">new version available</font>",
					"http://drugis.org/files/addis-" + latestVersion + ".zip");
			linkLabel.setForeground(Color.RED);
			builder.addButton(linkLabel);
			builder.addRelatedGap();
		}
		builder.addButton(GUIFactory.buildSiteLink());

		toolbar.add(builder.getPanel(), BorderLayout.CENTER);
		toolbar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(toolbar, BorderLayout.NORTH);
	}

	private void showMetaAnalysisWizard() {
		MetaAnalysisWizard wizard = new MetaAnalysisWizard(this,
				new MetaAnalysisWizardPresentation(getDomain(), d_pmManager));
		wizard.showInDialog("Create DerSimonian-Laird random effects meta-analysis", this,	true);
	}
	
	private void showNetworkMetaAnalysisWizard() {
		NetworkMetaAnalysisWizard wizard = new NetworkMetaAnalysisWizard(this,
				new NetworkMetaAnalysisWizardPM(getDomain(), d_pmManager));
		wizard.showInDialog("Create network meta-analysis", this, true);
	}	

	private void initPanel() {
		JSplitPane pane = new JSplitPane();
		pane.setBorder(BorderFactory.createEmptyBorder());
		pane.setEnabled(true);
		pane.setOneTouchExpandable(true);

		initLeftPanel();
		pane.setLeftComponent(d_leftPanel);

		initRightPanel();
		pane.setRightComponent(d_rightPanel);
		
		add(pane);
	}
	
	public JScrollPane getRightPanel(){
		return d_rightPanel;
	}

	private void initLeftPanel() {
		d_domainTreeModel = new DomainTreeModel(getDomain());
		d_leftPanelTree = new JTree(d_domainTreeModel);
		d_leftPanelTree.setCellRenderer(new DomainTreeCellRenderer());
		d_leftPanelTree.setRootVisible(false);
		expandLeftPanelTree();

		d_leftPanelTree.addTreeSelectionListener(createSelectionListener());
		d_domainTreeModel.addTreeModelListener(new TreeModelListener() {
			public void treeNodesChanged(TreeModelEvent arg0) {
			}

			public void treeNodesInserted(TreeModelEvent arg0) {
			}

			public void treeNodesRemoved(TreeModelEvent arg0) {
			}

			public void treeStructureChanged(TreeModelEvent arg0) {
				expandLeftPanelTree();
			}
		});

		d_leftPanel = new JScrollPane(d_leftPanelTree);
	}

	private void expandLeftPanelTree() {
		d_leftPanelTree.expandPath(new TreePath(new Object[] {
				d_domainTreeModel.getRoot(),
				d_domainTreeModel.getIndicationsNode() }));
		d_leftPanelTree.expandPath(new TreePath(new Object[] {
				d_domainTreeModel.getRoot(),
				d_domainTreeModel.getEndpointsNode() }));
		d_leftPanelTree.expandPath(new TreePath(new Object[] {
						d_domainTreeModel.getRoot(),
						d_domainTreeModel.getDrugsNode() }));
		d_leftPanelTree.expandPath(new TreePath(new Object[] {
				d_domainTreeModel.getRoot(),
				d_domainTreeModel.getAnalysesNode() }));
		d_leftPanelTree.expandPath(new TreePath(new Object[] {
				d_domainTreeModel.getRoot(),
				d_domainTreeModel.getAdverseEventsNode() }));
		d_leftPanelTree.expandPath(new TreePath(new Object[] {
				d_domainTreeModel.getRoot(),
				d_domainTreeModel.getPopulationCharacteristicsNode() }));
	}

	private TreeSelectionListener createSelectionListener() {
		return new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent event) {
				Object node = ((JTree) event.getSource())
						.getLastSelectedPathComponent();
				if (node == null) {
					noneSelected();
				} else if (node instanceof Entity) {
					entitySelected((Entity)node);
				} else if (node == d_domainTreeModel.getStudiesNode()) {
					studyLabelSelected();
				} else if (node == d_domainTreeModel.getDrugsNode()) {
					drugLabelSelected();
				} else if (node == d_domainTreeModel.getIndicationsNode()) {
					indicationLabelSelected();
				} else if (node == d_domainTreeModel.getEndpointsNode()) {
					endpointLabelSelected();
				} else if (node == d_domainTreeModel.getAdverseEventsNode()) {
					adverseEventLabelSelected();
				} else if (node == d_domainTreeModel.getPopulationCharacteristicsNode()) {
					populationCharacteristicsLabelSelected();
				} else if (node == d_domainTreeModel.getAnalysesNode()) {
					analysesLabelSelected();
				} else {
					noneSelected();
				}
			}
		};
	}
	
	private void entitySelected(Entity node) {
		ViewBuilder view = ViewFactory.createView(node, d_pmManager, this);
		setRightPanelView(view);
		d_editMenuDeleteItem.setEnabled(true);
	}

	private void noneSelected() {
		setRightPanelView(new ViewBuilder() {
			public JComponent buildPanel() {
				return new JPanel();
			}
		});
	}

	private void drugLabelSelected() {
		String [] properties = {"name", "atcCode"};
		buildEntityTable(getDomain().getDrugs(), properties, "Drugs");
	}

	private void endpointLabelSelected() {
		String [] properties = {"name", "description", "unitOfMeasurement",
				"type", "direction"};
		buildEntityTable(getDomain().getEndpoints(), properties, "Endpoints");
	}

	private void populationCharacteristicsLabelSelected() {
		String [] properties = {"name", "description", "unitOfMeasurement", "type"};
		buildEntityTable(getDomain().getVariables(), properties, "Population characteristics");
	}
	
	private void adverseEventLabelSelected() {
		String [] properties = {"name", "description", "unitOfMeasurement",
				"type", "direction"};
		buildEntityTable(getDomain().getAdverseEvents(), properties,
				"Adverse drug events");
	}
	
	private <T extends Entity> void buildEntityTable(SortedSet<T> allX,
			String[] formatter, String title) {
		List<PresentationModel<T>> dpms = new ArrayList<PresentationModel<T>>();
		for (T i : allX) {
			dpms.add(d_pmManager.getModel(i));
		}
		EntitiesNodeView<T> view = 
			new EntitiesNodeView<T>(Arrays.asList(formatter), dpms, this, title);
		setRightPanelView(view);
	}

	private void indicationLabelSelected() {
		String [] properties = {"name", "code"};
		buildEntityTable(getDomain().getIndications(), properties, "Indications");
	}

	private void studyLabelSelected() {
		DefaultStudyListPresentationModel studyListPM =
			new DefaultStudyListPresentationModel(getDomain().getStudiesHolder());
		StudiesNodeView view = new StudiesNodeView(new StudiesTablePanel(studyListPM, this));
		setRightPanelView(view);
	}

	private void analysesLabelSelected() {
		String [] properties = {"name", "type", "indication", "outcomeMeasure", 
				"includedDrugs", "studiesIncluded", "sampleSize"};
		buildEntityTable(getDomain().getMetaAnalyses(), properties,
				"Meta-Analyses");
	}

	private void setRightPanelView(ViewBuilder view) {
		d_rightPanelBuilder = view;
		setRightPanelContents(view.buildPanel());
		d_editMenuDeleteItem.setEnabled(true);
	}

	private void initRightPanel() {
		JPanel panel = new JPanel();
		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		d_rightPanel = scrollPane;
	}

	public static void main(String[] args) {
		Main frame = new Main();
		frame.initComponents();
		frame.pack();
		frame.setVisible(true);
	}

	private void dataModelChanged() {
		reloadRightPanel();
	}
	
	public void repaintRightPanel() {
		//d_rightPanel.doLayout();
		//d_rightPanel.repaint();
		d_rightPanel.setVisible(false);
		d_rightPanel.setVisible(true);
		d_rightPanel.revalidate();
	}
	
	public void reloadRightPanel() {
		if (d_rightPanelBuilder != null) {
			setRightPanelContents(d_rightPanelBuilder.buildPanel());
		}
	}

	private void setRightPanelContents(JComponent component) {
		d_rightPanel.setViewportView(component);

		setRightPanelViewSize();
	}

	private void setRightPanelViewSize() {
		JComponent view = (JComponent) d_rightPanel.getViewport().getView();
		Dimension dimension = new Dimension();
		int prefWidth = getSize().width - d_leftPanel.getPreferredSize().width - 40;
		dimension.width = Math.max(prefWidth, view.getMinimumSize().width);
		dimension.height = view.getPreferredSize().height;
		view.setPreferredSize(dimension);
	}

	private class MainListener implements DomainListener {
		public void domainChanged(DomainEvent evt) {
			dataModelChanged();
		}
	}

	public void leftTreeFocus(Object node) {
		if (d_domainTreeModel
				.getIndexOfChild(d_domainTreeModel.getRoot(), node) != -1) {
			d_leftPanelTree.setSelectionPath(new TreePath(new Object[] {
					d_domainTreeModel.getRoot(), node }));
		} else if (node instanceof Indication) {
			d_leftPanelTree.setSelectionPath(new TreePath(new Object[] {
					d_domainTreeModel.getRoot(),
					d_domainTreeModel.getIndicationsNode(), node }));
		} else if (node instanceof Drug) {
			d_leftPanelTree.setSelectionPath(new TreePath(new Object[] {
					d_domainTreeModel.getRoot(),
					d_domainTreeModel.getDrugsNode(), node }));
		} else if (node instanceof Endpoint) {
			d_leftPanelTree.setSelectionPath(new TreePath(new Object[] {
					d_domainTreeModel.getRoot(),
					d_domainTreeModel.getEndpointsNode(), node }));
		} else if (node instanceof AdverseEvent) {
			d_leftPanelTree.setSelectionPath(new TreePath(new Object[] {
					d_domainTreeModel.getRoot(),
					d_domainTreeModel.getAdverseEventsNode(), node }));
		} else if (node instanceof Variable) {
			d_leftPanelTree.setSelectionPath(new TreePath(new Object[] {
					d_domainTreeModel.getRoot(),
					d_domainTreeModel.getPopulationCharacteristicsNode(), node }));
		} else if (node instanceof Study) {
			d_leftPanelTree.setSelectionPath(null);
			StudyView view = new StudyView((StudyPresentationModel) d_pmManager
					.getModel(((Study) node)), getDomain(), this);
			setRightPanelView(view);
		} else if (node instanceof RandomEffectsMetaAnalysis) {
			d_leftPanelTree.setSelectionPath(new TreePath(new Object[] {
					d_domainTreeModel.getRoot(),
					d_domainTreeModel.getAnalysesNode(), node }));
		}
	}
}
