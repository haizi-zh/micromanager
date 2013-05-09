package org.ndaguan.micromanager.mmtracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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
	private int ITEMWIDTH = 140;
	private int ITEMHEIGHT = 25;
	final private int ITEMROW = 24;
	private JFrame instance_;

	private  JTextField[] jTextField;

	private JLabel[] jLabel;

	private ActionListener DialogListener;
	private int preferencesLen;
	private int columnNum = 4;
	String userDataDir_ = "";
	public static void main(String[] arg){

		PreferDailog pre = new PreferDailog();
		pre.setVisible(true);

	}
	public void enableEdit(boolean flag){
		 
		for (int i = 0; i < MMT.unEditAfterCalbration.length; i++) {
			this.jTextField[MMT.unEditAfterCalbration[i]].setEnabled(flag);
		}
	}
	public PreferDailog() {
		preferencesLen = MMT.VariablesNUPD.values().length;
		jTextField = new JTextField[preferencesLen];
		jLabel = new JLabel[preferencesLen];
		instance_ = this;
		initialize();
		onDataChange(getUserData());
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				UpdateData(false);//GUI  update
			}
		});

	}
	public void onDataChange(double[] data) {
		if(data != null){
			for (int i = 0; i < data.length; i++) {
				MMT.VariablesNUPD.values()[i].value(data[i]);
			}
			MMTracker mmt = MMTracker.getInstance();
			if(mmt != null){
				List<RoiItem> roilist = mmt.getRoiList();
				for(RoiItem item: roilist){
					item.setWidowSize(MMT.VariablesNUPD.frameToCalcForce.value());
					item.setChartWidowSize(MMT.VariablesNUPD.chartStatisWindow.value());
					item.setChartDrawingWidowSize((int)MMT.VariablesNUPD.chartWindowSize.value());
				}
			}
		}
		saveUserData();
	}

	public double[] getUserData() {
		try {
			File loginDataFile = new File(System.getProperty("user.home")+"/MMTracker/userData.txt");
			if(!loginDataFile.exists())
				return null;
			BufferedReader in;
			in = new BufferedReader(new FileReader(loginDataFile));
			String line;
			if((line = in.readLine()) == null)
			{
				in.close();
				return null;
			}
			String[] temp = line.split(","); 
			double[] userDataSet = new double[MMT.VariablesNUPD.values().length];
			if((temp.length-1) != MMT.VariablesNUPD.values().length){
				for (int i = 0; i < MMT.VariablesNUPD.values().length; i++) {
					 userDataSet[i]  = MMT.VariablesNUPD.values()[i].value();
				}
			}
			else{
			for (int i = 0; i < userDataSet.length; i++) {
				userDataSet[i] = Double.parseDouble(temp[i]);				
			}
			}
			if((line = in.readLine()) != null)
				userDataDir_ = line;
			in.close();
			return userDataSet;
		} catch (IOException e) {
			MMT.logError("read user data false");
			return null;
		} 
	}


	void saveUserData(){
		try {
			File dir = new File(System.getProperty("user.home"),"MMTracker");
			if(!dir.isFile())
				dir.mkdirs();

			File loginDataFile = new File(System.getProperty("user.home")+"/MMTracker/userData.txt");
			FileWriter out = new FileWriter((loginDataFile)); 
			String sData = "";
			for (int i = 0; i < MMT.VariablesNUPD.values().length; i++) {
				sData +=  Double.toString(MMT.VariablesNUPD.values()[i].value())+ " , ";
			}
			sData += "\r\n"+ userDataDir_;
			out.write(sData);
			out.close(); 
		} catch (IOException e) {
			MMT.logError("save user data err");
		}
	}
	/**
	 * @param flag true save to file,false: update gui
	 */
	public void UpdateData(boolean flag) {
		if(flag){//flush
			double[] preferences = new double[preferencesLen];
			for (int i = 0; i <preferencesLen; i++) {
				preferences[i] = Double.parseDouble(jTextField[i].getText());
			}			
			onDataChange(preferences);
		}
		else{//refresh GUI
			for (int i = 0; i <preferencesLen; i++) {
				double presicion = MMT.VariablesNUPD.values()[i].getPresicion();
				String str;
				if((presicion < 1/1e10)){
					str = String.format("%s.%df","%" , 0);
				}
				else{
				str = String.format("%s.%df","%" ,(int)(Math.log10(1/presicion)));
				}
				jTextField[i].setText(String.format(str,getUserData()[i]));
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
			int frameHeight = 2*ITEMHEIGHT*preferencesLen/columnNum + ITEMHEIGHT*ITEMROW/3 ;
			setBounds((int)(screen.width -frameWidth)/2,(int)(screen.height-frameHeight)/2,frameWidth ,frameHeight);
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			final JTabbedPane tabbedPane = new JTabbedPane();
			tabbedPane.setBounds(0,0,(int)(ITEMWIDTH*4.2), ITEMHEIGHT*ITEMROW);
			getContentPane().add(tabbedPane);

			final JPanel panel = new JPanel();
			panel.setLayout(null);
			tabbedPane.addTab("Preferences", null, panel, null);
			int y = 0;
			int x = 0;
			for (int i = 0; i < preferencesLen; i++) {//edit & label
				jLabel[i] = new JLabel();
				jLabel[i].setText(String.format("%s%s", MMT.VariablesNUPD.values()[i].name(),MMT.VariablesNUPD.values()[i].getUnit()));
				jLabel[i].setBounds(x,y,ITEMWIDTH,ITEMHEIGHT);
				panel.add(jLabel[i]);
				y += ITEMHEIGHT;
				jTextField[i] = new JTextField();
				jTextField[i].setBounds(x, y, ITEMWIDTH,ITEMHEIGHT);
				jTextField[i].setToolTipText(MMT.VariablesNUPD.values()[i].getToolTip());
				if(MMT.VariablesNUPD.values()[i].getImp()==1)
				jTextField[i].setForeground(new Color(255,0,0));
				panel.add(jTextField[i]);
				x += ITEMWIDTH;
				y -= ITEMHEIGHT;
				if((i+1)%columnNum  == 0 ){
					y += 2*ITEMHEIGHT;
					x = 0;
				}
			}			

			y +=ITEMHEIGHT*2;
			x = 0;
			

			final JSeparator separator2 = new JSeparator();
			separator2.setBounds(0,y, ITEMWIDTH*4, 50);
			panel.add(separator2);
			y += ITEMHEIGHT;
			
			ITEMWIDTH = ITEMWIDTH*3/4;
			
			final JButton OK = new JButton("OK");
			OK.setBounds(0,  y,ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
			panel.add(OK);
			x += ITEMWIDTH;
			
			final JButton Apply = new JButton("Apply");
			Apply.setBounds(x,  y,ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
			panel.add(Apply);
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
			Apply.addActionListener(DialogListener);
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
		if (e.getActionCommand().equals("Apply")) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					UpdateData(true);//flush
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
						userDataDir_=fileChooser.getSelectedFile().getAbsolutePath();
						saveUserData();
					}
				}
			});

		}

		if (e.getActionCommand().equals("OpenDir")) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					try {
						Runtime.getRuntime().exec("explorer /select, "+userDataDir_);
					} catch (IOException e) {
						e.printStackTrace();
					}
					instance_.setVisible(false);
				}
			});

		}


	}

}
