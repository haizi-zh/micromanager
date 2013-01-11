package org.ndaguan.study;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

public class std_SwingComDemo {

	class Table_2TableModel extends AbstractTableModel {
		public final String[] COLUMN_NAMES = new String[] {};

		public int getRowCount() {
			return 0;
		}

		public int getColumnCount() {
			return COLUMN_NAMES.length;
		}

		public String getColumnName(int columnIndex) {
			return COLUMN_NAMES[columnIndex];
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			return null;
		}
	}

	private JTable table_2;

	private ButtonGroup buttonGroup_1 = new ButtonGroup();

	private ButtonGroup buttonGroup = new ButtonGroup();

	class Table_1TableModel extends AbstractTableModel {
		public final String[] COLUMN_NAMES = new String[] {};

		public int getRowCount() {
			return 0;
		}

		public int getColumnCount() {
			return COLUMN_NAMES.length;
		}

		public String getColumnName(int columnIndex) {
			return COLUMN_NAMES[columnIndex];
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			return null;
		}
	}

	class TableTableModel extends AbstractTableModel {
		public final String[] COLUMN_NAMES = new String[] {};

		public int getRowCount() {
			return 0;
		}

		public int getColumnCount() {
			return COLUMN_NAMES.length;
		}

		public String getColumnName(int columnIndex) {
			return COLUMN_NAMES[columnIndex];
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			return null;
		}
	}

	private JTable table_1;

	private JTable table;

	private JTree tree;

	private JSpinner spinner;

	private JComboBox comboBox;

	private JList list;

	private JFormattedTextField formattedTextField;

	private JTextArea textArea;

	private JPasswordField passwordField;

	private JTextField jTextField;

	private JFrame frame;

