package me.laurens.FPL.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class Config {
	
	private Properties prop;
	
	public Config(String path) {
		
		File file = new File(path + "config.properties");
		prop = new Properties();
		
		if (!file.exists()) {
			setupConfig(path);
		}
		
		try (InputStream input = new FileInputStream(path + "config.properties")) {
			
			prop.load(input);		
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	public String getValue(String key) {
		
		return prop.getProperty(key);
		
	}
	
	private void setupConfig(String path) {
		
		try (OutputStream output = new FileOutputStream(path + "config.properties")) {
			
			prop.setProperty("db.host", "localhost");
			prop.setProperty("db.port", "3306");
			prop.setProperty("db.username", "username");
			prop.setProperty("db.password", "password");
			
			prop.store(output, path);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
