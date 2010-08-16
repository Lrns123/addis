package org.drugis.addis.imports;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class PubMedIDRetriever {
	private static final String PUBMED_API = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/";

	public List<String> importPubMedID(String StudyID) {
		// First returned document is a key into the results.
		InputStream inOne = openUrl(PUBMED_API + "esearch.fcgi?db=pubmed&retmax=0&usehistory=y&term="+StudyID+"[Secondary%20Source%20ID]");
		String resultsUrl = getResultsUrl(inOne);
		
		// Second returned document contains results.
		InputStream inTwo = openUrl(PUBMED_API + resultsUrl);
		Document docTwo = parse(inTwo);
		return getIdList(docTwo);
	}
	
	private Document parse (InputStream is) {
		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setValidating(false);
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			
			Document ret = builder.parse(is);
	
			return ret;
		} catch (Exception e) {
			throw new RuntimeException("Error parsing PubMed response", e);
		}
    }

	private String getResultsUrl(InputStream inOne) {
		Document docOne = parse(inOne);
		String queryKey = getTagValue(docOne, "QueryKey");
		String webEnv = getTagValue(docOne, "WebEnv");
		String resultsUrl = "esummary.fcgi?db=pubmed&retmode=xml&query_key="+queryKey+"&WebEnv="+webEnv+"&retstart=0";
		return resultsUrl;
	}

	private String getTagValue(Document docOne, String tagName) {
		NodeList QK = docOne.getElementsByTagName(tagName);
		return QK.item(0).getFirstChild().getNodeValue();
	}

	private List<String> getIdList(Document docTwo) {	
		NodeList PID = docTwo.getElementsByTagName("Id");
		
		List<String> PubMedID = new ArrayList<String>();
		for (int i = 0; i < PID.getLength(); i++) {
			PubMedID.add(PID.item(i).getFirstChild().getNodeValue());
		}
		
		return PubMedID;
	}

	private InputStream openUrl(String urlOne) {
		try { 
			URLConnection XmlUrlOne = new URL(urlOne).openConnection();
			XmlUrlOne.setConnectTimeout(1000);
			XmlUrlOne.setReadTimeout(1500);
			InputStream inOne = XmlUrlOne.getInputStream();
			return inOne;
		} catch (Exception e) {
			throw new RuntimeException("Could not open PubMed connection", e);
		}
	}

}