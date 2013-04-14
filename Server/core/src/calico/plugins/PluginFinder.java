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
package calico.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

class JarFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return (name.endsWith(".jar"));
	}
}

public class PluginFinder {

	// Parameters
	private static final Class<?>[] parameters = new Class[] { URL.class };

	private List<Class<?>> pluginCollection;

	public PluginFinder() {
		pluginCollection = new ArrayList<Class<?>>(5);
	}

	public void search(String directory) throws Exception {
		File dir = new File(directory);
		if (dir.isFile()) {
			return;
		}
		File[] files = dir.listFiles(new JarFilter());
		for (File f : files) {
			List<String> classNames = getClassNames(f.getAbsolutePath());
			for (String className : classNames) {
				// Remove the ".class" at the back
				String name = className.substring(0, className.length() - 6);
				Class<?> clazz = getClass(f, name);
				if (clazz.getSuperclass() != null
						&& clazz.getSuperclass().getName().compareTo("calico.plugins.AbstractCalicoPlugin") == 0)
					pluginCollection.add(clazz);
//				Class[] interfaces = clazz.getInterfaces();
//				for (Class c : interfaces) {
//					// Implement the Class<?> interface
//					if (c.getName().equals("base.Class<?>")) {
//						pluginCollection.add((Class<?>) clazz.newInstance());
//					}
//				}
			}
		}
	}

	protected List<String> getClassNames(String jarName) throws IOException {
		ArrayList<String> classes = new ArrayList<String>(10);
		JarInputStream jarFile = new JarInputStream(
				new FileInputStream(jarName));
		JarEntry jarEntry;
		while (true) {
			jarEntry = jarFile.getNextJarEntry();
			if (jarEntry == null) {
				break;
			}
			if (jarEntry.getName().endsWith(".class")) {
				classes.add(jarEntry.getName().replaceAll("/", "\\."));
			}
		}

		return classes;
	}

	public Class<?> getClass(File file, String name) throws Exception {
		addURL(file.toURI().toURL());

		URLClassLoader clazzLoader;
		Class<?> clazz;
		String filePath = file.getAbsolutePath();
		filePath = "jar:file://" + filePath + "!/";
		URL url = new File(filePath).toURI().toURL();
		clazzLoader = new URLClassLoader(new URL[] { url });
		clazz = clazzLoader.loadClass(name);
		return clazz;

	}

	public void addURL(URL u) throws IOException {
		URLClassLoader sysLoader = (URLClassLoader) ClassLoader
				.getSystemClassLoader();
		URL urls[] = sysLoader.getURLs();
		for (int i = 0; i < urls.length; i++) {
			if (urls[i].toString().equalsIgnoreCase(u.toString())) {
				return;
			}
		}
		Class<?> sysclass = URLClassLoader.class;
		try {
			Method method = sysclass.getDeclaredMethod("addURL", parameters);
			method.setAccessible(true);
			method.invoke(sysLoader, new Object[] { u });
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IOException(
					"Error, could not add URL to system classloader");
		}
	}

	public List<Class<?>> getPluginCollection() {
		return pluginCollection;
	}

	public void setPluginCollection(List<Class<?>> pluginCollection) {
		this.pluginCollection = pluginCollection;
	}
}
