package org.zephyre.micromanager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import mmcorej.TaggedImage;

import org.micromanager.api.DataProcessor;
import org.micromanager.api.ScriptInterface;

class AcqCallbackFrame extends JFrame {
	private boolean registered_;
	private ScriptInterface gui_;
	private DataProcessor<TaggedImage> processor_;

	AcqCallbackFrame(ScriptInterface gui) {
		gui_ = gui;
		JPanel basicPanel = new JPanel();
		basicPanel.setLayout(new BoxLayout(basicPanel, BoxLayout.X_AXIS));
		basicPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!registered_) {
					processor_ = new AcqAnalyzer(gui_);
					processor_.setName("AcqAnalyzer");
					gui_.getAcquisitionEngine().addImageProcessor(processor_);
					registered_ = true;
				}
			}
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (registered_) {
					processor_.requestStop();
					gui_.getAcquisitionEngine()
							.removeImageProcessor(processor_);
					registered_ = false;
				}
			}
		});

		basicPanel.add(okButton);
		basicPanel.add(Box.createHorizontalStrut(16));
		basicPanel.add(cancelButton);
		getContentPane().add(basicPanel);

		setDefaultCloseOperation(HIDE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
	}
}
