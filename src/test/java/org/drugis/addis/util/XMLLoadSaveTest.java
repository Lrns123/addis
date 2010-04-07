package org.drugis.addis.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javolution.xml.stream.XMLStreamException;

import org.drugis.addis.ExampleData;
import org.drugis.addis.entities.AdverseEvent;
import org.drugis.addis.entities.Arm;
import org.drugis.addis.entities.AssertEntityEquals;
import org.drugis.addis.entities.CategoricalPopulationCharacteristic;
import org.drugis.addis.entities.DomainData;
import org.drugis.addis.entities.DomainImpl;
import org.drugis.addis.entities.Drug;
import org.drugis.addis.entities.Endpoint;
import org.drugis.addis.entities.Indication;
import org.drugis.addis.entities.Note;
import org.drugis.addis.entities.Source;
import org.drugis.addis.entities.Study;
import org.drugis.addis.entities.Variable;
import org.drugis.addis.entities.OutcomeMeasure.Direction;
import org.drugis.addis.entities.Variable.Type;
import org.drugis.addis.entities.metaanalysis.MetaAnalysis;
import org.drugis.common.Interval;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class XMLLoadSaveTest {
	
	@Before
	public void setUp()  {
		
	}
	
	@Test
	public void doIndication() throws XMLStreamException {
		Indication i = ExampleData.buildIndicationDepression();
		String xml = XMLHelper.toXml(i, Indication.class);
//		System.out.println(xml);
		assertEquals(i, ((Indication)XMLHelper.fromXml(xml)));
		Indication parsedIndication = (Indication)XMLHelper.fromXml(xml);
		AssertEntityEquals.assertEntityEquals(i, parsedIndication);
		
	}
	
	@Test
	public void doEndpoint() throws XMLStreamException {
		Endpoint i = ExampleData.buildEndpointCgi();
		i.setDirection(Direction.LOWER_IS_BETTER);
		i.setType(Type.CATEGORICAL);
		String xml = XMLHelper.toXml(i, Endpoint.class);
//		System.out.println("\n"+xml+"\n");
		Endpoint objFromXml = XMLHelper.fromXml(xml);
		AssertEntityEquals.assertEntityEquals(i, objFromXml);
		i.setDirection(Direction.HIGHER_IS_BETTER);
		i.setType(Type.CONTINUOUS);
	}
	
	@Test
	public void doEndpointHamd() throws XMLStreamException {
		Endpoint i = ExampleData.buildEndpointHamd();
		String xml = XMLHelper.toXml(i, Endpoint.class);
//		System.out.println("\n"+xml+"\n");
		Endpoint objFromXml = XMLHelper.fromXml(xml);
		AssertEntityEquals.assertEntityEquals(i, objFromXml);
	}
	
	@Test
	public void doListOfEndpoints() throws XMLStreamException {
		List<Endpoint> list = new ArrayList<Endpoint>();
		
		list.add(ExampleData.buildEndpointCgi());
		list.add(ExampleData.buildEndpointHamd());
		list.add(ExampleData.buildEndpointCVdeath());
		
		XMLSet<Endpoint> xmlSet = new XMLSet<Endpoint>(list,"endpoints");
		
		String xml = XMLHelper.toXml(xmlSet,XMLSet.class);
//		System.out.println("\n"+xml+"\n");
		XMLSet<Endpoint> objFromXml = XMLHelper.fromXml(xml);
		
		assertEquals(list, objFromXml.getList());
	}
	
	@Test
	public void doAdverseEvent() throws XMLStreamException {
		AdverseEvent ade = new AdverseEvent("name", Variable.Type.RATE);
		String xml = XMLHelper.toXml(ade, AdverseEvent.class);
//		System.out.println("\n"+xml+"\n");
		AdverseEvent objFromXml = XMLHelper.fromXml(xml);
		AssertEntityEquals.assertEntityEquals(ade, objFromXml);
	}
	
	@Test
	public void doDrug() throws XMLStreamException {
		Drug d = ExampleData.buildDrugParoxetine();
		String xml = XMLHelper.toXml(d, Drug.class);
//		System.out.println(xml);
		AssertEntityEquals.assertEntityEquals(d,(Drug) XMLHelper.fromXml(xml));
		
	}	
	
	@Test
	public void doArm() throws XMLStreamException {
		Arm arm = ExampleData.buildStudyAdditionalThreeArm().getArms().get(0);
		String xml = XMLHelper.toXml(arm, Arm.class);
//		System.out.println(xml);
		Arm parsedArm = XMLHelper.fromXml(xml);
		AssertEntityEquals.assertEntityEquals(arm, parsedArm);
	}	
	
	@Test
	public void doPopulationChars() throws XMLStreamException {
		CategoricalPopulationCharacteristic gender = new CategoricalPopulationCharacteristic("Gender", new String[]{"Male", "Female"});
		String xml = XMLHelper.toXml(gender, CategoricalPopulationCharacteristic.class);
		
//		System.out.println("\n"+xml+"\n");
		
		CategoricalPopulationCharacteristic objFromXml = XMLHelper.fromXml(xml);
		assertEquals(gender, objFromXml);
	}
	
	@Test
	public void doInterval() throws XMLStreamException {
		Interval<Double> interval = new Interval<Double>(24.0,123.0);
		String xml = XMLHelper.toXml(interval, Interval.class);
		
		System.out.println("\n"+xml+"\n");
		
		Interval<Double> objFromXml = XMLHelper.fromXml(xml);
		assertEquals(interval, objFromXml);
	}
	
	@Test
	public void doStudy() throws XMLStreamException {
		Study s = ExampleData.buildStudyChouinard();
		
		
		Note note = new Note(Source.MANUAL, "this is the test text");
		s.putNote(s.getArms().get(0), note);
	
		String xml = XMLHelper.toXml(s, Study.class);
		System.out.println(xml);
//		s.addAdverseEvent(new AdverseEvent());
		
		Study parsedStudy = new Study();
		parsedStudy = (Study)XMLHelper.fromXml(xml);

		System.out.println(s.getMeasurements());
		System.out.println(parsedStudy.getMeasurements());
		
		AssertEntityEquals.assertEntityEquals(s, parsedStudy);
		//assertEquals(s.getStudyId(),parsedStudy.getStudyId());
		
		// TODO: characteristicmap
		
		assertEquals(s.getNote(s.getArms().get(0).toString()), parsedStudy.getNote(parsedStudy.getArms().get(0).toString()));
	}
	
	@Ignore
	public void doMetaAnalysis() throws XMLStreamException {
		MetaAnalysis analysis = ExampleData.buildNetworkMetaAnalysis();

		String xml = XMLHelper.toXml(analysis, MetaAnalysis.class);
		System.out.println(xml);


		assertEquals(analysis, (MetaAnalysis)XMLHelper.fromXml(xml));
	}	
	
	@Ignore
	public void doDomain() throws XMLStreamException {
		// TODO: Test whether the domains are actually equals!
		DomainImpl origDomain = new DomainImpl();
		ExampleData.initDefaultData(origDomain);
		DomainData origData = origDomain.getDomainData();
		
		origData.addVariable(new CategoricalPopulationCharacteristic("Gender", new String[]{"Male", "Female"}));
		//origData.addMetaAnalysis(ExampleData.buildNetworkMetaAnalysis()); // TODO
		
		String xml = XMLHelper.toXml(origData, DomainData.class);
		
		DomainData loadedData = XMLHelper.fromXml(xml);
		System.out.println("\n"+xml+"\n");
		assertEquals(origDomain.getIndications(), loadedData.getIndications());
		DomainImpl domainFromXml = new DomainImpl();
		
		domainFromXml.setDomainData(loadedData);
		assertEquals(origDomain, domainFromXml);
		

		// FIXME: Dates are still ignored!
	}

}
