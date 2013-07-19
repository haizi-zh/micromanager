
/**
 * StageControlFrame.java
 *
 * Created on Aug 19, 2010, 10:04:49 PM
 * Nico Stuurman, copyright UCSF, 2010
 * 
 * LICENSE:      This file is distributed under the BSD license.
 *               License text is included with the source distribution.
 *
 *               This file is distributed in the hope that it will be useful,
 *               but WITHOUT ANY WARRANTY; without even the implied warranty
 *               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
 */

package org.micromanager.stagecontrol;


import mmcorej.CMMCore;

import java.text.NumberFormat;
import java.text.ParseException;

import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javax.comm.*;

import org.micromanager.api.ScriptInterface;

/**
 *
 * @author nico
 */
public class MagnetControlFrame extends javax.swing.JFrame {
	private final ScriptInterface gui_;
	private final CMMCore core_;
	private Preferences prefs_;

	private double smallMovement_ = 1.0;
	private double mediumMovement_ = 10.0;
	private double largeMovement_ = 100.0;
	private double smallMovementZ_ = 1.0;
	private double mediumMovementZ_ = 10.0;
	private NumberFormat nf_;

	private int frameXPos_ = 100;
	private int frameYPos_ = 100;
	private SerialPort serialPort;
	private boolean deviceIsBusy = false;

	private static final String FRAMEXPOS = "FRAMEXPOS";
	private static final String FRAMEYPOS = "FRAMEYPOS";
	private static final String SMALLMOVEMENT = "SMALLMOVEMENT";
	private static final String MEDIUMMOVEMENT = "MEDIUMMOVEMENT";
	private static final String LARGEMOVEMENT = "LARGEMOVEMENT";
	private static final String SMALLMOVEMENTZ = "SMALLMOVEMENTZ";
	private static final String MEDIUMMOVEMENTZ = "MEDIUMMOVEMENTZ";

	public static void main(String[] str){
		MagnetControlFrame myFrame_ = new MagnetControlFrame();
		myFrame_.setVisible(true);
	}

	private void moveMagnet(double z) throws InterruptedException {
		if(deviceIsBusy)
			return;
		if(z<0){
			serialPort.setRTS(true);//BLUE
		}else{
			serialPort.setRTS(false);//BLUE
		}
		TimeUnit.MILLISECONDS.sleep(50);
		double distance = Math.abs(z);
		int pulseToUm = 10;
		int pluseToSend =  (int) (distance*pulseToUm);
		int velosity = 1000;//(0~100)
		long interval = 1000/velosity;
		deviceIsBusy = true;
		for (int i = 0; i < pluseToSend; i++) {
			serialPort.setDTR(true);//yellow
			TimeUnit.MILLISECONDS.sleep(interval);
			serialPort.setDTR(false);
			TimeUnit.MILLISECONDS.sleep(interval);
		}
		deviceIsBusy = false;

	}

