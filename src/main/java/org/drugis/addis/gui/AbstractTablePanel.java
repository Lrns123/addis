package org.drugis.addis.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.drugis.addis.gui.components.EnhancedTableHeader;

@SuppressWarnings("serial")
public class AbstractTablePanel extends JPanel {

	protected TableModel d_tableModel;
	protected JPanel d_rootPanel;
	protected JTable d_table;

	public AbstractTablePanel(TableModel tableModel) {
		super();
		setMembers(tableModel);
		setRenderer();
		initComps();
	}
	
	public void setRenderer() {
		d_table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer());
		EnhancedTableHeader.autoSizeColumns(d_table);
	}
	
	private void setMembers(TableModel tableModel) {
		d_tableModel = tableModel;
		d_table = new JTable(d_tableModel);
		d_rootPanel = new JPanel(new BorderLayout());
		d_rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	}

	protected void initComps() {
		EnhancedTableHeader.autoSizeColumns(d_table);
	
		JScrollPane scroll = new JScrollPane(d_table);
		scroll.setBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createEmptyBorder(7, 7, 7, 7),
						BorderFactory.createMatteBorder(1, 1, 0, 0, Color.gray)));
		d_table.setPreferredScrollableViewportSize(new Dimension(500, d_table.getPreferredSize().height ));
		
		d_rootPanel.add(scroll, BorderLayout.CENTER);			
		this.add(d_rootPanel); 
	}

	class DefaultTableCellRenderer implements TableCellRenderer {
		public Component getTableCellRendererComponent(JTable table,
				Object val, boolean isSelected, boolean hasFocus, int row, int col) {
		
			JLabel label = new JLabel(val.toString());
			// Resize column if necessary.
			TableColumn colo = table.getColumnModel().getColumn(col);
			if ((label.getPreferredSize().width+5) > colo.getWidth()) {
				table.setVisible(false);
				colo.setMinWidth(label.getPreferredSize().width + 5);
				table.setVisible(true);
				d_table.setPreferredScrollableViewportSize(d_table.getPreferredSize());
			}
			
			return label;
		}
	}
}