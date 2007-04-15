/*
 * Scriptographer
 *
 * This file is part of Scriptographer, a Plugin for Adobe Illustrator.
 *
 * Copyright (c) 2002-2007 Juerg Lehni, http://www.scratchdisk.com.
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
 * File created on Apr 10, 2007.
 *
 * $Id: $
 */

package com.scratchdisk.script.rhino;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.WrappedException;

import com.scratchdisk.script.ScriptException;
import com.scratchdisk.util.StringUtils;
import com.scriptographer.ScriptographerEngine;

/**
 * ScriptException for Rhino, preferably called RhinoException, but
 * that's already used by Rhino (org.mozilla.javascript.RhinoException).
 */
public class RhinoScriptException extends ScriptException {

	private static String formatMessage(Throwable t) {
		RhinoException re = t instanceof RhinoException ? (RhinoException) t
				: new WrappedException(t);

			String basePath =
				ScriptographerEngine.getScriptDirectory().getAbsolutePath();

			StringWriter buf = new StringWriter();
			PrintWriter writer = new PrintWriter(buf);

			writer.println(re.details());
			writer.print(StringUtils.replace(StringUtils.replace(
				re.getScriptStackTrace(), basePath, ""), "\t", "    "));
			
			return buf.toString();
	}

	public RhinoScriptException(Throwable cause) {
		super(formatMessage(cause), cause);
	}
}
