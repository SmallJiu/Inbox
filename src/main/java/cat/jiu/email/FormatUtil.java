package cat.jiu.email;

import java.awt.Font;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import cat.jiu.email.util.JsonUtil;

public class FormatUtil {
	static final JsonParser parse = new JsonParser();
	static final Filter JSON = new Filter("json", "(.json) Json File");
	
	public static void main(String[] args) {
		if(args.length >= 1) {
			File jsonFile = new File(args[0]);
			if(!jsonFile.exists()) {
				System.err.println("File not found!");
				return;
			}
			if(jsonFile.isDirectory()) {
				System.err.println("File cannot be Directory!");
				return;
			}
			try {
				long currenTime = System.currentTimeMillis();
				String writePath = jsonFile.getPath().substring(0, jsonFile.getPath().lastIndexOf(".")) + "-format.json";
				
				JsonUtil.toJsonFile(writePath, parse.parse(new FileReader(jsonFile)), true);
				
				System.out.println("Format successful (took " + (System.currentTimeMillis() - currenTime) + " ms)");
			}catch(JsonIOException | JsonSyntaxException | IOException e) {
				e.printStackTrace();
				return;
			}
		}else {
			JFrame main = new JFrame("Json Format");
			main.setBounds(0, 0, 660, 135);
			main.setLocationRelativeTo(null);
			main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			main.setLayout(null);
			
			JTextField pathField = new JTextField();
			pathField.setBounds(120, 5, 998, 40);
			pathField.setFont(new Font(null, 0, 20));
			main.add(pathField);
			
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.addChoosableFileFilter(JSON);
			
			JButton chooserFile = new JButton("File");
			chooserFile.setBounds(55, 5, 60, 40);
			chooserFile.setFont(new Font(null, 0, 17));
			chooserFile.addActionListener(e -> {
				fileChooser.showOpenDialog(main);
				if(fileChooser.getSelectedFile() != null) {
					pathField.setText(fileChooser.getSelectedFile().getPath());
				}
			});
			main.add(chooserFile);
			
			JLabel pathInfo = new JLabel("Path: ");
			pathInfo.setBounds(2, 5, 450, 40);
			pathInfo.setFont(new Font(null, 0, 20));
			main.add(pathInfo);
			
			JTextField info = new JTextField();
			info.setBounds(50, 50, 500, 40);
			info.setFont(new Font(null, 0, 20));
			info.setEditable(false);
			main.add(info);
			
			JLabel infoInfo = new JLabel("Info: ");
			infoInfo.setBounds(5, 50, 50, 40);
			infoInfo.setFont(new Font(null, 0, 20));
			main.add(infoInfo);
			
			JButton start = new JButton("Format");
			start.setBounds(555, 50, 83, 40);
			start.addActionListener(event->{
				String path = pathField.getText();
				if(path == null || path.isEmpty()) {
					info.setText("Path cannot be EMPTY!");
					return;
				}
				
				File jsonFile = new File(path);
				if(!jsonFile.exists()) {
					info.setText("File not found!");
					return;
				}
				if(jsonFile.isDirectory()) {
					info.setText("File cannot be Directory!");
					return;
				}
				try {
					long currenTime = System.currentTimeMillis();
					String writePath = jsonFile.getPath().substring(0, jsonFile.getPath().lastIndexOf(".")) + "-format.json";
					
					JsonUtil.toJsonFile(writePath, parse.parse(new FileReader(jsonFile)), true);
					
					info.setText("Format successful (took " + (System.currentTimeMillis() - currenTime) + " ms)");
				}catch(JsonIOException | JsonSyntaxException | IOException e) {
					e.printStackTrace();
					info.setText(e.getMessage());
					return;
				}
			});
			
			main.add(start);
			
			main.setVisible(true);
		}
	}
	
	static class Filter extends FileFilter implements java.io.FileFilter{
		final String end;
		final String info;

		public Filter(String end, String info) {
			this.end = end;
			this.info = info;
		}
		
		@Override
		public boolean accept(File file) {
			String fileName = file.getName();
			if (fileName.toLowerCase().endsWith(this.end.toLowerCase())) {
				return true;
			}else {
				return false;
			}
		}
		
		@Override
		public String getDescription() {
			return this.info;
		}
	}
}
