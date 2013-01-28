/**

 * 
 */
package org.ndaguan.study;

import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Toolbar;

import java.awt.Event;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import mmcorej.CMMCore;

import org.micromanager.MMStudioMainFrame;

/**
 * @author OD
 *
 */
public class MyListener implements MouseListener, MouseMotionListener {

	private MMStudioMainFrame gui_;
	private std_MyGUI MyGUI_;
	private ImageCanvas canvas_;
	private static boolean isRunning_ = false;
	private int lastX_, lastY_;

	private boolean bDragged;

	public MyListener(MMStudioMainFrame gui,std_MyGUI MyGUI) {
		gui_ = gui;
		MyGUI_ = MyGUI;
	}

	public void start () {
		if (isRunning_)
			return;
		isRunning_ = true;
		attach (gui_.getImageWin());
	}

	public void stop() {
		if (canvas_ != null) {
			canvas_.removeMouseListener(this);
			canvas_.removeMouseMotionListener(this);
		}
		isRunning_ = false;
	}

	public boolean isRunning() {
		return isRunning_;
	}

	/*
	 * Attached a MouseLisetener and MouseMotionListener to the Live Window
	 */
	public void attach(ImageWindow win) {
		if (win == null)
			return;
		if (!isRunning_)
			return;
		canvas_ = win.getCanvas();
		canvas_.addMouseListener(this);
		canvas_.addMouseMotionListener(this);
	}

	public void mouseClicked(MouseEvent e) {
		// TODO: respond when a specific Tool is chosen and double click
		if (Toolbar.getInstance() == null)
			return;
		if (Toolbar.getToolId() != Toolbar.HAND)
			return;
		if ((e.getModifiers() & Event.META_MASK) != 0 )
			return;
		int nc=   e.getClickCount(); 
		if( nc < 2) 
			return;          

		int x = e.getX();
		int y = e.getY();
		int cX = canvas_.offScreenX(x);
		int cY = canvas_.offScreenY(y);      
		//showPopMenu(e,cX,cY);
	} 

	public void mousePressed(MouseEvent e) {
		
		if (Toolbar.getInstance() == null)
			return;
		if (Toolbar.getToolId() != Toolbar.RECTANGLE)
			return;
		
		int x = e.getX();
		int y = e.getY();
		lastX_ = canvas_.offScreenX(x);
		lastY_ = canvas_.offScreenY(y);
	}

	public void mouseDragged(MouseEvent e) {
		if ((e.getModifiers() & Event.META_MASK) != 0) // right click: ignore
			return;
		if (Toolbar.getInstance() == null)
			return;
		if (Toolbar.getToolId() != Toolbar.RECTANGLE)
			return;
		bDragged = true;
	} 

	public void mouseReleased(MouseEvent e) {
		// Get coordinates of event
		if(!bDragged)
			return;

		int x = e.getX();
		int y = e.getY();
		int cX = canvas_.offScreenX(x);
		int cY = canvas_.offScreenY(y);

		// calculate needed relative movement

		Rectangle rt = new Rectangle();
		rt.x = lastX_;
		rt.y = lastY_;
		rt.width =  cX - lastX_;
		rt.height = cY - lastY_;
		//addNewRoi(rt);
		bDragged = false;
	}
	public void mouseExited(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}

}
