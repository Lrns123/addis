package org.drugis.addis.entities.relativeeffect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.drugis.addis.entities.Arm;
import org.drugis.addis.entities.BasicRateMeasurement;
import org.drugis.addis.entities.Drug;
import org.drugis.addis.entities.Endpoint;
import org.drugis.addis.entities.FixedDose;
import org.drugis.addis.entities.Indication;
import org.drugis.addis.entities.RateMeasurement;
import org.drugis.addis.entities.SIUnit;
import org.drugis.addis.entities.Study;
import org.drugis.addis.entities.Variable;
import org.junit.Before;
import org.junit.Test;

public class CorrectedOddsRatioTest {
	private Drug d_fluox;
	private Drug d_sertra;
	
	private Indication d_ind;
	private Endpoint d_ep;
	
	private Study d_bennie, d_boyer, d_fava, d_newhouse, d_sechter;
	
	private BasicOddsRatio d_ratioBennie, d_ratioBoyer, d_ratioFava, d_ratioNewhouse, d_ratioSechter;

	@Before
	public void setUp() {
		d_ind = new Indication(001L, "Impression");
		d_fluox = new Drug("Fluoxetine","01");
		d_sertra = new Drug("Sertraline","02");
		d_ep = new Endpoint("ep", Variable.Type.RATE);
		
		d_bennie = createStudy("Bennie 1995",0,144,73,142);
		d_boyer = createStudy("Boyer 1998", 50,120, 0,122);
		d_fava = createStudy("Fava 2002", 0, 92, 70, 96);
		d_newhouse = createStudy("Newhouse 2000", 50,119, 0,117);
		d_sechter = createStudy("Sechter 1999", 70,120, 86,118);
		d_ratioBennie = (BasicOddsRatio) RelativeEffectFactory.buildRelativeEffect(d_bennie, d_ep, d_fluox, d_sertra, BasicOddsRatio.class, true);
		d_ratioBoyer = (BasicOddsRatio) RelativeEffectFactory.buildRelativeEffect(d_boyer, d_ep, d_fluox, d_sertra, BasicOddsRatio.class, true);
		d_ratioFava = (BasicOddsRatio) RelativeEffectFactory.buildRelativeEffect(d_fava, d_ep, d_fluox, d_sertra, BasicOddsRatio.class, true);
		d_ratioNewhouse = (BasicOddsRatio) RelativeEffectFactory.buildRelativeEffect(d_newhouse, d_ep, d_fluox, d_sertra, BasicOddsRatio.class, true);
		d_ratioSechter = (BasicOddsRatio) RelativeEffectFactory.buildRelativeEffect(d_sechter, d_ep, d_fluox, d_sertra, BasicOddsRatio.class, true);

	}

	@Test
	public void testMeans() {
		assertEquals(5.722385342, d_ratioBennie.getMu(), 0.000001);
		assertEquals(-5.167618837007818, d_ratioBoyer.getMu(), 0.000001);
		assertEquals(6.198823801904371, d_ratioFava.getMu(), 0.000001);
		assertEquals(-5.140232097854726, d_ratioNewhouse.getMu(), 0.000001);
		assertEquals(0.645264951065233, d_ratioSechter.getMu(), 0.000001);
	}
	
	@Test
	public void testError() {
		// c=0.5, n2 = 144, a = 73.5, n1 = 142 -> b = 69.5, d = 144.5
		double expected = Math.sqrt(1.0/73.5 + 1.0/69.5 + 1.0/0.5 + 1.0/144.5);
		assertEquals(expected, d_ratioBennie.getError(), 0.001);
	}
	
	@Test
	public void testZeroBaselineRateShouldBeDefined() {
		RateMeasurement base = new BasicRateMeasurement(0, 100);
		RateMeasurement subj = new BasicRateMeasurement(50, 100);
		CorrectedBasicOddsRatio or = new CorrectedBasicOddsRatio(base, subj);
		assertTrue(or.isDefined());
	}
	
