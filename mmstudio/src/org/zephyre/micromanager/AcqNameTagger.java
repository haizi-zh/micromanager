package org.zephyre.micromanager;

import ij.IJ;

import java.util.HashMap;

import mmcorej.TaggedImage;

import org.json.JSONException;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.TaggedImageAnalyzer;

public class AcqNameTagger extends TaggedImageAnalyzer {
	private static HashMap<String, AcqNameTagger> hm_ = new HashMap<String, AcqNameTagger>();
	private String acqName_;

	private AcqNameTagger(String name) {
		super();
		hm_.put(name, this);
		acqName_ = name;
	}

	public static AcqNameTagger getInstance(String name) {
		AcqNameTagger instance = hm_.get(name);
		if (instance == null)
			instance = new AcqNameTagger(name);
		return instance;
	}

	private long cnt_ = 0;

	@Override
	protected void analyze(TaggedImage taggedImage) {
		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON)
			return;

		try {
			taggedImage.tags.put("AcqName", acqName_);
//			IJ.log(String.format("AcqAnalyzer: %d | Thread: %s / %d | AcqName: %s", cnt_,
//					Thread.currentThread().getName(), Thread.currentThread()
//							.getId(), acqName_));
			cnt_++;
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
