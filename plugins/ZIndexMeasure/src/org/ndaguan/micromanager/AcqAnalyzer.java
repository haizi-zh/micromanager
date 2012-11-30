package org.ndaguan.micromanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

public class AcqAnalyzer extends TaggedImageAnalyzer {
	private ZIndexMeasure main;
	private myGUI myGUI_;
	private ScriptInterface mainWnd_;
	private String baseDir_;

	public void setBaseDir(String path) {
		baseDir_ = path;
	}

	private HashMap<String, BufferedWriter> dataRecorder_;

	public AcqAnalyzer(ScriptInterface gui, ZIndexMeasure main_, myGUI mygui_) {
		myGUI_ = mygui_;
		main = main_;
		mainWnd_ = gui;
		dataRecorder_ = new HashMap<String, BufferedWriter>();
	}

	public boolean clearChart_;
	private long start_ts;

	@Override
	protected void analyze(final TaggedImage taggedImage) {
		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON
				|| !main.isCalibrated)
			return;
		// myGUI_.start();
		try {
			GetPosition(myGUI_.currFrame, taggedImage);
		} catch (IOException | JSONException e) {
			mainWnd_.logError(e);
		}
		myGUI_.currFrame++;
		// myGUI_.end(String.format("%d,#JAVA Cost Time",myGUI_.currFrame));
	}

	public void GetPosition(final int index_, TaggedImage taggedImage)
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
		mainWnd_.logMessage(String.format("AcqName: %s", acqName));

		String nameComp = "";
		if (acqName.equals(MMStudioMainFrame.SIMPLE_ACQ))
			nameComp = "Live";
		else
			nameComp = acqName;

		mainWnd_.logMessage(String.format("AcqName: %s", nameComp));

		BufferedWriter writer;
		if (dataRecorder_.containsKey(nameComp))
			writer = dataRecorder_.get(nameComp);
		else {
			// Build the path
			Calendar cal = new GregorianCalendar();
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			File dir = new File(new File(baseDir_, "ZIndexMeasure"),
					dateFormat.format(cal.getTime()));
			dir.mkdirs();

			dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
			File file = new File(dir, dateFormat.format(cal.getTime()) + "_"
					+ nameComp + ".txt");

			writer = new BufferedWriter(new FileWriter(file));
			dataRecorder_.put(nameComp, writer);
			start_ts = System.nanoTime();
			writer.write("Frame, Timestamp, XPos/pixel, YPos/pixel, ZPos/uM,<StdXPos>/nM,<StdYPos>/nM,<StdZPos>/nM,meanX/pixel,meanY/pixel,meanZ/pixel,ForceX/pN,ForceY/pN\r\n");
			writer.flush();

		}

		long ts = System.nanoTime();
		double dt = (ts - start_ts) / 1e6;

		String entry = String.format(
				"%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\r\n", index_, dt,
				pos[0], pos[1], pos[2], pos[6], pos[7], pos[8], pos[9],
				pos[10], pos[11], pos[12], pos[13]);
		mainWnd_.logMessage(entry);

		writer.write(entry);
		if (index_ % myGUI_.FrameCalcForce_ == 0)
			writer.flush();

		if (index_ % myGUI_.FrameCalcForce_ == 0 && myGUI_.F_L_Flag_ == 1) {
			main.PullMagnet();
		}

	}
}