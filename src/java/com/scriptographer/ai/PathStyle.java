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
 * File created on 14.02.2005.
 *
 * $Id$
 */

package com.scriptographer.ai;

import com.scratchdisk.script.ArgumentReader;
import com.scratchdisk.util.IntegerEnumUtils;
import com.scriptographer.Commitable;
import com.scriptographer.CommitManager;

/*
 * PathStyle, FillStyle and StrokeStyle are used for Item, CharacterAttributes,
 * and others In some places, not all of the values may be defined.
 * Setting any value to null means the value is not defined.
 * 
 * Setting fillColor or StrokeColor to Color.NONE means no fill / stroke
 * Setting it to null undefines the value, it doesn't have the same effect as
 * seting it to Color.NONE
 * 
 * Since Java 1.5 comes with auto boxing / unboxing, I don't think it's a big
 * deal that we're not returning native values but boxed ones here (or null,
 * in case the value isn't defined)
 * 
 * PathStyle derives AIObject so CharacterStyle has a handle.
 */

/**
 * @author lehni
 */
public class PathStyle extends NativeObject implements Style, Commitable {
	protected FillStyle fill;

	protected StrokeStyle stroke;
	
	/**
	 *  Whether or not to use this as a clipping path.
	 *  @deprecated in Illustrator, but we still need to keep it around to reflect the state
	 */
	protected Boolean clip;
	
	/**
	 *  Whether or not to lock the clipping path.
	 *  @deprecated in Illustrator, but we still need to keep it around to reflect the state
	 */
	protected Boolean lockClip;

	// Whether or not to use the even-odd rule to determine path insideness
	protected WindingRule windingRule;
	
	// Path's resolution
	protected Float resolution;
	
	private Item item = null;

	protected boolean dirty = false;
	protected int version = -1;
	
	// Don't fetch immediately. Only fetch once values are requested
	protected boolean fetched = false;
	
	/*
	 * for CharacterStyle
	 */
	protected PathStyle(int handle) {
		super(handle);
		fill = new FillStyle(this);
		stroke = new StrokeStyle(this);
	}

	protected PathStyle(int handle, ArgumentReader reader) {
		super(handle);
		// First try the full fill object
		fill = (FillStyle) reader.readObject("fill", FillStyle.class);
		if (fill == null) {
			fill = new FillStyle(this);
			// reader defines fill as null? set color to NONE
			if (reader.isHash() && reader.has("fill"))
				fill.setColor(Color.NONE);
		}
		fill.setStyle(this);
		// Otherwise read in redirecting properties...
		stroke = (StrokeStyle) reader.readObject("stroke", StrokeStyle.class);
		if (stroke == null) {
			stroke = new StrokeStyle(this);
			// reader defines stroke as null? set color to NONE
			if (reader.isHash() && reader.has("stroke"))
				stroke.setColor(Color.NONE);
		}
		stroke.setStyle(this);
		windingRule = reader.readEnum("windingRule", WindingRule.class);
		resolution = reader.readFloat("resolution");
	}

	/*
	 * For Item#getStyle
	 */
	protected PathStyle(Item item) {
		this(0); // PathStyle doesn't use the handle, but CharacterStyle does
		this.item = item;
	}

	protected PathStyle(PathStyle style) {
		this(0); // PathStyle doesn't use the handle, but CharacterStyle does
		init(style);
	}

	public PathStyle(FillStyle fill, StrokeStyle stroke) {
		super();
		this.fill = new FillStyle(fill, this);
		this.stroke = new StrokeStyle(stroke, this);
	}

	/**
	 * @jshide
	 */
	public PathStyle(ArgumentReader reader) {
		this(0, reader);
	}

	public boolean equals(Object obj) {
		if (obj instanceof PathStyle) {
			// TODO: Implement!
		}
		return false;
	}
	
	public Object clone() {
		return new PathStyle(this);
	}
	
	protected void update() {
		// Only update if it didn't change in the meantime:
		if (item != null && (!fetched || (!dirty && version != item.version)))
			fetch();
	}

