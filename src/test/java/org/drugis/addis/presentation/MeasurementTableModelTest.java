package org.drugis.addis.presentation;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.drugis.addis.ExampleData;
import org.drugis.addis.entities.BasicRateMeasurement;
import org.drugis.addis.entities.DomainImpl;
import org.drugis.addis.entities.Endpoint;
import org.drugis.addis.entities.OutcomeMeasure;
import org.drugis.addis.entities.Study;
import org.drugis.common.JUnitUtil;
import org.junit.Before;
import org.junit.Test;

public class MeasurementTableModelTest {

	private Study d_standardStudy;
	private PresentationModelFactory d_pmf;
	private MeasurementTableModel d_model;
	
	@Before
	public void setUp() {
		DomainImpl domain = new DomainImpl();
		ExampleData.initDefaultData(domain);
		d_pmf = new PresentationModelFactory(domain);
		d_standardStudy = ExampleData.buildStudyDeWilde();
		d_model = new MeasurementTableModel(d_standardStudy, d_pmf, Endpoint.class);
	}
		
	@Test
	public void testGetColumnCount() {
		assertEquals(d_standardStudy.getArms().size() + 1, d_model.getColumnCount());
	}

	@Test
	public void testGetRowCount() {
		assertEquals(d_standardStudy.getOutcomeMeasures().size(), d_model.getRowCount());
	}

	@Test
	public void testGetValueAt() {
		
		int index = 0;
		for (OutcomeMeasure m : d_standardStudy.getOutcomeMeasures()) {
			if (m instanceof Endpoint) {
				Endpoint e = (Endpoint) m;
				assertEquals(e.getName(), d_model.getValueAt(index, 0));
				index++;
			}
		}
		
		assertEquals(
				d_standardStudy.getMeasurement(
					d_standardStudy.getOutcomeMeasures().get(0),
					d_standardStudy.getArms().get(0)),
				d_model.getValueAt(0, 1));
	}
	
	@Test
	public void testGetColumnName() {
		assertEquals("Endpoint", d_model.getColumnName(0));
		for (int i=0;i<d_standardStudy.getArms().size();i++) {
			String exp = d_pmf.getLabeledModel(d_standardStudy.getArms().get(i)).getLabelModel().getString();
			String cname = d_model.getColumnName(i+1);
			assertEquals(exp, cname);
		}
	}
	
	@Test
	public void testIsCellEditable() {
		for (int i=0;i<d_model.getRowCount();i++) {
			for (int j=0;j<d_model.getColumnCount();j++) {
				assertFalse(d_model.isCellEditable(i, j));				
			}
		}
	}
	
	@Test 
	public void testAllMeasurementsChangesFireTableDataChanged() {
		TableModelListener mock = createMock(TableModelListener.class);
		d_model.addTableModelListener(mock);		
		mock.tableChanged((TableModelEvent)JUnitUtil.eqEventObject(new TableModelEvent(d_model)));
		expectLastCall().times(2);
		
		replay(mock);
		
		BasicRateMeasurement meas = (BasicRateMeasurement) d_standardStudy.getMeasurement(
				d_standardStudy.getOutcomeMeasures().get(0),
				d_standardStudy.getArms().get(0));
		meas.setSampleSize(667);
		meas.setRate(666);
		verify(mock);
	}	
}