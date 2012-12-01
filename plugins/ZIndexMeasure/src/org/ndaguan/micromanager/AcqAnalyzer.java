package org.ndaguan.micromanager;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import mmcorej.TaggedImage;

import org.json.JSONException;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.utils.MMScriptException;
import org.zephyre.micromanager.OverlayRender;
import org.zephyre.micromanager.OverlayRender.RenderItem;

public class AcqAnalyzer extends TaggedImageAnalyzer {
	private ZIndexMeasure main;
	private myGUI myGUI_;
	private ScriptInterface mainWnd_;
	private String baseDir_;
	private static AcqAnalyzer instance_;

	public void setBaseDir(String path) {
		baseDir_ = path;
	}

	private Writer dataFileWriter_;

	protected AcqAnalyzer(ScriptInterface gui, ZIndexMeasure main_, myGUI mygui_) {
		myGUI_ = mygui_;
		main = main_;
		mainWnd_ = gui;
		render_ = OverlayRender.getInstance(gui);
	}

	public static AcqAnalyzer getInstance(ScriptInterface gui, ZIndexMeasure main_,
			myGUI mygui_) {
		if (instance_ == null)
			instance_ = new AcqAnalyzer(gui, main_, mygui_);
		return instance_;
	}

	public boolean clearChart_;
	private OverlayRender render_;

	@Override
	protected void analyze(final TaggedImage taggedImage) {
		// Retrieving a POISON image indicates that current acquisition is
		// completed or has been canceled.
		if (taggedImage == TaggedImageQueue.POISON) {
			if (dataFileWriter_ != null) {
				try {
					dataFileWriter_.close();
					dataFileWriter_ = null;
					clearChart_ = true;
				} catch (IOException e) {
					mainWnd_.logError(e);
				}
			}
		}

		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON
				|| !main.isCalibrated)
			return;

		// myGUI_.start();
		double pos[] = null;
		try {
			pos = GetPosition(myGUI_.currFrame, taggedImage);
		} catch (IOException | JSONException e) {
			mainWnd_.logError(e);
			return;
		}
		myGUI_.currFrame++;
		// myGUI_.end(String.format("%d,#JAVA Cost Time",myGUI_.currFrame));

		// Render the overlay
		String acqName;
		long index;
		try {
			if (!taggedImage.tags.has("FrameIndex"))
				index = 0;
			else
				index = taggedImage.tags.getLong("FrameIndex");
			acqName = taggedImage.tags.getString("AcqName");
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}

		ArrayList<RenderItem> list = new ArrayList<OverlayRender.RenderItem>();
		list.add(RenderItem.createInstance(new Point2D.Float((float) pos[0],
				(float) pos[1]), String.format("(%f, %f, %f)", pos[0], pos[1],
				pos[2])));
		boolean update = acqName.equals(MMStudioMainFrame.SIMPLE_ACQ) ? true
				: false;
		try {
			render_.render(acqName, list, index, update);
		} catch (MMScriptException e) {
		}
	}

	public double[] GetPosition(final int index_, TaggedImage taggedImage)
			throws IOException, JSONException {
		Object[] dpos = main.mCalc.GetZPosition(taggedImage.pix,
				myGUI_.calcRoi_, index_);
		// double[] time = (double[]) dpos[1];
		// myGUI_.log(String.format("C Cost Time#%f", time[1]));
		final double pos[] = (double[]) dpos[0];

		final boolean clr = clearChart_;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (clr) {
					myGUI_.dataSeries_.clear();
					AcqAnalyzer.this.clearChart_ = false;
				}

				myGUI_.dataSeries_.add(index_, pos[2]);

				if (pos[11] != 0) {
					double p = Math.pow(10, 2);
					double center = Math.round(pos[11] * p) / p;
					myGUI_.chart.getXYPlot().getRangeAxis()
							.setRange(center - 0.06, center + 0.06);
				}
				myGUI_.dataSeries_.add(index_, pos[2]);
				myGUI_.Msg0.setText(String
						.format("index = %d # xpos = %f # ypos = %f # zpos = %f # forceX/pN=%f # forceY/pN=%f",
								index_, pos[0], pos[1], pos[2], pos[12],
								pos[13]));
				myGUI_.Msg1.setText(String
						.format("# <stdx> = %f # <stdy> = %f # <stdz> = %f # meanx = %f # meany = %f # meanz = %f",
								pos[6], pos[7], pos[8], pos[9], pos[10],
								pos[11]));
				myGUI_.reSetROI((int) pos[0], (int) pos[1]);

			}
		});

		String acqName = (String) taggedImage.tags.get("AcqName");
		String nameComp = "";
		if (acqName.equals(MMStudioMainFrame.SIMPLE_ACQ))
			nameComp = "Live";
		else
			nameComp = acqName;

		if (dataFileWriter_ == null) {
			// Build the path
			Calendar cal = new GregorianCalendar();
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			File dir = new File(new File(baseDir_, "ZIndexMeasure"),
					dateFormat.format(cal.getTime()));
			dir.mkdirs();

			dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
			File file = new File(dir, dateFormat.format(cal.getTime()) + "_"
					+ nameComp + ".txt");
			dataFileWriter_ = new BufferedWriter(new FileWriter(file));
			dataFileWriter_
					.write("Frame, Timestamp, XPos/pixel, YPos/pixel, ZPos/uM,<StdXPos>/nM,<StdYPos>/nM,<StdZPos>/nM,meanX/pixel,meanY/pixel,meanZ/pixel,ForceX/pN,ForceY/pN\r\n");
		}

		dataFileWriter_.write(String.format(
				"%d,%s,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\r\n", index_,
				taggedImage.tags.get("ElapsedTime-ms"), pos[0], pos[1], pos[2],
				pos[6], pos[7], pos[8], pos[9], pos[10], pos[11], pos[12],
				pos[13]));

		if (index_ % myGUI_.FrameCalcForce_ == 0 && myGUI_.F_L_Flag_ == 1) {
			main.PullMagnet();
		}

		return pos;
	}
}