	/*
	 * This is complicated: for undefined values:
	 * - color needs an additional boolean value
	 * - boolean values are passed as short: -1 = undefined, 0 = false,
	 *   1 = true
	 * - float are passed as float: < 0 = undefined, >= 0 = defined
	 */
	protected void init(
			Color fillColor, boolean hasFillColor, short fillOverprint,
			Color strokeColor, boolean hasStrokeColor, short strokeOverprint,
			float strokeWidth,
			float dashOffset, float[] dashArray,
			short cap, short join, float miterLimit,
			short clip, short lockClip, int windingRule, float resolution) {
		//  dashArray doesn't need the boolean, as it's {} when set but empty
		
		fill.init(fillColor, hasFillColor, fillOverprint);
		stroke.init(strokeColor, hasStrokeColor, strokeOverprint, strokeWidth,
			dashOffset, dashArray, cap, join, miterLimit);

		this.clip = clip >= 0 ? new Boolean(clip != 0) : null;
		this.lockClip = lockClip >= 0 ? new Boolean(lockClip != 0) : null;
		this.windingRule = IntegerEnumUtils.get(WindingRule.class, (int) windingRule);
		this.resolution = resolution >= 0 ? new Float(resolution) : null;
	}
	
	protected void init(PathStyle style) {
		FillStyle fillStyle = style.fill;
		StrokeStyle strokeStyle = style.stroke;
		fill.init(fillStyle.color, fillStyle.overprint);
		stroke.init(strokeStyle.color, strokeStyle.overprint, strokeStyle.width,
			strokeStyle.dashOffset, strokeStyle.dashArray, strokeStyle.cap,
			strokeStyle.join, strokeStyle.miterLimit);
		this.clip = style.clip;
		this.lockClip = style.lockClip;
		this.windingRule = style.windingRule;
		this.resolution = style.resolution;
	}

	protected native void nativeGet(int handle);
	
	protected native void nativeSet(int handle, int docHandle, 
			Color fillColor, boolean hasFillColor,
			short fillOverprint,
			Color strokeColor, boolean hasStrokeColor,
			short strokeOverprint, float strokeWidth,
			float dashOffset, float[] dashArray,
			int cap, int join, float miterLimit,
			short clip, short lockClip, int windingRule, float resolution);

	// These would belong to FillStyle and StrokeStyle, but in order to safe 4
	// new native files, they're here:
	protected static native void nativeInitStrokeStyle(int handle, Color color,
		boolean hasColor, short overprint, float width, float dashOffset,
		float[] dashArray, int cap, int join, float miterLimit);

	protected static native void nativeInitFillStyle(int handle, Color color,
		boolean hasColor, short overprint);
	
	/**
	 * just a wrapper around nativeCommit, which can be used in CharacterStyle
	 * as well (CharacterStyle has an own implementation of nativeCommit, but
	 * the calling is the same...)
	 */
	protected void commit(int handle, int docHandle) {
		nativeSet(handle, docHandle,
			fill.color != null && fill.color != Color.NONE ? fill.color : null,
			fill.color != null, 
			fill.overprint != null ? (short) (fill.overprint.booleanValue() ? 1 : 0) : -1,
			stroke.color != null && stroke.color != Color.NONE ? stroke.color : null,
			stroke.color != null,
			stroke.overprint != null ? (short) (stroke.overprint.booleanValue() ? 1 : 0) : -1,
			stroke.width != null ? stroke.width.floatValue() : -1,
			stroke.dashOffset != null ? stroke.dashOffset.floatValue() : -1,
			stroke.dashArray,
			stroke.cap != null ? stroke.cap.value : -1,
			stroke.join != null ? stroke.join.value : -1,
			stroke.miterLimit != null ? stroke.miterLimit.floatValue() : -1,
			clip != null ? (short) (clip.booleanValue() ? 1 : 0) : -1,
			lockClip != null ? (short) (lockClip.booleanValue() ? 1 : 0) : -1,
			windingRule != null ? windingRule.value() : -1,
			resolution != null ? resolution.floatValue() : -1
		);
	}

