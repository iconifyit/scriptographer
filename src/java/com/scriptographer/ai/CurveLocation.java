/*
 * Scriptographer
 *
 * This file is part of Scriptographer, a Scripting Plugin for Adobe Illustrator
 * http://scriptographer.org/
 *
 * Copyright (c) 2002-2010, Juerg Lehni
 * http://scratchdisk.com/
 *
 * All rights reserved. See LICENSE file for details.
 * 
 * File created on Jun 11, 2010.
 */

package com.scriptographer.ai;

/**
 * CurveLocation objects describe a location on {@Curve} objects, as
 * defined by the curve {@link #getParameter()}, a value between {@code 0}
 * (beginning of the curve) and {@code 1} (end of the curve). If the curve is
 * part of a {@link Path} item, its {@link #getIndex()} inside the
 * {@link Path#getCurves()} list is also provided.
 * 
 * The class is in use in many places, such as {@link Path#getLocation(double)},
 * {@link Path#getLength(CurveLocation)}, {@link Path#getPoint(double)},
 * {@link Path#split(CurveLocation)},
 * {@link PathItem#getIntersections(PathItem)}, etc.
 * 
 * @author lehni
 */
public class CurveLocation {
	private Curve curve;
	private double parameter;
	private Point point;
	private Segment segment;

	protected CurveLocation() {
	}

	protected CurveLocation(Curve curve, double parameter, Point point) {
		init(curve, parameter, point);
	}

	public CurveLocation(Curve curve, double parameter) {
		init(curve, parameter, null);
	}

	public CurveLocation(Path path, int index, double parameter) {
		init(path.getCurves().get(index), parameter, null);
	}

	protected void init(Curve curve, double parameter, Point point) {
		this.curve = curve;
		this.parameter = parameter;
		this.point = point;
	}

	/**
	 * The segment of the curve which is closer to the described location.
	 */
	public Segment getSegment() {
		if (segment == null) {
			// Determine the segment closest to the hit point
			if (parameter == 0) {
				segment = curve.getSegment1();
			} else if (parameter == 1) {
				segment = curve.getSegment2();
			} else if (parameter == -1) {
				return null;
			} else {
				// Determine the closest segment by comparing curve lengths
				Curve rightCurve = ((Curve) curve.clone()).divide(parameter);
				segment = rightCurve.getLength() > curve.getLength() / 2
						? curve.getSegment1()
						: curve.getSegment2();
			}
		}
		return segment;
	}

	/**
	 * The curve by which the location is defined.
	 */
	public Curve getCurve() {
		return curve;
	}

	/**
	 * The index of the curve within the {@link Path#getCurves()} list, if the
	 * curve is part of a {@link Path} item.
	 */
	public int getIndex() {
		if (curve != null)
			return curve.getIndex();
		return -1;
	}

	/**
	 * The length of the path from its beginning up to the location described
	 * by this object.
	 */
	public Double getLength() {
		Path path = curve.getPath();
		if (path != null)
			return path.getLength(this);
		return null;
	}
	
	/**
	 * The length of the curve from its beginning up to the location described
	 * by this object.
	 */
	public Double getCurveLength() {
		if (curve != null) {
			Double parameter = getParameter();
			if (parameter != null)
				return curve.getPartLength(0, parameter);
		}
		return null;
	}

	/**
	 * The curve parameter, as used by various bezier curve calculations. It is
	 * value between {@code 0} (beginning of the curve) and {@code 1} (end of
	 * the curve).
	 */
	public Double getParameter() {
		if (parameter == -1 && point != null) {
			parameter = curve.getParameter(point);
		}
		return parameter != -1 ? parameter : null;
	}
	
	/**
	 * The point which is defined by the {@link #getCurve()} and
	 * {@link #getParameter()}.
	 */
	public Point getPoint() {
		if (point == null && curve != null)
			point = curve.getPoint(parameter);
		return point;
	}

	/**
	 * The item this curve belongs to, if any.
	 */
	public Item getItem() {
		return curve != null ? curve.getPath() : null;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer(32);
		Point point = getPoint();
		if (point != null)
			buf.append(", point: ").append(getPoint());
		int index = getIndex();
		if (index >= 0)
			buf.append(", index: ").append(index);
		if (parameter != -1)
			buf.append(", parameter: ").append(parameter);
		// Replace the first ',' with a '{', no matter which one came first.
		buf.setCharAt(0, '{');
		buf.append(" }");
		return buf.toString();
	}
}
