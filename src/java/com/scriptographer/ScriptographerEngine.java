/*
 * Scriptographer
 *
 * This file is part of Scriptographer, a Plugin for Adobe Illustrator.
 *
 * Copyright (c) 2002-2005 Juerg Lehni, http://www.scratchdisk.com.
 * All rights reserved.
 *
 * Please visit http://scriptographer.com/ for updates and contact.
 *
 * -- GPL LICENSE NOTICE --
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * -- GPL LICENSE NOTICE --
 *
 * File created on 04.12.2004.
 *
 * $RCSfile: ScriptographerEngine.java,v $
 * $Author: lehni $
 * $Revision: 1.7 $
 * $Date: 2005/03/30 08:21:33 $
 */

package com.scriptographer;

import com.scriptographer.adm.*;
import com.scriptographer.ai.*;
import com.scriptographer.gui.*;
import com.scriptographer.js.ScriptographerContextFactory;

import org.mozilla.javascript.*;

import java.io.*;
import java.util.HashMap;
import java.util.prefs.Preferences;

public class ScriptographerEngine {
	private static ScriptographerEngine engine = null;
	private Context context;
	private HashMap scriptCache = new HashMap();
	private GlobalObject global;
	private static final boolean isWindows, isMacintosh;
	private static ConsoleDialog consoleDialog;
	private static MainDialog mainDialog;
	private static File baseDir = null;

	static {
		// immediatelly redirect system streams.
		ConsoleOutputStream.getInstance().enableRedirection(true);
		// getSystem variables
		String os = System.getProperty("os.name").toLowerCase();
		isWindows = (os.indexOf("windows") != -1);
		isMacintosh = (os.indexOf("mac os x") != -1);
	}

	public ScriptographerEngine() throws Exception {
		ContextFactory.initGlobal(new ScriptographerContextFactory());
		context = Context.enter();
		global = new GlobalObject(context);
	}

	public static void init() throws Exception {
		// This is needed on mac, where there is more than one thread and the Loader is initiated on startup
		// in the second thread. The ScriptographerEngine get loaded through the Loader, so getting the
		// ClassLoader from there is save:
		Thread.currentThread().setContextClassLoader(ScriptographerEngine.class.getClassLoader());
		// get the baseDir setting, if it's not set, ask the user
		Preferences prefs = Preferences.userNodeForPackage(ScriptographerEngine.class); 
		String dir = prefs.get("baseDir", null);
		baseDir = dir != null ? new File(dir) : null;
		if (baseDir == null || !baseDir.isDirectory()) {
			chooseBaseDirectory();
		}
		
		consoleDialog = new ConsoleDialog();
		mainDialog = new MainDialog(consoleDialog);
		ConsoleOutputStream.enableOutput(true);
		
		// execute all scripts in startup folder:
		if (baseDir != null)
			getInstance().executeAll(new File(baseDir, "startup"));
	}

	public static void destroy() {
		Dialog.destroyAll();
		LiveEffect.removeAll();
		MenuItem.removeAll();
		ConsoleOutputStream.getInstance().enableRedirection(false);
	}
	
	public static boolean chooseBaseDirectory() {
		baseDir = Dialog.chooseDirectory("Please choose the Scriptographer base directory:", baseDir);
		if (baseDir != null && baseDir.isDirectory()) {
			Preferences prefs = Preferences.userNodeForPackage(ScriptographerEngine.class); 
			prefs.put("baseDir", baseDir.getPath());
			return true;
		}
		return false;
	}
	
	public static File getBaseDirectory() {
		return baseDir;
	}
	
	public boolean isWindows() {
		return isWindows;
	}
	
	public boolean isMacintosh() {
		return isMacintosh;
	}

	static int reloadCount = 0;
	
	public static int getReloadCount() {
		return reloadCount;
	}
	
	public static String reload() {
		reloadCount++;
		return nativeReload();
	}
	
	public static native String nativeReload();

	public static ScriptographerEngine getInstance() throws Exception {
		if (engine == null)
			engine = new ScriptographerEngine();
		return engine;
	}
	
