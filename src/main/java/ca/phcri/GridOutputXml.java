package ca.phcri;


import java.io.File;
import java.util.ArrayList;

import ij.IJ;
import ij.io.SaveDialog;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class GridOutputXml {
	String[] xstartArray, ystartArray, xstartCoarseArray, ystartCoarseArray;
	String[] sliceNoArray;
	static String imageName;
	String savedDate;
	String type;
	String units;
	String gridRatio;
	String color;
	String areaPerPoint;
	String location;
	static String directory = IJ.getDirectory("plugins");
	Document doc;
	Element pluginEl, combinedgridEl;
	String marginLeft;
	String marginRight;
	String marginTop;
	String marginBottom;
	String prohibitedLineColor;
	String acceptanceLineColor;
	String acceptanceLineType;
	
	GridOutputXml(String[] parameterArray){
		ArrayList<String> parameterList = new ArrayList<String>();
		
		for(String str : parameterArray){
			if(str != null)
				parameterList.add(str);
		}
				
		int totalSlice = parameterList.size();
		sliceNoArray = new String[totalSlice];
		xstartArray = new String[totalSlice];
		ystartArray = new String[totalSlice];
		xstartCoarseArray = new String[totalSlice];
		ystartCoarseArray = new String[totalSlice];
			
		for(int i = 0; i < totalSlice; i++){
			String[] parameters = parameterList.get(i).split("\t");
			savedDate = parameters[0];
			imageName = parameters[1];
			sliceNoArray[i] = parameters[2];
			type = parameters[3];
			areaPerPoint = parameters[4];
			units = parameters[5];
			gridRatio = parameters[6];
			color = parameters[7];
			location = parameters[8]; 
			xstartArray[i] = parameters[9];
			ystartArray[i] = parameters[10];
			xstartCoarseArray[i] = parameters[11];
			ystartCoarseArray[i] = parameters[12];
			
			if(parameters.length == 20){
				marginLeft = parameters[13];
				marginRight = parameters[14];
				marginTop = parameters[15];
				marginBottom = parameters[16];
				prohibitedLineColor = parameters[17];
				acceptanceLineColor = parameters[18];
				acceptanceLineType = parameters[19];
			}
		}
				
		String prefixCellCounter = "Counter Window - ";
		if(imageName.startsWith(prefixCellCounter))
			imageName = imageName.substring(17);
		
		
		
		try {
			doc = DocumentBuilderFactory
					.newInstance()
					.newDocumentBuilder()
					.newDocument();
			doc.setXmlStandalone(true);
			pluginEl = doc.createElement("imagejCombinedGridsPlugin");
			doc.appendChild(pluginEl);
			combinedgridEl = doc.createElement("combinedGrids");
			pluginEl.appendChild(combinedgridEl);
			
			Element dateEl = doc.createElement("date");
			addElementWithText(doc, dateEl, "datetime", savedDate);
			combinedgridEl.appendChild(dateEl);
			
			Element imageEl = doc.createElement("image");
			addElementWithText(doc, imageEl, "title", imageName);
			combinedgridEl.appendChild(imageEl);
			//add image as an individual node and put image name in it
			
		} catch (ParserConfigurationException exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		} catch (TransformerFactoryConfigurationError exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
		
		
		
		
		//making a "grid" node
		if(!"".equals(type)){
			if(!"null".equals(gridRatio))
				gridRatio = gridRatio.substring(1);
			
			try {
				Element gridEl = doc.createElement("grids");
				combinedgridEl.appendChild(gridEl);
				
				String[] elementNameGridEl =
					{"type", "app", "units", "ratio","color", "location"};
				String[] inputGridEl =
					{type, areaPerPoint, units, gridRatio, color, location};
				addElementWithText(doc, gridEl, elementNameGridEl, inputGridEl);
				
				
				String[] startName = 
					{"sliceNo", "xstart", "ystart", "xstartCoarse", "ystartCoarse"};
				String[][] startInput = 
					{sliceNoArray, xstartArray, ystartArray, xstartCoarseArray, ystartCoarseArray};
				
				for(int i = 0; i < totalSlice; i++){
					Element sliceEl = doc.createElement("grid");
					gridEl.appendChild(sliceEl);
					
					for(int j = 0; j < startName.length; j++){
						Element el = doc.createElement(startName[j]);
						el.appendChild(doc.createTextNode(startInput[j][i]));
						sliceEl.appendChild(el);
					}
				}
				
			} catch (TransformerFactoryConfigurationError exc) {
				// TODO Auto-generated catch block
				exc.printStackTrace();
			}
		}
		
		//making a "samplingFrame" node
		if(marginLeft != null){
			try{
				Element samplingFrameEl = doc.createElement("samplingFrame");
				combinedgridEl.appendChild(samplingFrameEl);
				
				String[] elementNameSamplingFrameEl = 
					{"left", "right", "top", "bottom", 
						"prohibitedColor", "acceptanceColor", "acceptanceType"};
				String[] inputSamlingFrameEl =
					{marginLeft, marginRight, marginTop, marginBottom, 
						prohibitedLineColor, acceptanceLineColor, acceptanceLineType};
				
				addElementWithText(doc, samplingFrameEl, elementNameSamplingFrameEl, inputSamlingFrameEl);
		
			} catch (TransformerFactoryConfigurationError exc) {
				// TODO Auto-generated catch block
				exc.printStackTrace();
			}
		}
		
	}
	
	
	void addElementWithText(Document root, Element parent, String childTag, String childText){
		String[] tags = {childTag};
		String[] texts = {childText};
		addElementWithText(root, parent, tags, texts);
	}
	
	void addElementWithText(Document root, Element parent, String[] childTags, String[] childTexts){
		for(int i = 0; i < childTags.length; i++){
			Element child = root.createElement(childTags[i]);
			child.appendChild(root.createTextNode(childTexts[i]));
			parent.appendChild(child);
		}
	}
	
	boolean save(){
		Transformer tf;
		try {
			DOMSource source = new DOMSource(doc);
			
			tf = TransformerFactory.newInstance().newTransformer();
			tf.setOutputProperty(OutputKeys.INDENT, "yes");
			
			SaveDialog sd = 
					new SaveDialog("Save parameters as XML file"
							, directory
							, "grid_" + imageName + ".xml"
							, ".xml");
			
			directory = sd.getDirectory();
			String outputFileName = sd.getFileName();
			
			if(outputFileName == null)
				return false;
			
			StreamResult result = new StreamResult(new File(directory + outputFileName));
			tf.transform(source, result);
			
		} catch (TransformerConfigurationException exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		} catch (TransformerFactoryConfigurationError exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		} catch (TransformerException exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
		
		return true;
	}
	
}