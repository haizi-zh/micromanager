package org.zephyre.mloader;

import ij.IJ;

import java.util.concurrent.TimeUnit;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class MLoaderTest implements MMPlugin {

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setApp(ScriptInterface app) {
		// TODO Auto-generated method stub

	}

	@Override
	public void show() {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < 10; i++) {
					test();
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		})).start();
	}
	
	private void test() {
		IJ.log("Nong sm4 daguan");
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
