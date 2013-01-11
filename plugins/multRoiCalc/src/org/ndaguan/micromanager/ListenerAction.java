package org.ndaguan.micromanager;

import java.awt.Point;
import java.awt.Rectangle;

public class ListenerAction {

	private static ListenerAction instance_;
	private Function function_;
	public ListenerAction(Function function) {
		function_ = function;
	}

	public static ListenerAction getInstance(Function function) {
		if(instance_ == null)
			instance_ = new ListenerAction(function);		
		return instance_;
	}


	public synchronized void setFocusdRoi(Point point) {
		function_.setFocusdRoi(point);
	}

	public synchronized void moveFocusdRoi(int dx, int dy) {
		function_.moveFocusdRoi(dx, dy);
	}

	public synchronized void addRoi(Rectangle rectangle) {
		function_.addRoi(rectangle);
	}
	
	public synchronized void deleteRoi() {
		function_.deleteRoi();
	}

	public void selectRoiAsReference() {
		function_.selectRoiAsReference();
	}

	public void live() {
		(new Thread(new Runnable() { @Override public void run() {
			function_.liveView();
		}
		})).start();
	}

	public void calibrate() {
		(new Thread(new Runnable() { @Override public void run() {
			function_.calibrate();
		}
		})).start();
	}

	public void multiAcq() {
		(new Thread(new Runnable() { @Override public void run() {
			function_.multiAcq();
		}
		})).start();
	}

	public void installCallback() {
		(new Thread(new Runnable() { @Override public void run() {
			function_.installCallback();
		}
		})).start();
	}

	public void setScale() {
		(new Thread(new Runnable() { @Override public void run() {
			function_.setScale();
		}
		})).start();
	}

	public void showPreferencesDialog() {
		(new Thread(new Runnable() { @Override public void run() {
			function_.showPreferencesDialog();
		}
		})).start();
	}
}
