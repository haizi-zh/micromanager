import java.io.IOException;

import javax.swing.SwingUtilities;

import mmcorej.TaggedImage;

import org.jfree.chart.axis.NumberAxis;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.TaggedImageAnalyzer;

public class AcqAnalyzer extends TaggedImageAnalyzer {
	private ZIndexMeasure main;
	private myGUI myGUI_;

	public AcqAnalyzer(ScriptInterface gui) {
	}

	@Override
	protected void analyze(final TaggedImage taggedImage) {
		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON)
			return;

		myGUI_ = myGUI.getInstance();
		main = ZIndexMeasure.getInstance();
		myGUI_.start();
		GetPosition(myGUI_.currFrame, taggedImage);
		myGUI_.currFrame++;
		myGUI_.end(String.format("%d,#JAVA Cost Time", myGUI_.currFrame));
	}

	public void GetPosition(final int index_, TaggedImage taggedImage) {
		Object[] dpos = main.mCalc.GetZPosition(taggedImage.pix,
				myGUI_.calcRoi_, index_);
		double[] time = (double[]) dpos[1];
		final double pos[] = (double[]) dpos[0];

		myGUI_.log(String.format("C Cost Time#%f", time[1]));

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				myGUI_.dataSeries_.add(index_, pos[2]);

				if (pos[11] != 0) {
					double p = Math.pow(10, 2);
					double center = Math.round(pos[11] * p) / p;
					myGUI_.chart.getXYPlot().getRangeAxis()
							.setRange(center - 0.06, center + 0.06);
				}
				myGUI_.dataSeries_.add(index_, pos[2]);
				myGUI_.Msg0.setText(String
						.format("index = %d # xpos = %.1f # ypos = %.1f # zpos = %.1f # forceX/pN=%.2f # forceY/pN=%.2f",
								index_, pos[0], pos[1], pos[2], pos[12],
								pos[13]));
				myGUI_.Msg1.setText(String
						.format("# <stdx> = %.1f # <stdy> = %.1f # <stdz> = %.4f # meanx = %.2f # meany = %.2f # meanz = %.5f",
								pos[6], pos[7], pos[8], pos[9], pos[10],
								pos[11]));
				myGUI_.reSetROI((int) pos[0], (int) pos[1]);

			}
		});

		try {
			myGUI_.writer.write(String.format(
					"%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\r\n", index_, pos[0],
					pos[1], pos[2], pos[6], pos[7], pos[8], pos[9], pos[10],
					pos[11], pos[12], pos[13]));
			if (index_ % myGUI_.FrameCalcForce_ == 0)
				myGUI_.writer.flush();
		} catch (IOException e) {
		}

		if (index_ % myGUI_.FrameCalcForce_ == 0 && myGUI_.F_L_Flag_ == 1) {
			main.PullMagnet();
		}
	}
}