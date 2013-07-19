package org.ndaguan.micromanager.mmtracker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;

public class LoginDailog extends JFrame {
	private static final long serialVersionUID = 1L;
	 
	private JFrame instance_;
 

	private ActionListener DialogListener;
 
 
	public static void main(String[] arg){

		LoginDailog pre = new LoginDailog();
		pre.setVisible(true);

	}
	 
	public LoginDailog() {
		initialize();
	}
 

	public String[] getUsers() {
		try {
			File loginDataFile = new File(System.getProperty("user.home")+"/MMTracker/users.txt");
			if(!loginDataFile.exists())
				return initUsers();
			BufferedReader in;
			in = new BufferedReader(new FileReader(loginDataFile));
			String line;
			if((line = in.readLine()) == null)
			{
				in.close();
				return null;
			}
			String[] users = line.split(","); 
			in.close();
			if(users.length <=0 ){
				users = initUsers();
			}
			return users;
		} catch (IOException e) {
			MMT.logError("read user data false");
			return null;
		} 
	}

	public String[] initUsers(){
		try {
			File dir = new File(System.getProperty("user.home"),"MMTracker");
			if(!dir.isFile())
				dir.mkdirs();

			File loginDataFile = new File(System.getProperty("user.home")+"/MMTracker/users.txt");
			FileWriter out = new FileWriter((loginDataFile)); 
			String sData = "";
			String[] newUsers = new String []{"luyue","lijinghua","luying","teng","Ray","Ray-Feedback","n~daguan"};
			for(int i=0;i<newUsers.length;i++){
				sData += newUsers[i] + ",";
			}
			out.write(sData);
			out.close(); 
			return newUsers;
		} catch (IOException e) {
			MMT.logError("save user data err");
			return null;
		}
	}
 
	private void initialize(){
		instance_  = this;
		String[] users = getUsers();
		JButton userBtn[] = new JButton[users.length];
		this.setTitle("Login...please select a user");
		this.setBounds(400, 200, 100*(users.length),80);
		this.setLayout(null);
		int x = 0;

		DialogListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				String user = e.getActionCommand();
				MMT.currentUser  = user;
				instance_.setVisible(false);
				MMTracker.getInstance().initialize();
			}
		};

		for(int i=0;i<users.length;i++){
			userBtn[i] = new JButton(users[i]);
			userBtn[i].setBounds(x,5, 100, 35);
			x +=100;
			getContentPane().add(userBtn[i]);
			userBtn[i].addActionListener(DialogListener);
		}
		
	}
}
