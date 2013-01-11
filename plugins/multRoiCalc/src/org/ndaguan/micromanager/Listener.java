/**

 * 
 */
package org.ndaguan.micromanager;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Toolbar;

import java.awt.Event;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.utils.MMScriptException;

public class Listener implements MouseListener, MouseMotionListener,KeyListener,ActionListener{

	private MMStudioMainFrame gui_;
	private ImageCanvas canvas_;
	private int lastX_, lastY_;
	private static boolean isRunning_ = false;
	private static Listener instance_;
	private ListenerAction listenerAction_; 

	public Listener(MMStudioMainFrame gui,Function function) {
		gui_ =  gui;
		listenerAction_ = ListenerAction.getInstance(function);
	}
	public static Listener getInstance() {
		return instance_;
	}
	public static Listener getInstance(MMStudioMainFrame gui,
			Function function) {
		if(instance_ == null)
			instance_ = new Listener(gui,function);		
		return instance_;
	}
	public void start (String acqName) throws MMScriptException  {
		if (isRunning_)
			return;

		ImagePlus imageplus = gui_.getAcquisition(acqName).getAcquisitionWindow().getHyperImage();
		if(imageplus != null){
			attach (imageplus.getWindow());
			isRunning_ = true;
		}

	}

	public void stop() {
		if (canvas_ != null) {
			canvas_.removeMouseListener(this);
			canvas_.removeMouseMotionListener(this);
			canvas_.removeKeyListener(this);
		}
		isRunning_ = false;
	}

	public boolean isRunning() {
		return isRunning_;
	}

	public void attach(ImageWindow win) {
		if (win == null)
			return;
		if (isRunning_)
			return;
		canvas_ = win.getCanvas();
		canvas_.addMouseListener(this);
		canvas_.addMouseMotionListener(this);
		canvas_.addKeyListener(this);
	}


	public void mousePressed(MouseEvent e) {
		if (Toolbar.getInstance() == null)
			return;
		if ((e.getModifiers() & Event.META_MASK) != 0)  
			return;
		if (Toolbar.getToolId() != Toolbar.RECTANGLE)
			return;

		int x = e.getX();
		int y = e.getY();
		lastX_ = canvas_.offScreenX(x);
		lastY_ = canvas_.offScreenY(y);
		listenerAction_.setFocusdRoi(new Point(lastX_,lastY_));
	}

	public void mouseDragged(MouseEvent e) {
		if ((e.getModifiers() & Event.META_MASK) != 0)  
			return;
		if (Toolbar.getInstance() == null)
			return;
		if (Toolbar.getToolId() != Toolbar.RECTANGLE)
			return;

		int x = e.getX();
		int y = e.getY();
		int cX = canvas_.offScreenX(x);
		int cY = canvas_.offScreenY(y);	
		listenerAction_.moveFocusdRoi(cX - lastX_,cY - lastY_);  
		lastX_ = cX;
		lastY_ = cY;

	} 
	public void mouseClicked(MouseEvent e) {} 
	public void mouseReleased(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}

	public void keyPressed(KeyEvent e) {
		if(!e.isControlDown())
			return;			
		Rectangle rectangle = canvas_.getImage().getRoi().getBounds();

		if(e.getKeyCode() == KeyEvent.VK_A){			
			listenerAction_.addRoi(rectangle);
		}
		if(e.getKeyCode() == KeyEvent.VK_D){
			listenerAction_.deleteRoi();
		}
		if(e.getKeyCode() == KeyEvent.VK_S){
			listenerAction_.selectRoiAsReference();
		}

	}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent arg0) {}

 
	public void actionPerformed(ActionEvent e) {
		String cmdString = "";
		if(e.getSource().getClass().toString().equals("class javax.swing.JButton"))
			cmdString = ((javax.swing.JButton)e.getSource()).getToolTipText();
		if(e.getSource().getClass().toString().equals("class javax.swing.JMenuItem"))
			cmdString = ((javax.swing.JMenuItem)e.getSource()).getToolTipText();
		switch(cmdString){
		case "Live view":
			listenerAction_.live();
			break;	
		case "Calibrate":
			listenerAction_.calibrate();
			break;
		case "Mutil-ACQ with the default preferences":
			listenerAction_.multiAcq();
			break;
		case "Preferences":
			listenerAction_.showPreferencesDialog();
			break;
		case "Install callback":
			listenerAction_.installCallback();
			break;
		case "Set up":
			listenerAction_.setScale();
			break;
		case "Rectangle tool for select a ROI":
			if (Toolbar.getInstance() == null)
				return;

			Toolbar.getInstance().setTool(Toolbar.RECTANGLE);
			break;
		case "Hand tool for moving the xyStage":
			if (Toolbar.getInstance() == null)
				return;
			Toolbar.getInstance().setTool(Toolbar.HAND);
			break;
		 
		
		}

	}
	

}