	/** Creates new form StageControlFrame */
	public MagnetControlFrame(ScriptInterface gui) {
		gui_ = gui;
		core_ = gui_.getMMCore();
		nf_ = NumberFormat.getInstance();
		prefs_ = Preferences.userNodeForPackage(this.getClass());

		// Read values from PREFS
		frameXPos_ = prefs_.getInt(FRAMEXPOS, frameXPos_);
		frameYPos_ = prefs_.getInt(FRAMEYPOS, frameYPos_);
		double pixelSize = core_.getPixelSizeUm();
		long nrPixelsX = core_.getImageWidth();
		smallMovement_ = prefs_.getDouble(SMALLMOVEMENT, pixelSize);
		mediumMovement_ = prefs_.getDouble(MEDIUMMOVEMENT, pixelSize * nrPixelsX * 0.1);
		largeMovement_ = prefs_.getDouble(LARGEMOVEMENT, pixelSize * nrPixelsX);
		smallMovementZ_ = prefs_.getDouble(SMALLMOVEMENTZ, smallMovementZ_);
		mediumMovementZ_ = prefs_.getDouble(MEDIUMMOVEMENTZ, mediumMovementZ_);

		initComponents();

		setLocation(frameXPos_, frameYPos_);

		setBackground(gui_.getBackgroundColor());
		gui_.addMMBackgroundListener(this);

		jTextField1.setText(nf_.format(smallMovement_));
		jTextField2.setText(nf_.format(mediumMovement_));
		jTextField3.setText(nf_.format(largeMovement_));
		jTextField4.setText(nf_.format(smallMovementZ_));
		jTextField5.setText(nf_.format(mediumMovementZ_));
	}
	/** Creates new form StageControlFrame */
	public MagnetControlFrame() {
		gui_ = null;
		core_ = null;

		nf_ = NumberFormat.getInstance();
		prefs_ = Preferences.userNodeForPackage(this.getClass());

		// Read values from PREFS
		frameXPos_ = prefs_.getInt(FRAMEXPOS, frameXPos_);
		frameYPos_ = prefs_.getInt(FRAMEYPOS, frameYPos_);
		double pixelSize = 1;
		long nrPixelsX = 1;
		smallMovement_ = prefs_.getDouble(SMALLMOVEMENT, pixelSize);
		mediumMovement_ = prefs_.getDouble(MEDIUMMOVEMENT, pixelSize * nrPixelsX * 0.1);
		largeMovement_ = prefs_.getDouble(LARGEMOVEMENT, pixelSize * nrPixelsX);
		smallMovementZ_ = prefs_.getDouble(SMALLMOVEMENTZ, smallMovementZ_);
		mediumMovementZ_ = prefs_.getDouble(MEDIUMMOVEMENTZ, mediumMovementZ_);

		initComponents();
		initComn("COM1");

		setLocation(frameXPos_, frameYPos_);

		jTextField1.setText(nf_.format(smallMovement_));
		jTextField2.setText(nf_.format(mediumMovement_));
		jTextField3.setText(nf_.format(largeMovement_));
		jTextField4.setText(nf_.format(smallMovementZ_));
		jTextField5.setText(nf_.format(mediumMovementZ_));
	}

	private void initComn(String comId) {
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (portId.getName().equals(comId)) {
					try {
						serialPort = (SerialPort)
								portId.open("", 2000);
					} catch (PortInUseException e) {}

				}
			}
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	//@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jPanel1 = new javax.swing.JPanel();
		jButton1 = new javax.swing.JButton();
		jButton10 = new javax.swing.JButton();
		jButton2 = new javax.swing.JButton();
		jButton3 = new javax.swing.JButton();
		jButton7 = new javax.swing.JButton();
		jButton11 = new javax.swing.JButton();
		jButton8 = new javax.swing.JButton();
		jButton4 = new javax.swing.JButton();
		jButton12 = new javax.swing.JButton();
		jButton5 = new javax.swing.JButton();
		jButton9 = new javax.swing.JButton();
		jButton6 = new javax.swing.JButton();
		jTextField1 = new javax.swing.JTextField();
		jTextField2 = new javax.swing.JTextField();
		jTextField3 = new javax.swing.JTextField();
		jLabel1 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jLabel3 = new javax.swing.JLabel();
		jLabel4 = new javax.swing.JLabel();
		jLabel5 = new javax.swing.JLabel();
		jLabel6 = new javax.swing.JLabel();
		jButton13 = new javax.swing.JButton();
		jButton14 = new javax.swing.JButton();
		jButton15 = new javax.swing.JButton();
		jLabel7 = new javax.swing.JLabel();
		jButton16 = new javax.swing.JButton();
		jButton17 = new javax.swing.JButton();
		jButton18 = new javax.swing.JButton();
		jButton19 = new javax.swing.JButton();
		jTextField4 = new javax.swing.JTextField();
		jTextField5 = new javax.swing.JTextField();
		jLabel8 = new javax.swing.JLabel();
		jLabel9 = new javax.swing.JLabel();
		jLabel10 = new javax.swing.JLabel();

