package org.zephyre.micromanager;

import javax.swing.JFrame;

import mmcorej.CMMCore;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class AcqCallback implements MMPlugin {
	private JFrame mainFrame_;
	private ScriptInterface gui_;
	private CMMCore core_;

	public static String menuName = "Acquisition Callback";
	public static String tooltipDescription = "AcqCallback demo";

	@Override
	public void dispose() {
		if (mainFrame_ != null)
			mainFrame_.dispose();
		mainFrame_ = null;
	}

	@Override
	public void setApp(ScriptInterface app) {
		gui_ = app;
		core_ = app.getMMCore();
		if (mainFrame_ == null)
			mainFrame_ = new AcqCallbackFrame(gui_);
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub
		mainFrame_.setVisible(true);
	}

	@Override
	public void configurationChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCopyright() {
		// TODO Auto-generated method stub
		return null;
	}

}