	/**
	 * Launch the application
	 * 
	 * @param args
	 */
	 public static void main(String args[]) {
		try {
			std_SwingComDemo window = new std_SwingComDemo();
			window.frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the application
	 */
	 public std_SwingComDemo() {
		 initialize();
	 }

	 /**
	  * Initialize the contents of the frame
	  */
	 private void initialize() {
		 frame = new JFrame();
		 frame.getContentPane().setLayout(null);
		 frame.setBounds(100, 100, 500, 375);
		 frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		 final JMenuBar menuBar = new JMenuBar();
		 frame.setJMenuBar(menuBar);

		 final JMenu menu_3 = new JMenu();
		 menu_3.setText("Menu Item");
		 menuBar.add(menu_3);

		 final JMenu menu_4 = new JMenu();
		 menu_4.setText("Menu Item");
		 menu_3.add(menu_4);

		 final JRadioButtonMenuItem radioButtonMenuItem_2 = new JRadioButtonMenuItem();
		 radioButtonMenuItem_2.setText("RadioButton");
		 menu_4.add(radioButtonMenuItem_2);

		 final JRadioButtonMenuItem radioButtonMenuItem_3 = new JRadioButtonMenuItem();
		 radioButtonMenuItem_3.setText("RadioButton");
		 menu_4.add(radioButtonMenuItem_3);

		 final JMenu menu_5 = new JMenu();
		 menu_5.setText("Menu Item");
		 menu_3.add(menu_5);

		 final JCheckBoxMenuItem checkBoxMenuItem_2 = new JCheckBoxMenuItem();
		 checkBoxMenuItem_2.setText("CheckBox");
		 menu_5.add(checkBoxMenuItem_2);

		 final JCheckBoxMenuItem checkBoxMenuItem_3 = new JCheckBoxMenuItem();
		 checkBoxMenuItem_3.setText("CheckBox");
		 menu_5.add(checkBoxMenuItem_3);

		 menu_3.addSeparator();

		 final JMenuItem menuItem_1 = new JMenuItem();
		 menuItem_1.setText("Menu Item");
		 menu_3.add(menuItem_1);

		 final JPopupMenu popupMenu = new JPopupMenu();
		 addPopup(frame.getContentPane(), popupMenu);

		 final JMenu menu = new JMenu();
		 menu.setText("Menu Item");
		 popupMenu.add(menu);

		 final JMenu menu_1 = new JMenu();
		 menu_1.setText("Menu Item");
		 menu.add(menu_1);

		 final JRadioButtonMenuItem radioButtonMenuItem = new JRadioButtonMenuItem();
		 radioButtonMenuItem.setText("RadioButton");
		 menu_1.add(radioButtonMenuItem);

		 final JRadioButtonMenuItem radioButtonMenuItem_1 = new JRadioButtonMenuItem();
		 radioButtonMenuItem_1.setText("RadioButton");
		 menu_1.add(radioButtonMenuItem_1);

		 final JMenu menu_2 = new JMenu();
		 menu_2.setText("Menu Item");
		 menu.add(menu_2);

		 final JCheckBoxMenuItem checkBoxMenuItem = new JCheckBoxMenuItem();
		 checkBoxMenuItem.setText("CheckBox");
		 menu_2.add(checkBoxMenuItem);

		 final JCheckBoxMenuItem checkBoxMenuItem_1 = new JCheckBoxMenuItem();
		 checkBoxMenuItem_1.setText("CheckBox");
		 menu_2.add(checkBoxMenuItem_1);

		 popupMenu.addSeparator();

		 final JMenuItem menuItem = new JMenuItem();
		 menuItem.setText("Menu Item");
		 popupMenu.add(menuItem);

		 final JTabbedPane tabbedPane = new JTabbedPane();
		 tabbedPane.setBounds(10, 43, 472, 274);
		 frame.getContentPane().add(tabbedPane);

		 final JPanel panel = new JPanel();
		 panel.setLayout(null);
		 tabbedPane.addTab("jpanel", null, panel, null);

		 final JToolBar toolBar = new JToolBar();
		 toolBar.setBounds(0, 10, 100, 20);
		 panel.add(toolBar);

		 final JButton button = new JButton();
		 button.setText("New JButton");
		 button.setBounds(0, 32, 101, 25);
		 panel.add(button);

		 final JCheckBox checkBox = new JCheckBox();
		 buttonGroup_1.add(checkBox);
		 checkBox.setText("JCheckBox");
		 checkBox.setBounds(0, 63, 103, 23);
		 panel.add(checkBox);

		 final JCheckBox checkBox_1 = new JCheckBox();
		 buttonGroup_1.add(checkBox_1);
		 checkBox_1.setText("JCheckBox");
		 checkBox_1.setBounds(0, 85, 103, 23);
		 panel.add(checkBox_1);

		 final JRadioButton radioButton = new JRadioButton();
		 buttonGroup.add(radioButton);
		 radioButton.setText("JRadioButton");
		 radioButton.setBounds(0, 114, 101, 23);
		 panel.add(radioButton);

		 final JRadioButton radioButton_1 = new JRadioButton();
		 buttonGroup.add(radioButton_1);
		 radioButton_1.setText("JRadioButton");
		 radioButton_1.setBounds(0, 143, 100, 23);
		 panel.add(radioButton_1);

		 final JToggleButton toggleButton = new JToggleButton();
		 toggleButton.setText("JToggleButton");
		 toggleButton.setBounds(0, 172, 101, 23);
		 panel.add(toggleButton);

		 final JLabel label = new JLabel();
		 label.setText("New JLabel");
		 label.setBounds(0, 201, 60, 15);
		 panel.add(label);

		 jTextField = new JTextField();
		 jTextField.setAction(null);
		 jTextField.setText("JTextField");
		 jTextField.setBounds(0, 222, 90, 21);
		 panel.add(jTextField);

		 passwordField = new JPasswordField();
		 passwordField.setBounds(111, 34, 114, 21);
		 panel.add(passwordField);

		 textArea = new JTextArea();
		 textArea.setText("JtextArea");
		 textArea.setBounds(110, 64, 115, 44);
		 panel.add(textArea);

		 formattedTextField = new JFormattedTextField();
		 formattedTextField.setText("JformattedTextField");
		 formattedTextField.setBounds(106, 115, 144, 21);
		 panel.add(formattedTextField);

		 final JSlider slider = new JSlider();
		 slider.setMinimum(1);
		 slider.setValue(5);
		 slider.setMaximum(10);
		 slider.setBounds(106, 159, 200, 24);
		 panel.add(slider);

		 final JScrollBar scrollBar = new JScrollBar();
		 scrollBar.setMinimum(1);
		 scrollBar.setVisibleAmount(1);
		 scrollBar.setValue(5);
		 scrollBar.setMaximum(10);
		 scrollBar.setBounds(116, 189, 16, 48);
		 panel.add(scrollBar);

		 list = new JList();
		 list.setModel(new DefaultComboBoxModel(new String[] { "1", "2", "3",
				 "4", "5", "6", "7", "8", "9", "10" }));
		 list.setCellRenderer(null);
		 list.setBounds(138, 189, 100, 54);
		 panel.add(list);

		 comboBox = new JComboBox();
		 comboBox.setModel(new DefaultComboBoxModel(new String[] { "1", "2",
				 "3", "4", "5", "6", "7", "8", "9", "10" }));
		 comboBox.setBounds(231, 33, 75, 23);
		 panel.add(comboBox);

		 spinner = new JSpinner();
		 spinner.setBounds(231, 65, 75, 20);
		 panel.add(spinner);

		 final JSeparator separator = new JSeparator();
		 separator.setBounds(321, 32, 100, 20);
		 panel.add(separator);

		 final JProgressBar progressBar = new JProgressBar();
		 progressBar.setMaximum(10);
		 progressBar.setBounds(312, 70, 150, 16);
		 panel.add(progressBar);

		 final JTextPane textPane = new JTextPane();
		 textPane.setText("JtextPane");
		 textPane.setBounds(312, 114, 84, 21);
		 panel.add(textPane);

		 final JEditorPane editorPane = new JEditorPane();
		 editorPane.setText("JeditorPane");
		 editorPane.setBounds(244, 195, 200, 48);
		 panel.add(editorPane);

		 final JScrollPane scrollPane = new JScrollPane();
		 tabbedPane.addTab("jscrollpane", null, scrollPane, null);

		 final JScrollPane scrollPane_1 = new JScrollPane();
		 scrollPane.setViewportView(scrollPane_1);

		 table_1 = new JTable(10, 10);
		 table_1.setGridColor(Color.BLACK);
		 table_1.setCellSelectionEnabled(true);
		 table_1.setCellEditor(null);
		 table_1.setModel(new Table_1TableModel());
		 // scrollPane_1.setViewportView(table_1.getTableHeader());
		 scrollPane_1.setViewportView(table_1);

		 final JSplitPane splitPane = new JSplitPane();
		 tabbedPane.addTab("jsplitpane", null, splitPane, null);

		 tree = new JTree();
		 splitPane.setLeftComponent(tree);

		 table = new JTable(10, 10);
		 table.setModel(new TableTableModel());
		 splitPane.setRightComponent(table.getTableHeader());
		 splitPane.setRightComponent(table);

		 final JDesktopPane desktopPane = new JDesktopPane();
		 tabbedPane.addTab("jdesktoppane", null, desktopPane, null);

		 final JOptionPane optionPane = new JOptionPane();
		 optionPane.setBounds(10, 10, 200, 90);
		 desktopPane.add(optionPane);

		 /*
		  * final JLabel label_1 = DefaultComponentFactory.getInstance()
		  * .createLabel("&New Label:"); label_1.setBounds(10, 118, 200, 25);
		  * desktopPane.add(label_1);
		  * 
		  * final JLabel label_2 = DefaultComponentFactory.getInstance()
		  * .createTitle("&New Title:"); label_2.setBounds(10, 149, 200, 25);
		  * desktopPane.add(label_2);
		  * 
		  * final JComponent separator_1 = DefaultComponentFactory.getInstance()
		  * .createSeparator("&New Separator"); separator_1.setBounds(10, 194,
		  * 200, 25); desktopPane.add(separator_1);
		  */

		 table_2 = new JTable(5, 5);
		 table_2.setModel(new Table_2TableModel());
		 table_2.setBounds(299, 34, 146, 140);
		 desktopPane.add(table_2);

		 final JInternalFrame internalFrame = new JInternalFrame();
		 internalFrame.setTitle("New JInternalFrame");
		 internalFrame.setVisible(true);
		 tabbedPane.addTab("jinternalFrame", null, internalFrame, null);

	 }

	 private static void addPopup(Component component, final JPopupMenu popup) {
		 component.addMouseListener(new MouseAdapter() {
			 public void mousePressed(MouseEvent e) {
				 if (e.isPopupTrigger())
					 showMenu(e);
			 }

			 public void mouseReleased(MouseEvent e) {
				 if (e.isPopupTrigger())
					 showMenu(e);
			 }

			 private void showMenu(MouseEvent e) {
				 popup.show(e.getComponent(), e.getX(), e.getY());
			 }
		 });
	 }
}