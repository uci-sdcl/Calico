/*******************************************************************************
 * Copyright (c) 2013, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * None of the name of the Regents of the University of California, or the names of its
 * contributors may be used to endorse or promote products derived from this software without specific
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package calico;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.vfs.*;
import org.apache.commons.vfs.cache.*;
import org.apache.commons.vfs.impl.*;
import org.apache.commons.vfs.provider.*;
import org.apache.commons.vfs.provider.local.*;
import org.apache.commons.vfs.provider.ram.*;
import org.apache.commons.vfs.provider.jar.*;
import org.apache.commons.vfs.provider.url.*;
import org.apache.commons.vfs.provider.gzip.*;
import org.apache.commons.vfs.provider.temp.*;

import org.apache.log4j.Logger;

public class CalicoConfig {

	private static Logger logger = Logger.getLogger(CalicoConfig.class
			.getName());

	private static Properties configFile = null;

	// used to match color strings
	private static Pattern hexcolor6 = Pattern
			.compile("^#?([0-9A-F]{2})([0-9A-F]{2})([0-9A-F]{2})$");

	/**
	 * This is the main function, this is run to setup all the defaults and
	 * whatnot
	 */
	public static void setup() {

		// Check if they passed the config file via the command line, if they
		// did, then load it
		String cfgFilePath = System.getProperty("cfgfile", "server.properties");

		try {
			getConfigField("canvas.max_hold_duration");
		} catch (NoSuchFieldException nsfe) {

		}

		try {

			// COptions.fsManager = VFS.getManager();

			DefaultLocalFileProvider localProvider = new DefaultLocalFileProvider();

			COptions.fs = new DefaultFileSystemManager();
			DefaultFileReplicator replicator = new DefaultFileReplicator();
			((DefaultFileSystemManager) COptions.fs)
					.setReplicator(new PrivilegedFileReplicator(replicator));
			((DefaultFileSystemManager) COptions.fs)
					.setTemporaryFileStore(replicator);
			((DefaultFileSystemManager) COptions.fs).addProvider("file",
					localProvider);
			((DefaultFileSystemManager) COptions.fs)
					.setDefaultProvider(localProvider);
			((DefaultFileSystemManager) COptions.fs)
					.setFilesCache(new DefaultFilesCache());

			((DefaultFileSystemManager) COptions.fs).addProvider("jar",
					new JarFileProvider());
			((DefaultFileSystemManager) COptions.fs).addProvider("ram",
					new RamFileProvider());
			((DefaultFileSystemManager) COptions.fs).addProvider("gz",
					new GzipFileProvider());
			((DefaultFileSystemManager) COptions.fs).addProvider("tmp",
					new TemporaryFileProvider());

			((DefaultFileSystemManager) COptions.fs).init();

			((DefaultFileSystemManager) COptions.fs).setBaseFile(new File(
					System.getProperty("user.dir")));

			// logger.info("CWD: "+System.getProperty("user.dir"));
			// COptions.fsCWD =
			// COptions.fs.resolveFile(System.getProperty("user.dir"));
		} catch (Exception e) {
			logger.error("Could not make file system manager");
			e.printStackTrace();
		}

		try {
			FileObject propfile = COptions.fs.resolveFile(cfgFilePath);
			// configFile = loadPropertyFile(cfgFilePath);//new
			// Properties(defaultProps);
			configFile = loadPropertyFile(propfile.getContent()
					.getInputStream(), new Properties());
			logger.info("Loading config file: " + propfile.getURL().toString());

			setupConfigClass();
		} catch (Exception e) {
			logger.error("Failed to load config file, using default settings.");
		}

	}

	private static Class<?> getConfigClass(Class<?>[] classes, String name) {
		for (int j = 0; j < classes.length; j++) {
			if (classes[j].getSimpleName().equals(name)) {
				return classes[j];
			}
		}
		return null;
	}

	public static Field getConfigField(String fieldpath)
			throws NoSuchFieldException {
		String[] pathParts = fieldpath.split("\\.");
		String fieldName = pathParts[pathParts.length - 1];

		Class<?> rootClass = COptions.class;

		for (int i = 0; i < pathParts.length - 1; i++) {
			rootClass = getConfigClass(rootClass.getClasses(), pathParts[i]);
		}
		return rootClass.getField(fieldName);
	}

	public static void setconfig_int(Field field, int value)
			throws IllegalAccessException {
		field.setInt(null, value);
	}

	public static void setconfig_float(Field field, float value)
			throws IllegalAccessException {
		field.setFloat(null, value);
	}

	public static void setconfig_long(Field field, long value)
			throws IllegalAccessException {
		field.setLong(null, value);
	}

	public static void setconfig_double(Field field, double value)
			throws IllegalAccessException {
		field.setDouble(null, value);
	}

	public static void setconfig_boolean(Field field, boolean value)
			throws IllegalAccessException {
		field.setBoolean(null, value);
	}

	public static void setconfig_Object(Field field, Object value)
			throws IllegalAccessException {
		field.set(null, value);
	}

	/**
	 * This will take all the variables from the property file, and load it to
	 * the class objects
	 */
	private static void setupConfigClass() {
		Class<?>[] configClasses = COptions.class.getDeclaredClasses();

		for (int i = 0; i < configClasses.length; i++) {
			processClassConfig(configClasses[i]);
		}
	}

	public static void setConfigVariable(String configVariable, String value)
			throws NoSuchFieldException {
		Field field = getConfigField(configVariable);

		String fieldtype = field.getType().getCanonicalName();
		try {
			if (fieldtype.equals("long")) {
				setconfig_long(field, Long.decode(value).longValue());
			} else if (fieldtype.equals("int")) {
				setconfig_int(field, Integer.parseInt(value));
			} else if (fieldtype.equals("float")) {
				setconfig_float(field, Float.parseFloat(value));
			} else if (fieldtype.equals("boolean")) {
				setconfig_boolean(field, Boolean.parseBoolean(value));
			} else if (fieldtype.equals("double")) {
				setconfig_double(field, Double.parseDouble(value));
			} else if (fieldtype.equals("java.lang.String")) {
				setconfig_Object(field, value);
			} else if (fieldtype.equals("java.awt.Color")) {
				setconfig_Object(field, parseColorString(value));
			}
		} catch (IllegalAccessException e) {
			throw new NoSuchFieldException();
		}

	}

	private static void processClassConfig(Class<?> className) {
		// logger.debug("CONFIG ["+className.getCanonicalName()+"]");

		Field[] fields = className.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];

			String fieldname = className.getCanonicalName() + "."
					+ field.getName();
			fieldname = fieldname.replaceFirst(COptions.class
					.getCanonicalName()
					+ ".", "");
			String fieldtype = field.getType().getCanonicalName();
			// logger.debug(fieldname+"|"+fieldtype);
			try {
				String value = System.getProperty(fieldname, configFile
						.getProperty(fieldname));// configFile.getProperty(fieldname);

				if (value == null) {
					// Failed, so ignore it
				} else if (fieldtype.equals("long")) {
					field.setLong(null, Long.decode(value).longValue());
				} else if (fieldtype.equals("int")) {
					field.setInt(null, Integer.parseInt(value));
				} else if (fieldtype.equals("float")) {
					field.setFloat(null, Float.parseFloat(value));
				} else if (fieldtype.equals("boolean")) {
					field.setBoolean(null, Boolean.parseBoolean(value));
				} else if (fieldtype.equals("double")) {
					field.setDouble(null, Double.parseDouble(value));
				} else if (fieldtype.equals("java.lang.String")) {
					field.set(null, value);
				} else if (fieldtype.equals("java.awt.Color")) {
					field.set(null, parseColorString(value));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}// /

		Class<?>[] configClasses = className.getDeclaredClasses();
		for (int i = 0; i < configClasses.length; i++) {
			processClassConfig(configClasses[i]);
		}
	}// /////

	public static HashMap<String, Object> getConfigHashMap() {
		Class<?>[] configClasses = COptions.class.getDeclaredClasses();

		HashMap<String, Object> hashmap = new HashMap<String, Object>();

		for (int i = 0; i < configClasses.length; i++) {
			hashmap.put(configClasses[i].getName(),
					class2hashmap(configClasses[i]));
		}

		// Specialty ones

		return hashmap;
	}

	private static HashMap<String, Object> class2hashmap(Class<?> className) {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();

		Field[] fields = className.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			try {
				hashmap.put(field.getName(), field.get(null));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}// /

		Class<?>[] configClasses = className.getDeclaredClasses();
		for (int i = 0; i < configClasses.length; i++) {
			hashmap.put(configClasses[i].getName(),
					class2hashmap(configClasses[i]));
		}

		return hashmap;
	}

	private static Color parseColorString(String color) {
		color = color.toUpperCase().trim();

		// Look for a 6 character hex code
		Matcher hx6 = hexcolor6.matcher(color);
		if (hx6.find()) {
			return new Color(Integer.parseInt(hx6.group(1), 16), Integer
					.parseInt(hx6.group(2), 16), Integer.parseInt(hx6.group(3),
					16));
		}

		return null;
	}

	public static Properties loadPropertyFile(String confFile, Properties def)
			throws FileNotFoundException, IOException {
		Properties prop = new Properties(def);
		prop.load(new FileInputStream(new File(confFile)));
		return prop;
	}

	public static Properties loadPropertyFile(String confFile)
			throws FileNotFoundException, IOException {
		return loadPropertyFile(confFile, new Properties());
	}

	public static Properties loadPropertyFile(InputStream instream,
			Properties def) throws FileNotFoundException, IOException {
		Properties prop = new Properties(def);
		prop.load(instream);
		return prop;
	}

	public static String getconfig_String(String key) {
		return System.getProperty(key, configFile.getProperty(key));
	}

	public static int getconfig_int(String key) {
		return Integer.parseInt(System.getProperty(key, configFile
				.getProperty(key)));
	}

	public static long getconfig_long(String key) {
		return Long
				.decode(System.getProperty(key, configFile.getProperty(key)))
				.longValue();
	}

	public static float getconfig_float(String key) {
		return Float.parseFloat(System.getProperty(key, configFile
				.getProperty(key)));
	}

	public static double getconfig_double(String key) {
		return Double.parseDouble(System.getProperty(key, configFile
				.getProperty(key)));
	}

	public static boolean getconfig_boolean(String key) {
		return Boolean.parseBoolean(System.getProperty(key, configFile
				.getProperty(key)));
	}

}