	private void reportRhinoException(RhinoException re) {
		String source = re.sourceName();
		if (source != null) {
			System.err.print(source);
			System.err.print(":");
		}
		System.err.println(re.lineNumber() + "," + re.columnNumber() + ": " + re.getMessage());
	}
	
	public static void onAbout() {
		AboutDialog.show();
	}

	/**
	 * Internal Class used for caching compiled scripts
	 */
	class ScriptCacheEntry {
		File file;
		long lastModified;
		Script script;

		ScriptCacheEntry(File file) {
			this.file = file;
			lastModified = -1;
			script = null;
		}

		Script compile() {
			long modified = file.lastModified();
			if (script == null || modified > lastModified) {
				script = null;
				FileReader in = null;
				try {
					in = new FileReader(file);
					script = context.compileReader(in, file.getPath(), 1, null);
					lastModified = modified;
				} catch (RhinoException re) {
					reportRhinoException(re);
				} catch (FileNotFoundException ex) {
					Context.reportError("File does not exist: " + file);
				} catch (IOException ioe) {
					System.err.println(ioe.toString());
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException ioe) {
							System.err.println(ioe.toString());
						}
					}
				}
			}
			return script;
		}
	}

	/**
	 * Compiles the specified file.
	 * Caching for the compiled scripts is used for speed increase.
	 * 
	 * @param file
	 * @return
	 */
	public Script compileFile(File file) {
		String path = file.getPath();
		ScriptCacheEntry entry = (ScriptCacheEntry) scriptCache.get(path);
		if (entry == null) {
			entry = new ScriptCacheEntry(file);
			scriptCache.put(path, entry);
		}
		return entry.compile();
	}

	public Script compileString(String string) {
		try {
			return context.compileString(string, null, 1, null);
		} catch (RhinoException re) {
			reportRhinoException(re);
		}
		return null;
	}

	public Scriptable executeScript(Script script, File scriptFile, Scriptable scope) {
		Scriptable ret = null;
		try {
			if (scope == null)
				scope = global.createScope(scriptFile);
			// disable output to the console while the script is executed as it won't get updated anyway
			ConsoleOutputStream.enableOutput(false);
			script.exec(context, scope);
			// now commit all the changes:
			CommitManager.commit();
			ret = scope;
		} catch (WrappedException we) {
			System.err.println(we.getMessage());
			we.getWrappedException().printStackTrace();
		} catch (RhinoException re) {
			reportRhinoException(re);
		} finally {
			// now reenable the console, this also writes out all the things that were printed in the meantime:
			ConsoleOutputStream.enableOutput(true);
		}
		return ret;
	}

	/**
	 * executes all scripts in the given folder
	 *
	 * @param dir
	 */
	private void executeAll(File dir) {
		File []files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				executeAll(file);
			} else if (file.getName().endsWith(".js")) {
				executeFile(file, null);
			}
		}
	}

	/**
	 * evaluates the specified file.
	 *
	 * @param file
	 * @return
	 */
	public Scriptable executeFile(File file, Scriptable scope) {
		Script script = compileFile(file);
		if (script != null)
			return executeScript(script, file, scope);
		return null;
	}

	public Scriptable executeFile(String path, Scriptable scope) {
		return executeFile(new File(path), scope);
	}

	public Scriptable executeString(String string, Scriptable scope) {
		Script script = compileString(string);
		if (script != null)
			return executeScript(script, null, scope);
		return null;
	}
	
	public Scriptable createScope(File scriptFile) {
		return global.createScope(scriptFile);
	}
	
	/**
	 * Launches the filename with the default associated editor.
	 * 
	 * @param filename
	 * @return
	 */
	
	public static native boolean launch(String filename);

	public static boolean launch(File file) {
		return launch(file.getPath());
	}

	public static void main(String args[]) throws Exception {
		ConsoleOutputStream.getInstance().enableRedirection(false);
	 	getInstance().executeFile("/Users/Lehni/Development/C & C++/Scriptographer/scripts/test.js", null);
	}
}