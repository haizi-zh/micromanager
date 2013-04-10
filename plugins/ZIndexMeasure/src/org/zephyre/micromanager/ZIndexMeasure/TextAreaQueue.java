package org.zephyre.micromanager.ZIndexMeasure;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * TextAreaQueue extends JTextArea class, providing additional features.
 * It contains a certain amount of lines. While the added lines excesses 
 * such number, the former ones will be flushed out.
 * @author Zephyre
 *
 */
public class TextAreaQueue extends JTextArea {
	private ConcurrentLinkedDeque<String> bufferedEntries_;
	// Maximal number of entries
	private volatile int maxLines_;
	private String contents_;
	// A routine to update the content of JTextArea
	private final Runnable updateProc_;

	/**
	 * @see JTextArea
	 * @param rows
	 * @param cols
	 */
	public TextAreaQueue(int rows, int cols) {
		super(rows, cols);
		bufferedEntries_ = new ConcurrentLinkedDeque<String>();
		maxLines_ = 10;
		updateProc_ = new Runnable() {
			@Override
			public void run() {
				TextAreaQueue.this.setText(contents_);
			}
		};
	}

	public void setMaxLines(int val) {
		maxLines_ = val;
	}

	public int getMaxLines() {
		return maxLines_;
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
		StringBuilder sb = new StringBuilder();
		synchronized (bufferedEntries_) {
			Iterator<String> it = bufferedEntries_.iterator();
			while (it.hasNext()) {
				sb.append(it.next());
				sb.append('\n');
			}
		}
		contents_ = sb.toString();
		SwingUtilities.invokeLater(updateProc_);
	}

	/**
	 * Add an entry to the area
	 * 
	 * @param t
	 */
	public void addEntry(String t) {
		synchronized (bufferedEntries_) {
			while (bufferedEntries_.size() >= maxLines_) {
				bufferedEntries_.removeFirst();
			}
			bufferedEntries_.addLast(t);
		}
		updateContent();
	}
}
