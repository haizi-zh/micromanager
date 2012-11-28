package org.zephyre.micromanager;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class TextAreaQueue extends JTextArea {
	private ConcurrentLinkedDeque<String> bufferedEntries_;
	// Maximal number of entries
	private int bufferSize_;
	private StringBuilder sb_;
	// A routine to update the content of JTextArea
	private final Runnable updateProc_;

	public TextAreaQueue(int rows, int cols) {
		super(rows, cols);
		bufferedEntries_ = new ConcurrentLinkedDeque<String>();
		bufferSize_ = 10;
		updateProc_ = new Runnable() {
			@Override
			public void run() {
				TextAreaQueue.this.setText(sb_.toString());
			}
		};
	}

	@Override
	public void setText(String t) {
		synchronized (bufferedEntries_) {
			clear();
			addEntry(t);
		}
	}

	/**
	 * Clear all the entries.
	 */
	public void clear() {
		bufferedEntries_.clear();
		updateContent();
	}

	private void updateContent() {
		sb_ = new StringBuilder();

		synchronized (bufferedEntries_) {
			Iterator<String> it = bufferedEntries_.iterator();
			while (it.hasNext()) {
				sb_.append(it.next());
				sb_.append('\n');
			}
		}
		SwingUtilities.invokeLater(updateProc_);
	}

	/**
	 * Add an entry to the area
	 * 
	 * @param t
	 */
	public void addEntry(String t) {
		synchronized (bufferedEntries_) {
			if (bufferedEntries_.size() == bufferSize_)
				bufferedEntries_.removeFirst();
			bufferedEntries_.addLast(t);
		}
		updateContent();
	}
}
