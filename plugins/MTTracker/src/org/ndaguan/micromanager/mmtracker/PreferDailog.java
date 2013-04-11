package org.ndaguan.micromanager.mmtracker;

import ij.IJ;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class PreferDailog extends JFrame {
	private static final long serialVersionUID = 1L;
	final private int ITEMWIDTH = 140;
	final private int ITEMHEIGHT = 25;
	private JFrame instance_;

	private JTextField[] jTextField;

	private JLabel[] jLabel;

	private ActionListener DialogListener;
	private Preferences preferences_;
	private int preferencesLen;
	private int columnNum = 4;

	public static void main(String[] arg){

		PreferDailog pre = new PreferDailog(new Preferences());
		pre.setVisible(true);

	}
	public PreferDailog(Preferences preferences) {
		preferences_ = preferences;
		preferencesLen = MMT.VARNAME.length;
		jTextField = new JTextField[preferencesLen];
		jLabel = new JLabel[preferencesLen];
		instance_ = this;
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
			double[] preferences = new double[preferencesLen];
			for (int i = 0; i <preferencesLen; i++) {
				preferences[i] = Double.parseDouble(jTextField[i].getText());
			}			
			preferences_.onDataChange(preferences);
		}
		else{//refresh GUI
			for (int i = 0; i <preferencesLen; i++) {
				String str = String.format("%s.%df","%" ,MMT.PRECISION[i]);
				jTextField[i].setText(String.format(str,preferences_.getUserData()[i]));
			}	
		}

	}

	private void initialize(){

		DialogListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{PhraseActionEvent(e);}};

			Toolkit kit = Toolkit.getDefaultToolkit();
			Dimension screen = kit.getScreenSize();
			getContentPane().setLayout(null);
			int frameWidth = (int)(ITEMWIDTH*columnNum);
			int frameHeight = 2*ITEMHEIGHT*preferencesLen/columnNum + ITEMHEIGHT*6 ;
			setBounds((int)(screen.width -frameWidth)/2,(int)(screen.height-frameHeight)/2,frameWidth ,frameHeight);
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			final JTabbedPane tabbedPane = new JTabbedPane();
			tabbedPane.setBounds(0,0,(int)(ITEMWIDTH*4.2), ITEMHEIGHT*12);
			getContentPane().add(tabbedPane);

			final JPanel panel = new JPanel();
			panel.setLayout(null);
			tabbedPane.addTab("Preferences", null, panel, null);
			int y = 0;
			int x = 0;
			for (int i = 0; i < preferencesLen; i++) {
				jLabel[i] = new JLabel();
				jLabel[i].setText(String.format("%s%s", MMT.VARNAME[i],MMT.UNIT[i]));
				jLabel[i].setBounds(x,y,ITEMWIDTH,ITEMHEIGHT);
				panel.add(jLabel[i]);
				y += ITEMHEIGHT;
				jTextField[i] = new JTextField();
				jTextField[i].setBounds(x, y, ITEMWIDTH,ITEMHEIGHT);
				panel.add(jTextField[i]);
				x += ITEMWIDTH;
				y -= ITEMHEIGHT;
				if((i+1)%columnNum  == 0 ){
					y += 2*ITEMHEIGHT;
					x = 0;
				}
			}			

			y +=ITEMHEIGHT;
			x = 0;

			final JSeparator separator2 = new JSeparator();
			separator2.setBounds(0,y, ITEMWIDTH*4, 50);
			panel.add(separator2);
			y += ITEMHEIGHT;
			final JButton OK = new JButton("OK");
			OK.setBounds(0,  y,ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
			panel.add(OK);
			x += ITEMWIDTH;

			final JButton Cancel = new JButton("Cancel");
			Cancel.setBounds(x,y, ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
			panel.add(Cancel);
			x += ITEMWIDTH;

			final JButton SelectDir = new JButton("SelectDir");
			SelectDir.setBounds(x,y, ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
			panel.add(SelectDir);
			x += ITEMWIDTH;

			final JButton OpenDir = new JButton("OpenDir");
			OpenDir.setBounds(x,y, ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
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
					instance_.setVisible(false);
				}
			});

		}
		if (e.getActionCommand().equals("Cancel")) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					instance_.setVisible(false);
				}
			});

		}

		if (e.getActionCommand().equals("SelectDir")) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JFileChooser fileChooser = new JFileChooser(".");		 
					fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					fileChooser.setDialogTitle("Select a data save path");
					int ret = fileChooser.showOpenDialog(null);
					if (ret == JFileChooser.APPROVE_OPTION) {
						preferences_.userDataDir_=fileChooser.getSelectedFile().getAbsolutePath();
						preferences_.saveUserData();
						IJ.log(String.format("\r\nCurrent DataDir is:%s.",preferences_.userDataDir_));
					}
				}
			});

		}

		if (e.getActionCommand().equals("OpenDir")) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					try {
						Runtime.getRuntime().exec("explorer /select, "+preferences_.userDataDir_);
					} catch (IOException e) {
						e.printStackTrace();
					}
					instance_.setVisible(false);
				}
			});

		}


	}

}