	protected void fetch() {
		nativeGet(item.handle);
		version = item.version;
		fetched = true;
	}

	public void commit() {
		if (dirty && item != null) {
			commit(item.handle, item.document.handle);
			version = item.version;
			dirty = false;
		}
	}

	protected void markDirty() {
		// only mark it as dirty if it's attached to a path already:
		if (!dirty && item != null) {
			CommitManager.markDirty(item, this);
			dirty = true;
		}
	}

	/**
	 * @jshide
	 */
	public FillStyle getFill() {
		return fill;
	}

	/**
	 * @jshide
	 */
	public void setFill(FillStyle fill) {
		update();
		this.fill = new FillStyle(fill, this);
		markDirty();
	}

	protected FillStyle getFill(boolean create) {
		if (fill == null && create)
			fill = new FillStyle(this);
 		return fill;
	}

	/**
	 * @jshide
	 */
	public StrokeStyle getStroke() {
 		return stroke;
	}

	protected StrokeStyle getStroke(boolean create) {
		if (stroke == null && create)
			stroke = new StrokeStyle(this);
 		return stroke;
	}

	/**
	 * @jshide
	 */
	public void setStroke(StrokeStyle stroke) {
		update();
		this.stroke = new StrokeStyle(stroke, this);
		markDirty();
	}

	/*
	 * Stroke Styles
	 */

	public Color getStrokeColor() {
		// TODO: Return Color.NONE instead of null?
		return stroke != null ? stroke.getColor() : null;
	}

	public void setStrokeColor(Color color) {
		getStroke(true).setColor(color);
	}

	public void setStrokeColor(java.awt.Color color) {
		getStroke(true).setColor(color);
	}

	public Float getStrokeWidth() {
		return stroke != null ? stroke.getWidth() : null;
	}

	public void setStrokeWidth(Float width) {
		getStroke(true).setWidth(width);
	}

	public StrokeCap getStrokeCap() {
		return stroke != null ? stroke.getCap() : null;
	}

	public void setStrokeCap(StrokeCap cap) {
		getStroke(true).setCap(cap);
	}

	public StrokeJoin getStrokeJoin() {
		return stroke != null ? stroke.getJoin() : null;
	}

	public void setStrokeJoin(StrokeJoin join) {
		getStroke(true).setJoin(join);
	}

	public Float getDashOffset() {
		return stroke != null ? stroke.getWidth() : null;
	}

	public void setDashOffset(Float offset) {
		getStroke(true).setDashOffset(offset);
	}
	
	public float[] getDashArray() {
		return stroke != null ? stroke.getDashArray() : null;
	}

	public void setDashArray(float[] array) {
		getStroke(true).setDashArray(array);
	}
	
	public Float getMiterLimit() {
		return stroke != null ? stroke.getWidth() : null;
	}

	public void setMiterLimit(Float limit) {
		getStroke(true).setMiterLimit(limit);
	}

	public Boolean getStrokeOverprint() {
		return stroke != null ? stroke.getOverprint() : null;
	}

	public void setStrokeOverprint(Boolean overprint) {
		getStroke(true).setOverprint(overprint);
	}

	/*
	 * Fill Style
	 */

	public Color getFillColor() {
		// TODO: Return Color.NONE instead of null?
		return fill != null ? fill.getColor() : null;
	}

	public void setFillColor(Color color) {
		getFill(true).setColor(color);
	}

	public void setFillColor(java.awt.Color color) {
		getFill(true).setColor(color);
	}

	public Boolean getFillOverprint() {
		return fill != null ? fill.getOverprint() : null;
	}

	public void setFillOverprint(Boolean overprint) {
		getFill(true).setOverprint(overprint);
	}

	/*
	 * Path Style
	 */
	public WindingRule getWindingRule() {
		update();
		return windingRule;
	}

	public void setWindingRule(WindingRule rule) {
		update();
		this.windingRule = rule;
		markDirty();
	}

	public Float getResolution() {
		update();
		return resolution;
	}

	public void setResolution(Float resolution) {
		update();
		this.resolution = resolution;
		markDirty();
	}
}
