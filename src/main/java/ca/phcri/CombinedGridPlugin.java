package ca.phcri;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.text.TextWindow;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * <p>
 * The Combined Grid Plugin is based on the Grid Plugin (<a target="_blank"
 * href="http://rsb.info.nih.gov/ij/plugins/grid.html"
 * >http://rsb.info.nih.gov/ij/plugins/grid.html</a>) originally written by Dr
 * Wayne Rasband and modified with his permission.
 * </p>
 * 
 * <p>
 * Implemented Functions:
 * <ol>
 * <li>Combined, or multi-purpose, point grid with several fine to coarse grid
 * ratios</li>
 * <li>Double lattice square grid with several fine to coarse grid ratios</li>
 * <li>Output of grid parameters</li>
 * <li>Manual specification of grid position</li>
 * <li>Overlay/remove a grid without destroying the other Overlay elements.</li>
 * <li>Check box to choose if you want to launch grid switch plugin
 * (installation of "Grid Switch" plugin is necessary).</li>
 * </ol>
 * </p>
 * 
 * <p>
 * Installation and Usage:
 * <ol>
 * <li>Put "CombinedGridPlugin.class" into the plugins folder of ImageJ (or
 * Fiji) and restart the software. Please note that this does
 * <strong>NOT</strong> overwrite or replace the existing Grid plugin installed
 * with ImageJ.</li>
 * <li>Open an image in ImageJ.</li>
 * <li>Choose "Grid with Combined_Grids" in the "Plugins" tab of ImageJ.</li>
 * <li>Specify your grid settings in the dialog box and click OK.</li>
 * </ol>
 * </p>
 * 
 * <p>
 * Notes:
 * <ol>
 * <li>For the Combined Point Grid and the Double Lattice Square grid,
 * "Area per Point" determines density of the fine grid.</li>
 * <li>Grid location by "Fixed Position" in this plugin is same as grid location
 * with "random-offset" unchecked in the original "Grid" plugin. For
 * "Combined Point" and "Double Lattice" grids, the first point of the coarse
 * grid is placed on the first point for the fine grid at the left upper corner.
 * </li>
 * </ol>
 * </p>
 * 
 * <p>
 * References:
 * <ol>
 * <li>Howard CV, Reed MG. Unbiased Stereology, 2nd ed. Oxon, UK: Garland
 * Science/BIOS Scientific Publishers; 2005.</li>
 * <li>Hsia CC, Hyde DM, Ochs M, Weibel ER; ATS/ERS Joint Task Force on
 * Quantitative Assessment of Lung Structure. An official research policy
 * statement of the American Thoracic Society/European Respiratory Society:
 * standards for quantitative assessment of lung structure. Am J Respir Crit
 * Care Med 2010;181:39-418.</li>
 * </ol>
 * </p>
 * 
 * <p>
 * Corresponding Author:<br/>
 * Please contact Daisuke Kinose (Daisuke.Kinose@hli.ubc.ca) for questions about
 * this plugin.
 * </p>
 * 
 * @author Daisuke Kinose
 * @author Adrian Png
 * 
 */
public class CombinedGridPlugin implements PlugIn, DialogListener {
	private final static String[] colors = { "Red", "Green", "Blue", "Magenta",
			"Cyan", "Yellow", "Orange", "Black", "White" };
	private static String color = "Blue";
	private final static int COMBINED = 0, DOUBLE_LATTICE = 1, LINES = 2,
			HLINES = 3, CROSSES = 4, POINTS = 5;
	private final static String[] types = { "Combined Point", "Double Lattice",
			"Lines", "Horizontal Lines", "Crosses", "Points" };
	private static String type = types[COMBINED];
	private static double areaPerPoint;

	private final static int ONE_TO_FOUR = 0, ONE_TO_NINE = 1,
			ONE_TO_SIXTEEN = 2, ONE_TO_TWENTYFIVE = 3, ONE_TO_THIRTYSIX = 4;
	private final static String[] ratioChoices = { "1:4", "1:9", "1:16",
			"1:25", "1:36" };
	private static String gridRatio = ratioChoices[ONE_TO_FOUR];
	private final static String[] radiobuttons = { "Random Offset",
			"Fixed Position", "Manual Input" };
	private final static int RANDOM = 0, FIXED = 1, MANUAL = 2;
	private static String radiochoice = radiobuttons[RANDOM];
	private static Component[] components; // this is to select components in
											// the dialog box
	private final static int[] ratioField = { 4, 5 };
	private final static int[] combinedGridFields = { 14, 15, 16, 17 };
	private final static int[] parameterFieldsOff = { 10, 11, 12, 13, 14, 15,
			16, 17 };
	private final static int[] xstartField = { 10, 11 };
	private final static int[] ystartField = { 12, 13 };
	private static boolean showGridSwitch = true;

	private Random random = new Random(System.currentTimeMillis());
	private ImagePlus imp;
	private double tileWidth, tileHeight;
	private int xstart, ystart;
	private int xstartCoarse, ystartCoarse, coarseGridX, coarseGridY;
	private int linesV, linesH;
	private double pixelWidth = 1.0, pixelHeight = 1.0;
	private String units;
	private String err = "";

	public void run(String arg) {
		if (IJ.versionLessThan("1.43u"))
			return;
		imp = IJ.getImage();
		showDialog();
	}

	void showGrid(Shape shape) {
		Overlay layer = imp.getOverlay();
		if (shape == null) {
			if (layer != null)
				layer.remove(layer.getIndex("grid"));
		} else {
			Roi roi = new ShapeRoi(shape);
			roi.setStrokeColor(getColor());
			roi.setName("grid");
			if (layer != null) {
				if (layer.getIndex("grid") != -1)
					layer.remove(layer.getIndex("grid"));
				layer.add(roi);
			} else
				layer = new Overlay(roi);
		}
		imp.setOverlay(layer);
	}

	// methods to form grids
	void drawPoints() {
		int one = 1;
		int two = 2;
		GeneralPath path = new GeneralPath();
		for (int h = 0; h < linesV; h++) {
			for (int v = 0; v < linesH; v++) {
				float x = (float) (xstart + h * tileWidth);
				float y = (float) (ystart + v * tileHeight);
				path.moveTo(x - two, y - one);
				path.lineTo(x - two, y + one);
				path.moveTo(x + two, y - one);
				path.lineTo(x + two, y + one);
				path.moveTo(x - one, y - two);
				path.lineTo(x + one, y - two);
				path.moveTo(x - one, y + two);
				path.lineTo(x + one, y + two);
			}
		}
		showGrid(path);
	}

	void drawCrosses() {
		GeneralPath path = new GeneralPath();
		float arm = 5;
		for (int h = 0; h < linesV; h++) {
			for (int v = 0; v < linesH; v++) {
				float x = (float) (xstart + h * tileWidth);
				float y = (float) (ystart + v * tileHeight);
				path.moveTo(x - arm, y);
				path.lineTo(x + arm, y);
				path.moveTo(x, y - arm);
				path.lineTo(x, y + arm);
			}
		}
		showGrid(path);
	}

	void drawCombined() {
		GeneralPath path = new GeneralPath();
		float arm = 5;
		float pointSizeCoarse = 10;
		float armCoarse = pointSizeCoarse / 2;

		for (int h = 0; h < linesV; h++) {
			for (int v = 0; v < linesH; v++) {
				float x = (float) (xstart + h * tileWidth);
				float y = (float) (ystart + v * tileHeight);
				path.moveTo(x - arm, y);
				path.lineTo(x + arm, y);
				path.moveTo(x, y - arm);
				path.lineTo(x, y + arm);

				if ((h % coarseGridX == 0) && (v % coarseGridY == 0)) {
					float centerX = (float) (xstart + xstartCoarse * tileWidth + h
							* tileWidth);
					float centerY = (float) (ystart + ystartCoarse * tileHeight + v
							* tileHeight);

					// drawing a coarse point by lines
					path.moveTo(centerX - pointSizeCoarse, centerY - armCoarse);
					path.lineTo(centerX - pointSizeCoarse, centerY + armCoarse);
					path.moveTo(centerX + pointSizeCoarse, centerY - 0);
					path.lineTo(centerX + pointSizeCoarse, centerY + armCoarse);
					path.moveTo(centerX - armCoarse, centerY - pointSizeCoarse);
					path.lineTo(centerX + 0, centerY - pointSizeCoarse);
					path.moveTo(centerX - armCoarse, centerY + pointSizeCoarse);
					path.lineTo(centerX + armCoarse, centerY + pointSizeCoarse);
				}
			}
		}
		showGrid(path);
	}

	void drawDoubleLattice() {
		GeneralPath path = new GeneralPath();
		int width = imp.getWidth();
		int height = imp.getHeight();
		float arm = 5;
		float rad = 14;
		float radkappa = (float) (rad * 0.5522847498); // ref
														// https://www.java.net/node/660133

		for (int i = 0; i < linesV; i++) {
			float xoff = (float) (xstart + i * tileWidth);
			path.moveTo(xoff, 0f);
			path.lineTo(xoff, height);
		}
		for (int i = 0; i < linesH; i++) {
			float yoff = (float) (ystart + i * tileHeight);
			path.moveTo(0f, yoff);
			path.lineTo(width, yoff);
		}

		for (int h = 0; h < linesV; h++) {
			for (int v = 0; v < linesH; v++) {
				if ((h % coarseGridX == 0) && (v % coarseGridY == 0)) {
					float centerX = (float) (xstart + xstartCoarse * tileWidth + h
							* tileWidth);
					float centerY = (float) (ystart + ystartCoarse * tileHeight + v
							* tileHeight);
					// drawing curve for coarse grid
					path.moveTo(centerX, centerY - rad);
					path.curveTo(centerX - radkappa, centerY - rad, centerX
							- rad, centerY - radkappa, centerX - rad, centerY);
					path.curveTo(centerX - rad, centerY + radkappa, centerX
							- radkappa, centerY + rad, centerX, centerY + rad);
					path.curveTo(centerX + radkappa, centerY + rad, centerX
							+ rad, centerY + radkappa, centerX + rad, centerY);
				}
			}
		}
		showGrid(path);
	}

	void drawLines() {
		GeneralPath path = new GeneralPath();
		int width = imp.getWidth();
		int height = imp.getHeight();
		for (int i = 0; i < linesV; i++) {
			float xoff = (float) (xstart + i * tileWidth);
			path.moveTo(xoff, 0f);
			path.lineTo(xoff, height);
		}
		for (int i = 0; i < linesH; i++) {
			float yoff = (float) (ystart + i * tileHeight);
			path.moveTo(0f, yoff);
			path.lineTo(width, yoff);
		}
		showGrid(path);
	}

	void drawHorizontalLines() {
		GeneralPath path = new GeneralPath();
		int width = imp.getWidth();
		int height = imp.getHeight();
		for (int i = 0; i < linesH; i++) {
			float yoff = (float) (ystart + i * tileHeight);
			path.moveTo(0f, yoff);
			path.lineTo(width, yoff);
		}
		showGrid(path);
	}

	// end of methods for drawing grids

	void showDialog() {
		int width = imp.getWidth();
		int height = imp.getHeight();
		Calibration cal = imp.getCalibration();
		int places;
		if (cal.scaled()) {
			pixelWidth = cal.pixelWidth;
			pixelHeight = cal.pixelHeight;
			units = cal.getUnits();
			places = 2;
		} else {
			pixelWidth = 1.0;
			pixelHeight = 1.0;
			units = "pixels";
			places = 0;
		}
		if (areaPerPoint == 0.0)
			areaPerPoint = (width * cal.pixelWidth * height * cal.pixelHeight) / 81.0; // default
																						// to
																						// 9x9
																						// grid

		// get values in a dialog box
		GenericDialog gd = new GenericDialog("Grid...");
		gd.addChoice("Grid Type:", types, type);
		gd.addNumericField("Area per Point:", areaPerPoint, places, 6, units
				+ "^2");
		gd.addChoice("Ratio:", ratioChoices, gridRatio);
		gd.addChoice("Color:", colors, color);
		gd.addRadioButtonGroup("Grid Location", radiobuttons, 3, 1,
				radiobuttons[RANDOM]);
		gd.addNumericField("xstart:", 0, 0);
		gd.addNumericField("ystart:", 0, 0);
		gd.addNumericField("xstartCoarse:", 0, 0);
		gd.addNumericField("ystartCoarse:", 0, 0);
		gd.addCheckbox("Show Grid Switch", showGridSwitch);

		// to switch enable/disable parameter input boxes
		components = gd.getComponents();
		for (int i : parameterFieldsOff)
			components[i].setEnabled(false);
		if (!(types[COMBINED].equals(type) || types[DOUBLE_LATTICE]
				.equals(type)))
			for (int i : ratioField)
				components[i].setEnabled(false);

		gd.addDialogListener(this);
		gd.showDialog();

		if (gd.wasCanceled())
			showGrid(null);
		if (gd.wasOKed()) {
			if ("".equals(err)) {
				showParameterList();
				if (showGridSwitch)
					IJ.runPlugIn("Grid_Switch", "");
			} else {
				IJ.error("Grid", err);
				showGrid(null);
			}
		}
	}

	// event control for dialog box
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		int width = imp.getWidth();
		int height = imp.getHeight();
		type = gd.getNextChoice();
		areaPerPoint = gd.getNextNumber();
		gridRatio = gd.getNextChoice();
		color = gd.getNextChoice();
		radiochoice = gd.getNextRadioButton();
		xstart = (int) gd.getNextNumber();
		ystart = (int) gd.getNextNumber();
		xstartCoarse = (int) gd.getNextNumber();
		ystartCoarse = (int) gd.getNextNumber();
		showGridSwitch = gd.getNextBoolean();
		err = "";
		IJ.showStatus(err);

		// if areaPerPoint is not too small, this shows error
		double minArea = (width * height) / 50000.0;
		if (type.equals(types[CROSSES]) && minArea < 144.0) // to avoid overlap
															// of grid points.
															// ((5 + 1) * 2) ^2
															// = 12^2 = 144
			minArea = 144.0;
		else if (type.equals(types[COMBINED]) && minArea < 484.0) // As
																	// pointSizeCoarse
																	// = 10, (10
																	// + 1) *
																	// 2)^2 =
																	// 22^2 =
																	// 484
			minArea = 484.0;
		else if (type.equals(types[DOUBLE_LATTICE]) && minArea < 900.0) // As
																		// rad =
																		// 14,
																		// ((14
																		// + 1)
																		// * 2)
																		// ^2 =
																		// 900
			minArea = 900.0;
		else if (minArea < 16)
			minArea = 16.0;

		if (Double.isNaN(areaPerPoint)
				|| areaPerPoint / (pixelWidth * pixelHeight) < minArea) {
			err = "\"Area per Point\" too small. \n";
			areaPerPoint = 0;
		}

		// choose gridRatio for Combined Points and Double Lattice
		if (type.equals(types[COMBINED]) || type.equals(types[DOUBLE_LATTICE])) {
			for (int i : ratioField)
				components[i].setEnabled(true);
		} else {
			for (int i : ratioField)
				components[i].setEnabled(false);
		}

		if (gridRatio.equals(ratioChoices[ONE_TO_FOUR])) {
			coarseGridX = 2;
			coarseGridY = 2;
		} else if (gridRatio.equals(ratioChoices[ONE_TO_NINE])) {
			coarseGridX = 3;
			coarseGridY = 3;
		} else if (gridRatio.equals(ratioChoices[ONE_TO_SIXTEEN])) {
			coarseGridX = 4;
			coarseGridY = 4;
		} else if (gridRatio.equals(ratioChoices[ONE_TO_TWENTYFIVE])) {
			coarseGridX = 5;
			coarseGridY = 5;
		} else if (gridRatio.equals(ratioChoices[ONE_TO_THIRTYSIX])) {
			coarseGridX = 6;
			coarseGridY = 6;
		}

		// calculation for tileWidth and tileLength
		double tileSize = Math.sqrt(areaPerPoint);
		tileWidth = tileSize / pixelWidth;
		tileHeight = tileSize / pixelHeight;

		// choose first point(s) depending on the way to place grid
		if (radiochoice.equals(radiobuttons[RANDOM])) {
			for (int i : parameterFieldsOff)
				components[i].setEnabled(false);
			xstart = (int) (random.nextDouble() * tileWidth);
			ystart = (int) (random.nextDouble() * tileHeight);
			// 0 <= random.nextDouble() < 1
			xstartCoarse = random.nextInt(coarseGridX);
			ystartCoarse = random.nextInt(coarseGridY);
		} else if (radiochoice.equals(radiobuttons[FIXED])) {
			for (int i : parameterFieldsOff)
				components[i].setEnabled(false);
			xstart = (int) (tileWidth / 2.0 + 0.5);
			ystart = (int) (tileHeight / 2.0 + 0.5);
			xstartCoarse = 0;
			ystartCoarse = 0;
		} else if (radiochoice.equals(radiobuttons[MANUAL])) {
			for (int i : ystartField)
				components[i].setEnabled(true);

			if (type.equals(types[HLINES])) {
				for (int i : xstartField)
					components[i].setEnabled(false);
				xstart = 0; // just to prevent an error
			} else {
				for (int i : xstartField)
					components[i].setEnabled(true);
			}

			// check if both xstart and ystart are within proper ranges
			if (areaPerPoint != 0
					&& (xstart >= tileWidth || ystart >= tileHeight)) {
				if (xstart >= tileWidth)
					err = err + "\"xstart\" ";
				if (ystart >= tileHeight)
					err = err + "\"ystart\" ";
				err = err + "too large. \n";
			}

			// input for the Combined grid
			if (type.equals(types[COMBINED])
					|| type.equals(types[DOUBLE_LATTICE])) {
				for (int i : combinedGridFields)
					components[i].setEnabled(true);

				// check if both xstartCoarse and ystartCoarse are within proper
				// ranges
				if (xstartCoarse >= coarseGridX || ystartCoarse >= coarseGridY) {
					if (xstartCoarse >= coarseGridX)
						err = err + "\"xstartCoarse\" ";
					if (ystartCoarse >= coarseGridY)
						err = err + "\"ystartCoarse\" ";
					err = err + "too large.";
				}
			} else {
				for (int i : combinedGridFields)
					components[i].setEnabled(false);
			}
		}

		if (!"".equals(err)) {
			IJ.showStatus(err);
			return true;
		}

		// calculating number of vertical and horizontal lines in a selected
		// image
		linesV = (int) ((width - xstart) / tileWidth) + 1;
		linesH = (int) ((height - ystart) / tileHeight) + 1;

		// execution part
		if (gd.invalidNumber())
			return true;
		if (type.equals(types[LINES]))
			drawLines();
		else if (type.equals(types[HLINES]))
			drawHorizontalLines();
		else if (type.equals(types[CROSSES]))
			drawCrosses();
		else if (type.equals(types[POINTS]))
			drawPoints();
		else if (type.equals(types[COMBINED]))
			drawCombined();
		else if (type.equals(types[DOUBLE_LATTICE]))
			drawDoubleLattice();
		else
			showGrid(null);
		return true;
	}

	Color getColor() {
		Color c = Color.black;
		if (color.equals(colors[0]))
			c = Color.red;
		else if (color.equals(colors[1]))
			c = Color.green;
		else if (color.equals(colors[2]))
			c = Color.blue;
		else if (color.equals(colors[3]))
			c = Color.magenta;
		else if (color.equals(colors[4]))
			c = Color.cyan;
		else if (color.equals(colors[5]))
			c = Color.yellow;
		else if (color.equals(colors[6]))
			c = Color.orange;
		else if (color.equals(colors[7]))
			c = Color.black;
		else if (color.equals(colors[8]))
			c = Color.white;
		return c;
	}

	// output grid parameters
	void showParameterList() {
		TextWindow gridParameterWindow = (TextWindow) WindowManager
				.getWindow("Grid Parameters");
		Integer xStartOutput = new Integer(xstart);
		Integer xStartCoarseOutput = new Integer(xstartCoarse);
		Integer yStartCoarseOutput = new Integer(ystartCoarse);
		String singleQuart = "'";

		if (type.equals(types[HLINES]))
			xStartOutput = null;

		if (!(type.equals(types[COMBINED]) || type
				.equals(types[DOUBLE_LATTICE]))) {
			xStartCoarseOutput = null;
			yStartCoarseOutput = null;
			singleQuart = "";
			gridRatio = null;
		}

		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();

		String GridParameters = df.format(date) + "\t" + imp.getTitle() + "\t"
				+ type + "\t" + areaPerPoint + "\t" + units + "^2" + "\t"
				+ singleQuart + gridRatio + "\t" + color + "\t" + radiochoice
				+ "\t" + xStartOutput + "\t" + (int) ystart + "\t"
				+ xStartCoarseOutput + "\t" + yStartCoarseOutput;
		// singleQuart before gridRatio is to prevent conversion to date in
		// excel.

		if (gridParameterWindow == null) {
			gridParameterWindow = new TextWindow(
					"Grid Parameters",
					"Date \t Image \t Grid Type \t Area per Point \t Unit \t Ratio \t Color \t Location Setting \t xstart \t ystart \t xstartCoarse \t ystartCoarse",
					GridParameters, 1028, 250);
		} else {
			gridParameterWindow.append(GridParameters);
		}
	}
}
