package org.zephyre.mloader;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.ScriptInterface;

public class MLoaderFrame extends JFrame {
	private JTextField pathField_;
	private JTextField classField_;

	public MLoaderFrame() {
		this.getContentPane().add(initMainPanel());
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo(null);
	}

	private JPanel initMainPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(createCenterPanel());
		panel.add(createBtnBox(), BorderLayout.SOUTH);
		return panel;
	}

	private JPanel createCenterPanel() {
		// Pref
		Preferences pref = Preferences.userNodeForPackage(MLoaderFrame.class);
		String classPath = pref.get("ClassPath", "");
		String className = pref.get("ClassName", "");

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(8, 8, 8, 8);
		c.fill = GridBagConstraints.HORIZONTAL;

		c.anchor = GridBagConstraints.LINE_END;
		JLabel pathLabel = new JLabel("Path:");
		panel.add(pathLabel, c);

		c.gridx++;
		c.weightx = 1;
		pathField_ = new JTextField(classPath);
		Dimension dim = new Dimension(480, pathField_.getPreferredSize().height);
		pathField_.setMinimumSize(dim);
		pathField_.setPreferredSize(dim);
		panel.add(pathField_, c);
		c.weightx = 0;

		c.gridx++;
		JButton browseBtn = new JButton("Browse");
		browseBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (fc.showOpenDialog(MLoaderFrame.this) == JFileChooser.APPROVE_OPTION) {
					pathField_.setText(fc.getSelectedFile().toString());
				}
			}

		});
		panel.add(browseBtn);

		c.gridx = 0;
		c.gridy = 1;
		panel.add(new JLabel("Class:"), c);

		c.gridx++;
		c.gridwidth = 2;

		classField_ = new JTextField(className);
		panel.add(classField_, c);

		return panel;
	}

	private JPanel createBtnBox() {
		JPanel panel = new JPanel();
		panel.add(Box.createHorizontalGlue());
		JButton okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				URLClassLoader loader = null;
				try {
					String classPath = pathField_.getText();
					String className = classField_.getText();
					loader = new URLClassLoader(
							new URL[] { (new File(classPath)).toURI().toURL() },
							Thread.currentThread().getContextClassLoader());
					Class<?> cls = loader.loadClass(className);
					Object instance = cls.newInstance();

					// 查找
					Method method = cls.getMethod("setApp",
							ScriptInterface.class);
					method.invoke(instance,
							new Object[] { MMStudioMainFrame.getInstance() });
					method = cls.getMethod("show");
					method.invoke(instance);

					// 如果成功，则保存相关信息
					Preferences pref = Preferences
							.userNodeForPackage(MLoaderFrame.class);
					pref.put("ClassPath", classPath);
					pref.put("ClassName", className);
				} catch (ClassNotFoundException | IOException
						| InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException
						| NoSuchMethodException | SecurityException e) {
					JOptionPane.showMessageDialog(MLoaderFrame.this,
							"Error loading the class!");
				} finally {
					if (loader != null)
						try {
							loader.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
			}

		});
		panel.add(okBtn);
		return panel;
	}
}
