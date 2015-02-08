package net.veierland.aix;

import java.util.Calendar;

public class AixUtils {

	private AixUtils() {
		
	}

	public final static int clamp(int value, int min, int max) {
		int result = value;
		if (min == max) {
			if (value != min) {
				result = min;
			}
		} else if (min < max) {
			if (value < min) {
				result = min;
			} else if (value > max) {
				result = max;
			}
		} else {
			result = clamp(value, max, min);
		}

		return result;
	}
	
	public static long lcap(long x, long c) {
		return x > c ? x : c;
	}
	
	public static long hcap(long x, long c) {
		return x < c ? x : c;
	}
	
	public static int lcap(int x, int c) {
		return x > c ? x : c;
	}
	
	public static int hcap(int x, int c) {
		return x < c ? x : c;
	}
	
	public static float lcap(float x, float c) {
		return x > c ? x : c;
	}
	
	public static float hcap(float x, float c) {
		return x < c ? x : c;
	}
	
	public static boolean isPrime(long n) {
		boolean prime = true;
		for (long i = 3; i <= Math.sqrt(n); i += 2)
			if (n % i == 0) {
				prime = false;
				break;
			}
		if ((n % 2 != 0 && prime && n > 2) || n == 2) {
			return true;
		} else {
			return false;
		}
	}
	
//	public static NinePatchDrawable getNinePatchDrawable(Resources resources, int resId) {
//		Log.d("AixDetailedWidget", "GG");
//		Bitmap bitmap = getBitmapUnscaled(resources, resId);
//		Log.d("AixDetailedWidget", "HF");
//		byte[] qq = bitmap.getNinePatchChunk();
//		Log.d("AixDetailedWidget", "qq=" + qq);
//		NinePatch np = new NinePatch(bitmap, bitmap.getNinePatchChunk(), null);
//		Log.d("AixDetailedWidget", "AHA");
//		return new NinePatchDrawable(resources, np);
//	}
//
//	public static Bitmap getBitmapUnscaled(Resources resources, int resId) {
//		Log.d("AixDetailedWidget", "!!");
//		BitmapFactory.Options opts = new BitmapFactory.Options();
//		Log.d("AixDetailedWidget", "@@");
//		opts.inDensity = opts.inTargetDensity = resources.getDisplayMetrics().densityDpi;
//		Log.d("AixDetailedWidget", "## " + opts.inTargetDensity);
//		Bitmap bitmap = BitmapFactory.decodeResource(resources, resId, opts);
//		Log.d("AixDetailedWidget", "$$ " + bitmap);
//		return bitmap;
//	}
	
	public static Calendar truncateDay(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}
	
	public static Calendar truncateHour(Calendar calendar) {
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}
	
}