	@Test
	public void testZeroRateBaselineAndSubjectShouldNotBeDefined() {
		RateMeasurement base = new BasicRateMeasurement(0, 100);
		RateMeasurement subj = new BasicRateMeasurement(0, 100);
		CorrectedBasicOddsRatio or = new CorrectedBasicOddsRatio(base, subj);
		assertFalse(or.isDefined());
	}
	
	@Test
	public void testZeroSubjectRateShouldBeDefined() {
		RateMeasurement base = new BasicRateMeasurement(50, 100);
		RateMeasurement subj = new BasicRateMeasurement(0, 100);
		CorrectedBasicOddsRatio or = new CorrectedBasicOddsRatio(base, subj);
		assertTrue(or.isDefined());
	}

	@Test
	public void testFullBaselineRateShouldBeDefined() {
		RateMeasurement base = new BasicRateMeasurement(100, 100);
		RateMeasurement subj = new BasicRateMeasurement(50, 100);
		CorrectedBasicOddsRatio or = new CorrectedBasicOddsRatio(base, subj);
		assertTrue(or.isDefined());
	}
	
	@Test
	public void testFullRateBaselineAndSubjectShouldNotBeDefined() {
		RateMeasurement base = new BasicRateMeasurement(100, 100);
		RateMeasurement subj = new BasicRateMeasurement(100, 100);
		CorrectedBasicOddsRatio or = new CorrectedBasicOddsRatio(base, subj);
		assertFalse(or.isDefined());
	}
	
	@Test
	public void testFullSubjectRateShouldBeDefined() {
		RateMeasurement base = new BasicRateMeasurement(50, 100);
		RateMeasurement subj = new BasicRateMeasurement(100, 100);
		CorrectedBasicOddsRatio or = new CorrectedBasicOddsRatio(base, subj);
		assertTrue(or.isDefined());
	}
	
	@Test
	public void testUndefinedShouldResultInNaN() {
		RateMeasurement rmA1 = new BasicRateMeasurement(0, 100);
		RateMeasurement rmC1 = new BasicRateMeasurement(0, 100);
		BasicOddsRatio or = new CorrectedBasicOddsRatio(rmA1, rmC1);
		assertEquals(Double.NaN, or.getError(), 0.001);
		assertEquals(Double.NaN, or.getConfidenceInterval().getPointEstimate(), 0.001);
	}

	@Test
	public void testDefinedShouldNotResultInNaN() {
		RateMeasurement rmA1 = new BasicRateMeasurement(0, 100);
		RateMeasurement rmC1 = new BasicRateMeasurement(50, 100);
		BasicOddsRatio or = new CorrectedBasicOddsRatio(rmA1, rmC1);
		assertFalse(or.getError() == Double.NaN);
		assertFalse(Double.NaN == or.getConfidenceInterval().getPointEstimate()); 
	}

	private Study createStudy(String studyName, int fluoxResp, int fluoxSize, int sertraResp, int sertraSize)
	{
		Study s = new Study(studyName, d_ind);
		s.addEndpoint(d_ep);
		Arm g_fluox = new Arm(d_fluox, new FixedDose(10.0, SIUnit.MILLIGRAMS_A_DAY), fluoxSize);
		Arm g_parox = new Arm(d_sertra, new FixedDose(10.0, SIUnit.MILLIGRAMS_A_DAY), sertraSize);		
		
		s.addArm(g_parox);
		s.addArm(g_fluox);
		
		BasicRateMeasurement m_parox = (BasicRateMeasurement) d_ep.buildMeasurement(g_parox);
		BasicRateMeasurement m_fluox = (BasicRateMeasurement) d_ep.buildMeasurement(g_fluox);
		
		m_parox.setRate(sertraResp);
		m_fluox.setRate(fluoxResp);
		
		s.setMeasurement(d_ep, g_parox, m_parox);
		s.setMeasurement(d_ep, g_fluox, m_fluox);		
		
		return s;
	}

}
