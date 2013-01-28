package org.ndaguan.micromanager;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class PreferDailog {
	public JFrame frame ;
	final private int ITEMWIDTH = 100;
	final private int ITEMHEIGHT = 20;

	private JTextField BeanRadius;
	private JTextField DNALength;
	private JComboBox<String> ZCalScale;
	private JTextField ZCalStep;
	private JTextField RinterStep;
	private JTextField HalfCorrWin;
	private JTextField MagnetStep;
	private JTextField FrameToAcq;
	private JTextField FrameToCalcForce;
	private JTextField BeanRadiuPixel;
	private JTextField Preservation1;
	private JTextField Preservation2;


	private ActionListener DialogListener;
	private ZIndexMeasureFrame myform_;
	private Preferences Preferences_;
	private Function function_;

	public PreferDailog(Function function) {
		Preferences_ = Preferences.getInstance();
		function_  = function;
		initialize();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				UpdateData(false);//GUI  update
			}
		});

	}

	private void UpdateData(boolean flag) {
		if(flag){//flush
			Preferences_.beanRadius_ = Double.parseDouble(BeanRadius.getText());
			Preferences_.contourLength_ = Double.parseDouble(DNALength.getText());
			Preferences_.zCalRange_ = (double)ZCalScale.getSelectedIndex()+2;
			Preferences_.zCalStep_ = Double.parseDouble(ZCalStep.getText());
			Preferences_.rInterpStep_ = Double.parseDouble(RinterStep.getText());
			Preferences_.halfQuadWindow_ = Double.parseDouble(HalfCorrWin.getText());
			Preferences_.magnetMoveStep_ = Double.parseDouble(MagnetStep.getText());
			Preferences_.frameToAcq_ = Integer.parseInt(FrameToAcq.getText());
			Preferences_.frameToCalcForce_ = Integer.parseInt(FrameToCalcForce.getText());
			Preferences_.beanRadiusPiexl_ = Double.parseDouble(BeanRadiuPixel.getText());
			Preferences_.preservation1_ = Double.parseDouble(Preservation1.getText());
			Preferences_.preservation2_ = Double.parseDouble(Preservation2.getText());
			Preferences_.saveUserData();
			function_.onDataChange(Preferences_);
		}
		else{//refresh GUI
			BeanRadius.setText(String.format("%.2f",Preferences_.beanRadius_));
			DNALength.setText(String.format("%.2f",Preferences_.contourLength_));
			ZCalScale.setSelectedIndex((int)(Preferences_.zCalRange_/1000  - 2));
			ZCalStep.setText(String.format("%.2f",Preferences_.zCalStep_));
			RinterStep.setText(String.format("%.2f",Preferences_.rInterpStep_));
			HalfCorrWin.setText(String.format("%.0f",Preferences_.halfQuadWindow_));
			MagnetStep.setText(String.format("%.1f",Preferences_.magnetMoveStep_));
			FrameToAcq.setText(String.format("%d",Preferences_.frameToAcq_));
			FrameToCalcForce.setText(String.format("%d",Preferences_.frameToCalcForce_));
			BeanRadiuPixel.setText(String.format("%.0f",Preferences_.beanRadiusPiexl_));
			Preservation1.setText(String.format("%.0f",Preferences_.preservation1_));
			Preservation2.setText(String.format("%.0f",Preferences_.preservation2_));
		}

	}

	private void initialize(){

		DialogListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{PhraseActionEvent(e);}};

			Toolkit kit = Toolkit.getDefaultToolkit();
			Dimension screen = kit.getScreenSize();
			frame = new JFrame();
			frame.getContentPane().setLayout(null);
			int frameWidth = (int)(ITEMWIDTH*4.4);
			int frameHeight = ITEMHEIGHT*14;
			frame.setBounds((int)(screen.width -frameWidth)/2,(int)(screen.height-frameHeight)/2,frameWidth ,frameHeight);
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			final JTabbedPane tabbedPane = new JTabbedPane();
			tabbedPane.setBounds(0,0,(int)(ITEMWIDTH*4.2), ITEMHEIGHT*12);
			frame.getContentPane().add(tabbedPane);

			final JPanel panel = new JPanel();
			panel.setLayout(null);
			tabbedPane.addTab("Preferences", null, panel, null);

			final JLabel label = new JLabel("BallRadius/um");
			label.setBounds(0,0, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label);
			BeanRadius = new JTextField();
			BeanRadius.setBounds(0, ITEMHEIGHT, ITEMWIDTH,ITEMHEIGHT);
			panel.add(BeanRadius);

			final JLabel label1 = new JLabel("DNALength/um");
			label1.setBounds(ITEMWIDTH,0, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label1);
			DNALength = new JTextField();
			DNALength.setBounds(ITEMWIDTH, ITEMHEIGHT, ITEMWIDTH,ITEMHEIGHT);
			panel.add(DNALength);

			final JLabel label2 = new JLabel("ZCalScale/um");
			label2.setBounds(ITEMWIDTH*2,0, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label2);

			ZCalScale =new JComboBox<String>();		 
			ZCalScale.setModel(new DefaultComboBoxModel(new String[] { "2", "3",
					"4", "5", "6", "7", "8" }));
			ZCalScale.setBounds(ITEMWIDTH*2, ITEMHEIGHT, ITEMWIDTH,ITEMHEIGHT);
			panel.add(ZCalScale);


			final JLabel label3 = new JLabel("Frame2Acq");
			label3.setBounds(ITEMWIDTH*3, 0, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label3);

			FrameToAcq = new JTextField();
			FrameToAcq.setBounds(ITEMWIDTH*3, ITEMHEIGHT, ITEMWIDTH,ITEMHEIGHT);
			panel.add(FrameToAcq);

			final JSeparator separator = new JSeparator();
			separator.setBounds(0,(int)(ITEMHEIGHT*2), ITEMWIDTH*4, 50);
			panel.add(separator);

			final JLabel label4 = new JLabel("ZCalStep/uM");
			label4.setBounds( 0, ITEMHEIGHT*3, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label4);
			ZCalStep = new JTextField();
			ZCalStep.setBounds(0, ITEMHEIGHT*4, ITEMWIDTH,ITEMHEIGHT);
			panel.add(ZCalStep);

			final JLabel label5 = new JLabel("RinterStep/pixel");
			label5.setBounds( ITEMWIDTH, ITEMHEIGHT*3, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label5);
			RinterStep = new JTextField();
			RinterStep.setBounds(ITEMWIDTH, ITEMHEIGHT*4, ITEMWIDTH,ITEMHEIGHT);
			panel.add(RinterStep);

			final JLabel label6 = new JLabel("HalfCorrWin");
			label6.setBounds( ITEMWIDTH*2, ITEMHEIGHT*3, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label6);
			HalfCorrWin = new JTextField();
			HalfCorrWin.setBounds(ITEMWIDTH*2, ITEMHEIGHT*4, ITEMWIDTH,ITEMHEIGHT);
			panel.add(HalfCorrWin);

			final JLabel label7 = new JLabel("MagnetStep/uM");
			label7.setBounds( ITEMWIDTH*3, ITEMHEIGHT*3, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label7);
			MagnetStep = new JTextField();
			MagnetStep.setBounds(ITEMWIDTH*3, ITEMHEIGHT*4, ITEMWIDTH,ITEMHEIGHT);
			panel.add(MagnetStep);
			final JSeparator separator1 = new JSeparator();
			separator1.setBounds(0,(int)(ITEMHEIGHT*5), ITEMWIDTH*4, 50);
			panel.add(separator1);

			final JLabel label8 = new JLabel("ITEM0");
			label8.setBounds( 0, ITEMHEIGHT*6, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label8);
			BeanRadiuPixel = new JTextField();
			BeanRadiuPixel.setBounds(0, ITEMHEIGHT*7, ITEMWIDTH,ITEMHEIGHT);
			panel.add(BeanRadiuPixel);

			final JLabel label9 = new JLabel("ITEM1");
			label9.setBounds( ITEMWIDTH, ITEMHEIGHT*6, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label9);
			Preservation1 = new JTextField();
			Preservation1.setBounds(ITEMWIDTH, ITEMHEIGHT*7, ITEMWIDTH,ITEMHEIGHT);
			panel.add(Preservation1);

			final JLabel label10 = new JLabel("ITEM2");
			label10.setBounds( ITEMWIDTH*2, ITEMHEIGHT*6, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label10);
			Preservation2 = new JTextField();
			Preservation2.setBounds(ITEMWIDTH*2, ITEMHEIGHT*7, ITEMWIDTH,ITEMHEIGHT);
			panel.add(Preservation2);

			final JLabel label11 = new JLabel("FrameCalcF");
			label11.setBounds( ITEMWIDTH*3, ITEMHEIGHT*6, ITEMWIDTH,ITEMHEIGHT);
			panel.add(label11);
			FrameToCalcForce = new JTextField();
			FrameToCalcForce.setBounds(ITEMWIDTH*3, ITEMHEIGHT*7, ITEMWIDTH,ITEMHEIGHT);
			panel.add(FrameToCalcForce);
			final JSeparator separator2 = new JSeparator();
			separator2.setBounds(0,(int)(ITEMHEIGHT*8), ITEMWIDTH*4, 50);
			panel.add(separator2);

			final JButton OK = new JButton("OK");
			OK.setBounds(0,  (int)(ITEMHEIGHT*8.5),ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
			panel.add(OK);
			final JButton Cancel = new JButton("Cancel");
			Cancel.setBounds(ITEMWIDTH, (int)(ITEMHEIGHT*8.5), ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
			panel.add(Cancel);
			final JButton SelectDir = new JButton("SelectDir");
			SelectDir.setBounds(ITEMWIDTH*2, (int)(ITEMHEIGHT*8.5), ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
			panel.add(SelectDir);
			final JButton OpenDir = new JButton("OpenDir");
			OpenDir.setBounds(ITEMWIDTH*3, (int)(ITEMHEIGHT*8.5), ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
			panel.add(OpenDir);

			OK.addActionListener(DialogListener);
			Cancel.addActionListener(DialogListener);
			SelectDir.addActionListener(DialogListener);
			OpenDir.addActionListener(DialogListener);
	}

	private void PhraseActionEvent(ActionEvent e){
		if (e.getActionCommand().equals("OK")) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					UpdateData(true);//flush
					frame.setVisible(false);
				}
			});

		}
		if (e.getActionCommand().equals("Cancel")) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					frame.setVisible(false);
				}
			});

		}

		if (e.getActionCommand().equals("SelectDir")) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JFileChooser fileChooser = new JFileChooser(".");		 
					fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					fileChooser.setDialogTitle("打开文件夹");
					int ret = fileChooser.showOpenDialog(null);
					if (ret == JFileChooser.APPROVE_OPTION) {
						Preferences_.userDataDir_=fileChooser.getSelectedFile().getAbsolutePath();
						Preferences_.saveUserData();
						myform_.log(String.format("Current DataDir is:%s.",Preferences_.userDataDir_));
					}
				}
			});

		}

		if (e.getActionCommand().equals("OpenDir")) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {
						Runtime.getRuntime().exec("explorer /select, "+Preferences_.userDataDir_);
					} catch (IOException e) {
						e.printStackTrace();
					}
					frame.setVisible(false);
				}
			});

		}


	}

}