		setTitle("Stage Control");
		setLocationByPlatform(true);
		setResizable(false);
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				onWindowClosing(evt);
			}
		});

		jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-sl.png"))); // NOI18N
		jButton1.setBorderPainted(false);
		jButton1.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-slp.png"))); // NOI18N
		jButton1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton1ActionPerformed(evt);
			}
		});

		jButton10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-sd.png"))); // NOI18N
		jButton10.setBorderPainted(false);
		jButton10.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-sdp.png"))); // NOI18N
		jButton10.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton10ActionPerformed(evt);
			}
		});

		jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-tl.png"))); // NOI18N
		jButton2.setBorderPainted(false);
		jButton2.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-tlp.png"))); // NOI18N
		jButton2.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton2ActionPerformed(evt);
			}
		});

		jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-dl.png"))); // NOI18N
		jButton3.setBorderPainted(false);
		jButton3.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-dlp.png"))); // NOI18N
		jButton3.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton3ActionPerformed(evt);
			}
		});

		jButton7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-su.png"))); // NOI18N
		jButton7.setBorderPainted(false);
		jButton7.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-sup.png"))); // NOI18N
		jButton7.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton7ActionPerformed(evt);
			}
		});

		jButton11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-dd.png"))); // NOI18N
		jButton11.setBorderPainted(false);
		jButton11.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-ddp.png"))); // NOI18N
		jButton11.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton11ActionPerformed(evt);
			}
		});

		jButton8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-tu.png"))); // NOI18N
		jButton8.setBorderPainted(false);
		jButton8.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-tup.png"))); // NOI18N
		jButton8.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton8ActionPerformed(evt);
			}
		});

		jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-sr.png"))); // NOI18N
		jButton4.setBorderPainted(false);
		jButton4.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-srp.png"))); // NOI18N
		jButton4.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton4ActionPerformed(evt);
			}
		});

		jButton12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-td.png"))); // NOI18N
		jButton12.setBorderPainted(false);
		jButton12.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-tdp.png"))); // NOI18N
		jButton12.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton12ActionPerformed(evt);
			}
		});

		jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-tr.png"))); // NOI18N
		jButton5.setBorderPainted(false);
		jButton5.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-trp.png"))); // NOI18N
		jButton5.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton5ActionPerformed(evt);
			}
		});

		jButton9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-du.png"))); // NOI18N
		jButton9.setBorderPainted(false);
		jButton9.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-dup.png"))); // NOI18N
		jButton9.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton9ActionPerformed(evt);
			}
		});

		jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-dr.png"))); // NOI18N
		jButton6.setBorderPainted(false);
		jButton6.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-drp.png"))); // NOI18N
		jButton6.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton6ActionPerformed(evt);
			}
		});

		org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
		jPanel1.setLayout(jPanel1Layout);
		jPanel1Layout.setHorizontalGroup(
				jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(jPanel1Layout.createSequentialGroup()
						.addContainerGap()
						.add(jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
						.add(jButton3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
						.add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
								.add(jButton12, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
								.add(org.jdesktop.layout.GroupLayout.LEADING, jButton11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
								.add(jButton10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
								.add(org.jdesktop.layout.GroupLayout.LEADING, jButton8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
								.add(org.jdesktop.layout.GroupLayout.LEADING, jButton9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
								.add(jButton7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
								.add(jPanel1Layout.createSequentialGroup()
										.add(jButton1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.add(28, 28, 28)
										.add(jButton4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
										.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
										.add(jButton6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
										.add(jButton5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.addContainerGap())
				);
		jPanel1Layout.setVerticalGroup(
				jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
						.addContainerGap(7, Short.MAX_VALUE)
						.add(jButton8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
						.add(jButton9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						.add(jButton7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
						.add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								.add(jButton4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jButton1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jButton3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jButton6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(jButton5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
								.add(jButton10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
								.add(jButton11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
								.add(jButton12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 33, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
				);

		jTextField1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jTextField1ActionPerformed(evt);
			}
		});
		jTextField1.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent evt) {
				focusLostHandlerJTF1(evt);
			}
		});

		jTextField2.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jTextField2ActionPerformed(evt);
			}
		});
		jTextField2.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent evt) {
				focusLostHandlerJTF2(evt);
			}
		});

		jTextField3.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jTextField3ActionPerformed(evt);
			}
		});
		jTextField3.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent evt) {
				focusLostHandlerJTF3(evt);
			}
		});

		jLabel1.setIcon(new javax.swing.ImageIcon("/Users/nico/svn/micromanager1.4/plugins/StageControl/src/org/micromanager/stagecontrol/icons/arrowhead-sr.png")); // NOI18N

		jLabel2.setIcon(new javax.swing.ImageIcon("/Users/nico/svn/micromanager1.4/plugins/StageControl/src/org/micromanager/stagecontrol/icons/arrowhead-dr.png")); // NOI18N

		jLabel3.setIcon(new javax.swing.ImageIcon("/Users/nico/svn/micromanager1.4/plugins/StageControl/src/org/micromanager/stagecontrol/icons/arrowhead-tr.png")); // NOI18N

		jLabel4.setText("µm");

		jLabel5.setText("µm");

		jLabel6.setText("µm");

		jButton13.setFont(new java.awt.Font("Arial", 0, 10));
		jButton13.setText("1 pixel");
		jButton13.setIconTextGap(6);
		jButton13.setMaximumSize(new java.awt.Dimension(0, 0));
		jButton13.setMinimumSize(new java.awt.Dimension(0, 0));
		jButton13.setPreferredSize(new java.awt.Dimension(35, 20));
		jButton13.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton13ActionPerformed(evt);
			}
		});

		jButton14.setFont(new java.awt.Font("Arial", 0, 10));
		jButton14.setText("0.1 field");
		jButton14.setMaximumSize(new java.awt.Dimension(35, 20));
		jButton14.setMinimumSize(new java.awt.Dimension(0, 0));
		jButton14.setPreferredSize(new java.awt.Dimension(35, 20));
		jButton14.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton14ActionPerformed(evt);
			}
		});

		jButton15.setFont(new java.awt.Font("Arial", 0, 10));
		jButton15.setText("1 field");
		jButton15.setMaximumSize(new java.awt.Dimension(35, 20));
		jButton15.setMinimumSize(new java.awt.Dimension(0, 0));
		jButton15.setPreferredSize(new java.awt.Dimension(35, 20));
		jButton15.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton15ActionPerformed(evt);
			}
		});

		jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel7.setText("XY Stage");

		jButton16.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-du.png"))); // NOI18N
		jButton16.setBorderPainted(false);
		jButton16.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-dup.png"))); // NOI18N
		jButton16.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton16ActionPerformed(evt);
			}
		});

		jButton17.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-su.png"))); // NOI18N
		jButton17.setBorderPainted(false);
		jButton17.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-sup.png"))); // NOI18N
		jButton17.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton17ActionPerformed(evt);
			}
		});

		jButton18.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-sd.png"))); // NOI18N
		jButton18.setBorderPainted(false);
		jButton18.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-sdp.png"))); // NOI18N
		jButton18.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton18ActionPerformed(evt);
			}
		});

		jButton19.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-dd.png"))); // NOI18N
		jButton19.setBorderPainted(false);
		jButton19.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/micromanager/stagecontrol/icons/arrowhead-ddp.png"))); // NOI18N
		jButton19.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton19ActionPerformed(evt);
			}
		});

		jTextField4.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jTextField4ActionPerformed(evt);
			}
		});
		jTextField4.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent evt) {
				jTextField4focusLostHandlerJTF1(evt);
			}
		});

		jTextField5.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jTextField5ActionPerformed(evt);
			}
		});
		jTextField5.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent evt) {
				jTextField5focusLostHandlerJTF2(evt);
			}
		});

		jLabel8.setText("µm");

		jLabel9.setText("µm");

		jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel10.setText("Z Stage");

		org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(layout.createSequentialGroup()
						.addContainerGap()
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
								.add(layout.createSequentialGroup()
										.add(21, 21, 21)
										.add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.add(layout.createSequentialGroup()
												.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
														.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
																.add(jLabel1)
																.add(jLabel2))
																.add(jLabel3))
																.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
																.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
																		.add(org.jdesktop.layout.GroupLayout.LEADING, jTextField3, 0, 0, Short.MAX_VALUE)
																		.add(org.jdesktop.layout.GroupLayout.LEADING, jTextField2, 0, 0, Short.MAX_VALUE)
																		.add(org.jdesktop.layout.GroupLayout.LEADING, jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 55, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
																		.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
																				.add(layout.createSequentialGroup()
																						.add(jLabel6)
																						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
																						.add(jButton15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 85, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
																						.add(layout.createSequentialGroup()
																								.add(jLabel5)
																								.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
																								.add(jButton14, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE))
																								.add(layout.createSequentialGroup()
																										.add(jLabel4)
																										.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
																										.add(jButton13, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE))))
																										.add(layout.createSequentialGroup()
																												.add(53, 53, 53)
																												.add(jLabel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 116, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
																												.add(18, 18, 18)
																												.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
																														.add(layout.createSequentialGroup()
																																.add(8, 8, 8)
																																.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
																																		.add(jButton19, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
																																		.add(org.jdesktop.layout.GroupLayout.TRAILING, jButton18, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
																																		.add(jButton16, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
																																		.add(org.jdesktop.layout.GroupLayout.TRAILING, jButton17, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
																																		.add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
																																				.add(30, 30, 30)
																																				.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
																																						.add(org.jdesktop.layout.GroupLayout.LEADING, jTextField5, 0, 0, Short.MAX_VALUE)
																																						.add(org.jdesktop.layout.GroupLayout.LEADING, jTextField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 55, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
																																						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
																																						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
																																								.add(jLabel8)
																																								.add(jLabel9))
																																								.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
																																								.add(layout.createSequentialGroup()
																																										.add(jLabel10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 116, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																																										.add(12, 12, 12)))
																																										.addContainerGap())
				);
		layout.setVerticalGroup(
				layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(layout.createSequentialGroup()
						.addContainerGap()
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
								.add(layout.createSequentialGroup()
										.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 56, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.add(jButton16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
										.add(jButton17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.add(30, 30, 30)
										.add(jButton18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
										.add(jButton19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.add(49, 49, 49)
										.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
												.add(jTextField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												.add(jLabel9))
												.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
												.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
														.add(jLabel8)))
														.add(layout.createSequentialGroup()
																.add(jLabel10)
																.add(265, 265, 265))
																.add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
																		.add(jLabel7)
																		.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 9, Short.MAX_VALUE)
																		.add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
																				.add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																				.add(jLabel1)
																				.add(jLabel4)
																				.add(jButton13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
																				.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
																				.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
																						.add(jTextField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																						.add(jLabel2)
																						.add(jLabel5)
																						.add(jButton14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
																						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
																						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
																								.add(jTextField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																								.add(jLabel3)
																								.add(jLabel6)
																								.add(jButton15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
																								.add(26, 26, 26))
				);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void setRelativeXYStagePosition(double x, double y) {

	}

	private void setRelativeStagePosition(double z)
	{
		try {
			moveMagnet(z);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
		setRelativeXYStagePosition(-smallMovement_, 0.0);
	}//GEN-LAST:event_jButton1ActionPerformed

	private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
		setRelativeXYStagePosition(-largeMovement_, 0.0);
	}//GEN-LAST:event_jButton2ActionPerformed

	private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
		setRelativeXYStagePosition(-mediumMovement_, 0.0);
	}//GEN-LAST:event_jButton3ActionPerformed

	private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
		setRelativeXYStagePosition(smallMovement_, 0.0);
	}//GEN-LAST:event_jButton4ActionPerformed

	private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
		setRelativeXYStagePosition(largeMovement_, 0.0);
	}//GEN-LAST:event_jButton5ActionPerformed

	private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
		setRelativeXYStagePosition(mediumMovement_, 0.0);
	}//GEN-LAST:event_jButton6ActionPerformed

	private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
		setRelativeXYStagePosition(0.0, -smallMovement_);
	}//GEN-LAST:event_jButton7ActionPerformed

	private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
		setRelativeXYStagePosition(0.0, -largeMovement_);
	}//GEN-LAST:event_jButton8ActionPerformed

	private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
		setRelativeXYStagePosition(0.0, -mediumMovement_);
	}//GEN-LAST:event_jButton9ActionPerformed

	private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
		setRelativeXYStagePosition(0.0, smallMovement_);
	}//GEN-LAST:event_jButton10ActionPerformed

	private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
		setRelativeXYStagePosition(0.0, mediumMovement_);
	}//GEN-LAST:event_jButton11ActionPerformed

	private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
		setRelativeXYStagePosition(0.0, largeMovement_);
	}//GEN-LAST:event_jButton12ActionPerformed

	private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
		try {
			mediumMovement_ = nf_.parse(jTextField2.getText()).doubleValue();
		} catch(ParseException e) {
			// ignore if parsing fails
		}
	}//GEN-LAST:event_jTextField2ActionPerformed

	private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
		long nrPixelsX = core_.getImageWidth();
		double pixelSize = core_.getPixelSizeUm();
		jTextField2.setText(nf_.format(pixelSize * nrPixelsX * 0.1));
	}//GEN-LAST:event_jButton14ActionPerformed

	private void jTextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField3ActionPerformed
		try {
			largeMovement_ = nf_.parse(jTextField3.getText()).doubleValue();
		} catch(ParseException e) {
			// ignore if parsing fails
		}
	}//GEN-LAST:event_jTextField3ActionPerformed

	private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
		try {
			smallMovement_ = nf_.parse(jTextField1.getText()).doubleValue();
		} catch(ParseException e) {
			// ignore if parsing fails
		}
	}//GEN-LAST:event_jTextField1ActionPerformed

	private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
		double pixelSize = core_.getPixelSizeUm();
		jTextField1.setText(nf_.format(pixelSize));
		smallMovement_ = pixelSize;
	}//GEN-LAST:event_jButton13ActionPerformed

	private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
		long nrPixelsX = core_.getImageWidth();
		double pixelSize = core_.getPixelSizeUm();
		jTextField3.setText(nf_.format(pixelSize * nrPixelsX));
	}//GEN-LAST:event_jButton15ActionPerformed

	private void onWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_onWindowClosing
		prefs_.putInt(FRAMEXPOS, (int) getLocation().getX());
		prefs_.putInt(FRAMEYPOS, (int) getLocation().getY());
		prefs_.putDouble(SMALLMOVEMENT, smallMovement_);
		prefs_.putDouble(MEDIUMMOVEMENT, mediumMovement_);
		prefs_.putDouble(LARGEMOVEMENT, largeMovement_);
		prefs_.putDouble(SMALLMOVEMENTZ, smallMovementZ_);
		prefs_.putDouble(MEDIUMMOVEMENTZ, mediumMovementZ_);
	}//GEN-LAST:event_onWindowClosing

	private void focusLostHandlerJTF1(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_focusLostHandlerJTF1
		try {
			smallMovement_ = nf_.parse(jTextField1.getText()).doubleValue();
		} catch(ParseException e) {
			// ignore if parsing fails
		}
	}//GEN-LAST:event_focusLostHandlerJTF1

	private void focusLostHandlerJTF2(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_focusLostHandlerJTF2
		try {
			mediumMovement_ = nf_.parse(jTextField2.getText()).doubleValue();
		} catch(ParseException e) {
			// ignore if parsing fails
		}
	}//GEN-LAST:event_focusLostHandlerJTF2

	private void focusLostHandlerJTF3(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_focusLostHandlerJTF3
		try {
			largeMovement_ = nf_.parse(jTextField3.getText()).doubleValue();
		} catch(ParseException e) {
			// ignore if parsing fails
		}
	}//GEN-LAST:event_focusLostHandlerJTF3

	private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton16ActionPerformed
		setRelativeStagePosition(mediumMovementZ_);
	}//GEN-LAST:event_jButton16ActionPerformed

	private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton17ActionPerformed
		setRelativeStagePosition(smallMovementZ_);
	}//GEN-LAST:event_jButton17ActionPerformed

	private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
		setRelativeStagePosition(-smallMovementZ_);
	}//GEN-LAST:event_jButton18ActionPerformed

	private void jButton19ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton19ActionPerformed
		setRelativeStagePosition(-mediumMovementZ_);
	}//GEN-LAST:event_jButton19ActionPerformed

	private void jTextField4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField4ActionPerformed
		try {
			smallMovementZ_ = nf_.parse(jTextField4.getText()).doubleValue();
		} catch(ParseException e) {
			// ignore if parsing fails
		}
	}//GEN-LAST:event_jTextField4ActionPerformed

	private void jTextField4focusLostHandlerJTF1(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField4focusLostHandlerJTF1
		try {
			smallMovementZ_ = nf_.parse(jTextField4.getText()).doubleValue();
		} catch(ParseException e) {
			// ignore if parsing fails
		}
	}//GEN-LAST:event_jTextField4focusLostHandlerJTF1

	private void jTextField5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField5ActionPerformed
		try {
			mediumMovementZ_ = nf_.parse(jTextField5.getText()).doubleValue();
		} catch(ParseException e) {
			// ignore if parsing fails
		}
	}//GEN-LAST:event_jTextField5ActionPerformed

	private void jTextField5focusLostHandlerJTF2(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField5focusLostHandlerJTF2
		try {
			mediumMovementZ_ = nf_.parse(jTextField5.getText()).doubleValue();
		} catch(ParseException e) {
			// ignore if parsing fails
		}
	}//GEN-LAST:event_jTextField5focusLostHandlerJTF2


	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton jButton1;
	private javax.swing.JButton jButton10;
	private javax.swing.JButton jButton11;
	private javax.swing.JButton jButton12;
	private javax.swing.JButton jButton13;
	private javax.swing.JButton jButton14;
	private javax.swing.JButton jButton15;
	private javax.swing.JButton jButton16;
	private javax.swing.JButton jButton17;
	private javax.swing.JButton jButton18;
	private javax.swing.JButton jButton19;
	private javax.swing.JButton jButton2;
	private javax.swing.JButton jButton3;
	private javax.swing.JButton jButton4;
	private javax.swing.JButton jButton5;
	private javax.swing.JButton jButton6;
	private javax.swing.JButton jButton7;
	private javax.swing.JButton jButton8;
	private javax.swing.JButton jButton9;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel10;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JLabel jLabel8;
	private javax.swing.JLabel jLabel9;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JTextField jTextField1;
	private javax.swing.JTextField jTextField2;
	private javax.swing.JTextField jTextField3;
	private javax.swing.JTextField jTextField4;
	private javax.swing.JTextField jTextField5;
	// End of variables declaration//GEN-END:variables

}
