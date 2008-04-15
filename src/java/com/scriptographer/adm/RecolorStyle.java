/*
 * Scriptographer
 *
 * This file is part of Scriptographer, a Plugin for Adobe Illustrator.
 *
 * Copyright (c) 2002-2008 Juerg Lehni, http://www.scratchdisk.com.
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
 * File created on Apr 15, 2008.
 *
 * $Id$
 */

package com.scriptographer.adm;

import com.scratchdisk.util.IntegerEnum;

/**
 * ADMRecolorStyle
 * 
 * @author lehni
 */
public enum RecolorStyle implements IntegerEnum {
	NO(0),
	ACTIVE(1),
	INACTIVE(2),
	DISABLED(3);

	protected int value;

	private RecolorStyle(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}
}