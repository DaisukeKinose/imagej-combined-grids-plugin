package ca.phcri;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ij.IJ;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.OpenDialog;
import ij.measure.Calibration;

public class GridFromXml extends CombinedGridsPlugin {
	int[] xstartArray, ystartArray, xstartCoarseArray, ystartCoarseArray, sliceNoArray;
	private String imageName, gridDate;
	private Element imageNode;
	
	private String unitsXml;
	private int totalGridNo;
	String filePath;
	private boolean goBack;
	
	
	@Override
	public void run(String arg) {
		if (IJ.versionLessThan("1.47")) 
			return;
		imageInformation();
		
		xmlFileOpen();
		if(filePath == null)
			return;
		xmlReader();
		imageCheck();
		if(goBack)
			return;//filename, stacksize and units
		gridLayer();
		
		Grid_Switch gs = new Grid_Switch();
		gs.gridSwitch();
		showHistory(null);
	}
	
	void xmlReader(){
		//read in parameters as an Array
			
		try {
			//load common parameters
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			Document doc = builder.parse(new File(filePath));
			
			NodeList imageNodeList = doc.getElementsByTagName("image");
			
			
			imageNode = (Element) imageNodeList.item(0);
			imageName = imageNode.getAttribute("name");
			gridDate = imageNode.getAttribute("date");
			
			
			NodeList combinedgridNL = imageNode.getElementsByTagName("combinedgrid");
			if(combinedgridNL != null){
				type = getElementValueAsStr(imageNode, "type", 0);
				areaPerPoint = getElementValueAsInteger(imageNode,  "app", 0);
				unitsXml = getElementValueAsStr(imageNode, "units", 0);
				gridRatio = getElementValueAsStr(imageNode, "ratio", 0);
				
				NodeList sliceNL = imageNode.getElementsByTagName("slice");
				totalGridNo = sliceNL.getLength();
				
				sliceNoArray = new int[totalGridNo];
				xstartArray = new int[totalGridNo];
				ystartArray = new int[totalGridNo];
				xstartCoarseArray = new int[totalGridNo];
				ystartCoarseArray = new int[totalGridNo];
				
				for(int i = 0; i < sliceNL.getLength(); i++){
					Element sliceNode = (Element) sliceNL.item(i);
					String sliceNo = sliceNode.getAttribute("name");
					if(!"All".equals(sliceNo))
						sliceNoArray[i] = Integer.parseInt(sliceNo);
					xstartArray[i] = getElementValueAsInteger(sliceNode, "xstart", 0);
					ystartArray[i] = getElementValueAsInteger(sliceNode, "ystart", 0);
					xstartCoarseArray[i] = getElementValueAsInteger(sliceNode, "xstartCoarse", 0);
					ystartCoarseArray[i] = getElementValueAsInteger(sliceNode, "ystartCoarse", 0);
				}
			}
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	String getElementValueAsStr(Element e, String tag, int index){
		return e.getElementsByTagName(tag).item(index).getFirstChild().getNodeValue();
	}
	
	
	int getElementValueAsInteger(Element e, String tag, int index){
		return Integer.parseInt(getElementValueAsStr(e, tag, index));
	}
	
	
	void gridLayer(){		
		gridRoiArray = new Roi[totalGridNo];
		
		setCoarseGrids();
		calculateTile();
		
		for(int i = 0; i < totalGridNo; i++){
			xstart = xstartArray[i];
			ystart = ystartArray[i];
			xstartCoarse = xstartCoarseArray[i];
			ystartCoarse = ystartCoarseArray[i];
			int sliceNo = sliceNoArray[i];
			
			calculateNLines();
			
			ShapeRoi gridRoi = getGridRoi();
			addGridOnArray(gridRoi, sliceNo);
		}
		
		showGrid(gridRoiArray);
	}
	
	void xmlFileOpen(){
		OpenDialog.setDefaultDirectory(IJ.getDirectory("plugins"));
		OpenDialog od = new OpenDialog("Select XML file containing grid data");
		filePath = od.getPath();
	}
	
	
	void imageInformation(){
		imp = IJ.getImage();
		width = imp.getWidth();
		height = imp.getHeight();
		Calibration cal = imp.getCalibration();
		units = cal.getUnits();
		pixelWidth = cal.pixelWidth;
		pixelHeight = cal.pixelHeight;
	}
	
	void imageCheck(){
		goBack = false;
	}
	
}