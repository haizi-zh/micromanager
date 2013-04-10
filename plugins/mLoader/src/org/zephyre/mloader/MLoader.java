package org.zephyre.mloader;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class MLoader implements MMPlugin {

	private ScriptInterface gui_;

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setApp(ScriptInterface app) {
		gui_ = app;
	}

	@Override
	public void show() {
		(new MLoaderFrame()).setVisible(true);
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
