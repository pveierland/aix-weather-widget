package net.veierland.aix.util;

import android.graphics.Path;
import android.graphics.PointF;

public class CatmullRomSpline {
	
	private static PointF getDerivative(PointF[] pointArray, int i, float tension) {
		PointF first, second;
		
		if (i == 0)
		{
			// First point
			first = pointArray[0];
			second = pointArray[1];
		}
		else if (i == pointArray.length - 1)
		{
			// Last point
			first = pointArray[i - 1];
			second = pointArray[i];
		}
		else
		{
			first = pointArray[i - 1];
			second = pointArray[i + 1];
		}
		
		float x = (second.x - first.x) / tension;
		float y = (second.y - first.y) / tension;

		return new PointF(x, y);
		
		/*
		if (i == 0) {
			// First point
			x = (points[1].x - points[0].x) / tension;
			y = (points[1].y - points[0].y) / tension;
		} else if (i == points.length - 1) {
			// Last point
			x = (points[i].x - points[i - 1].x) / tension;
			y = (points[i].y - points[i])
		}
		
		
		if (i == 0) {
			// First point
			ret.set((float)((points[1].x - points[0].x) / tension),
					(float)((points[1].y - points[0].y) / tension));
		} else if (i == points.length - 1) {
			// Last point
			ret.set((float)((points[i].x - points[i - 1].x) / tension),
					(float)((points[i].y - points[i - 1].y) / tension));
		} else {
			ret.set((float)((points[i + 1].x - points[i - 1].x) / tension),
					(float)((points[i + 1].y - points[i - 1].y) / tension));
		}
		*/
	}
		
	private static PointF getB1(PointF[] points, int i, float tension) {
		PointF derivative = getDerivative(points, i, tension);
		
		float x = points[i].x + derivative.x / 3.0f;
		float y = points[i].y + derivative.y / 3.0f;
		
		return new PointF(x, y);
		
		/*
		return new PointF((float)(points[i].x + derivative.x / 3.0f),
						  (float)(points[i].y + derivative.y / 3.0f));
		*/
	}
		
	private static PointF getB2(PointF[] points, int i, float tension) {
		PointF derivative = getDerivative(points, i + 1, tension);
		
		float x = points[i + 1].x - derivative.x / 3.0f;
		float y = points[i + 1].y - derivative.y / 3.0f;
		
		return new PointF(x, y);
		
		/*
		return new PointF((float)(points[i + 1].x - derivative.x / 3.0f),
						  (float)(points[i + 1].y - derivative.y / 3.0f));
		*/
	}
		
	public static Path buildPath(PointF[] pointArray) {
		Path path = new Path();
		
		if (pointArray != null && pointArray.length > 2) {
			path.moveTo(pointArray[0].x, pointArray[0].y);
			
			for (int i = 1; i < pointArray.length; i++) {
				PointF b1 = getB1(pointArray, i - 1, 2.0f);
				PointF b2 = getB2(pointArray, i - 1, 2.0f);
				path.cubicTo(b1.x, b1.y, b2.x, b2.y, pointArray[i].x, pointArray[i].y);
			}
		}

		return path;
	}
	
}
