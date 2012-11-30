package org.ndaguan.micromanager;
import java.io.IOException;

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

	public AcqAnalyzer(ScriptInterface gui,ZIndexMeasure main_, myGUI mygui_) {
		myGUI_ = mygui_;
		main = main_;
	}

	@Override
	protected void analyze(final TaggedImage taggedImage) {
		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON || !main.isCalibration)
			return;
		//myGUI_.start();
		GetPosition(myGUI_.currFrame, taggedImage);
		myGUI_.currFrame++;
		//myGUI_.end(String.format("%d,#JAVA Cost Time",myGUI_.currFrame));
	}

	public void GetPosition(final int index_, TaggedImage taggedImage) {
		Object[] dpos = main.mCalc.GetZPosition(taggedImage.pix,
				myGUI_.calcRoi_, index_);
		//double[] time = (double[]) dpos[1];
		//myGUI_.log(String.format("C Cost Time#%f", time[1]));
		final double pos[] = (double[]) dpos[0];

		SwingUtilities.invokeLater(
				new Runnable() {
					@Override
					public void run() {
						myGUI_.dataSeries_.add(index_, pos[2]);

						if (pos[11] != 0) {
							double p = Math.pow(10, 2);
							double center = Math.round(pos[11] * p) / p;
							myGUI_.chart.getXYPlot().getRangeAxis().setRange(center - 0.06,
									center + 0.06);
						}
						myGUI_.dataSeries_.add(index_, pos[2]);
						myGUI_.Msg0
						.setText(String
								.format(
										"index = %d # xpos = %f # ypos = %f # zpos = %f # forceX/pN=%f # forceY/pN=%f",
										index_, pos[0], pos[1], pos[2], pos[12],
										pos[13]));
						myGUI_.Msg1
						.setText(String
								.format(
										"# <stdx> = %f # <stdy> = %f # <stdz> = %f # meanx = %f # meany = %f # meanz = %f",
										pos[6], pos[7], pos[8], pos[9], pos[10],
										pos[11]));
						myGUI_.reSetROI((int) pos[0], (int) pos[1]);

					}}
				);

		try {
			String acqName = (String) taggedImage.tags.get("AcqName");				
			if(!acqName.equals(MMStudioMainFrame.SIMPLE_ACQ)){
				myGUI_.writer.write(String.format(
						"%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\r\n", index_, pos[0],
						pos[1], pos[2], pos[6], pos[7], pos[8], pos[9], pos[10],
						pos[11], pos[12], pos[13]));
				if (index_ % myGUI_.FrameCalcForce_ == 0)
					myGUI_.writer.flush();

				if (index_ % myGUI_.FrameCalcForce_ == 0 && myGUI_.F_L_Flag_ == 1) {
					main.PullMagnet();
				}
			}
		} catch (IOException | JSONException e) {
		}





	}
}