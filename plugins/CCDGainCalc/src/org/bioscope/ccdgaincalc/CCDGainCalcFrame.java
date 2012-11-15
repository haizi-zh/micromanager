package org.bioscope.ccdgaincalc;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.micromanager.api.ScriptInterface;

import mmcorej.CMMCore;

public class CCDGainCalcFrame extends JFrame {
	private CMMCore core_;
	private ScriptInterface gui_;
	private CCDGainCalc plugin_;
	private JFormattedTextField nLevelsEdt_;
	private JFormattedTextField nSamplesEdt_;
	private static CCDGainCalcFrame instance;

	boolean isCalibr = false;
	JProgressBar progress_;
	JButton calibrBtn;

	private CCDGainCalcFrame(CCDGainCalc plugin) {
		gui_ = plugin.getMMGui();
		core_ = plugin.getMMCore();
		plugin_ = plugin;
		initUI();
		instance = this;
	}

	static CCDGainCalcFrame getInstance(CCDGainCalc plugin) {
		if (instance == null)
			instance = new CCDGainCalcFrame(plugin);
		return instance;
	}

	private GridBagConstraints makeConstraints(int row, int col, int ipadx,
			int ipady, Insets insets, int anchor) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = col;
		c.gridy = row;
		c.ipadx = ipadx;
		c.ipady = ipady;
		if (insets != null)
			c.insets = insets;
		c.anchor = anchor;
		return c;
	}

	private void initUI() {
		// TODO Auto-generated method stub
		int defSamples = 100;
		int defLevels = 10;

		JPanel basic = new JPanel();
		basic.setLayout(new BoxLayout(basic, BoxLayout.Y_AXIS));
		this.getContentPane().add(basic);

		JPanel para = new JPanel(new GridBagLayout());
		Insets ist = new Insets(5, 5, 5, 5);
		JLabel nLevels = new JLabel("# of intensity levels:");
		para.add(nLevels,
				makeConstraints(0, 0, 5, 5, ist, GridBagConstraints.LINE_START));
		nLevelsEdt_ = new JFormattedTextField(new DecimalFormat());
		nLevelsEdt_.setText(String.format("%d", defLevels));
		Dimension dim = nLevelsEdt_.getPreferredSize();
		dim.setSize(32, dim.getHeight());
		nLevelsEdt_.setPreferredSize(dim);
		para.add(nLevelsEdt_,
				makeConstraints(0, 1, 5, 5, ist, GridBagConstraints.LINE_START));
		JLabel nSamplesPerLevel = new JLabel("# of samples per level:");
		para.add(nSamplesPerLevel,
				makeConstraints(1, 0, 5, 5, ist, GridBagConstraints.LINE_START));
		nSamplesEdt_ = new JFormattedTextField(new DecimalFormat());
		nSamplesEdt_.setText(String.format("%d", defSamples));
		dim = nSamplesEdt_.getPreferredSize();
		dim.setSize(32, dim.getHeight());
		nSamplesEdt_.setPreferredSize(dim);
		para.add(nSamplesEdt_,
				makeConstraints(1, 1, 5, 5, ist, GridBagConstraints.LINE_START));
		basic.add(para);
		basic.add(Box.createRigidArea(new Dimension(0, 5)));

		JPanel progBox = new JPanel();
		progBox.setLayout(new BoxLayout(progBox, BoxLayout.X_AXIS));
		progBox.add(Box.createRigidArea(new Dimension(15, 0)));
		progress_ = new JProgressBar();
		progress_.setVisible(false);
		progBox.add(progress_);
		progBox.add(Box.createRigidArea(new Dimension(15, 0)));
		basic.add(progBox);

		JPanel btnBox = new JPanel(new FlowLayout());
		btnBox.setAlignmentX(Box.LEFT_ALIGNMENT);
		calibrBtn = new JButton("Start");
		calibrBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isCalibr) {
					progress_.setVisible(false);
					calibrBtn.setText("Start");
					pack();
					plugin_.stopCalibr();
				} else {
					progress_.setVisible(true);
					calibrBtn.setText("Stop");
					pack();
					plugin_.startCalibr(new CalibrSetting(Integer
							.parseInt(nLevelsEdt_.getText()), Integer
							.parseInt(nSamplesEdt_.getText()),
							CCDGainCalcFrame.this));
				}
				isCalibr = !isCalibr;
			}
		});
		JButton closeBtn = new JButton("Close");
		closeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				setVisible(false);
			}
		});
		btnBox.add(calibrBtn);
		btnBox.add(closeBtn);
		basic.add(btnBox);
		basic.add(Box.createRigidArea(new Dimension(0, 5)));

		add(basic);
		pack();

		setTitle("CCD Gain Calc");
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setLocationRelativeTo(null);
	}
}

class CalibrSetting {
	public int nLevels;
	public int nSamples;
	public CCDGainCalcFrame frame_;

	public CalibrSetting(int l, int s, CCDGainCalcFrame frame) {
		nLevels = l;
		nSamples = s;
		frame_ = frame;
	}
}
