package net.veierland.aix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;

public class AixUtils {

	public static final int[] WEATHER_ICONS_DAY = {
		R.drawable.weather_icon_day_sun,
		R.drawable.weather_icon_day_polar_lightcloud,
		R.drawable.weather_icon_day_partlycloud,
		R.drawable.weather_icon_cloud,
		R.drawable.weather_icon_day_lightrainsun,
		R.drawable.weather_icon_day_polar_lightrainthundersun,
		R.drawable.weather_icon_day_polar_sleetsun,
		R.drawable.weather_icon_day_snowsun,
		R.drawable.weather_icon_lightrain,
		R.drawable.weather_icon_rain,
		R.drawable.weather_icon_rainthunder,
		R.drawable.weather_icon_sleet,
		R.drawable.weather_icon_snow,
		R.drawable.weather_icon_snowthunder,
		R.drawable.weather_icon_fog
	};

	public static final int[] WEATHER_ICONS_NIGHT = {
		R.drawable.weather_icon_night_sun,
		R.drawable.weather_icon_night_lightcloud,
		R.drawable.weather_icon_night_partlycloud,
		R.drawable.weather_icon_cloud,
		R.drawable.weather_icon_night_lightrainsun,
		R.drawable.weather_icon_night_lightrainthundersun,
		R.drawable.weather_icon_night_sleetsun,
		R.drawable.weather_icon_night_snowsun,
		R.drawable.weather_icon_lightrain,
		R.drawable.weather_icon_rain,
		R.drawable.weather_icon_rainthunder,
		R.drawable.weather_icon_sleet,
		R.drawable.weather_icon_snow,
		R.drawable.weather_icon_snowthunder,
		R.drawable.weather_icon_fog
	};

	public static final int[] WEATHER_ICONS_POLAR = {
		R.drawable.weather_icon_polar_sun,
		R.drawable.weather_icon_day_polar_lightcloud,
		R.drawable.weather_icon_polar_partlycloud,
		R.drawable.weather_icon_cloud,
		R.drawable.weather_icon_polar_lightrainsun,
		R.drawable.weather_icon_day_polar_lightrainthundersun,
		R.drawable.weather_icon_day_polar_sleetsun,
		R.drawable.weather_icon_polar_snowsun,
		R.drawable.weather_icon_lightrain,
		R.drawable.weather_icon_rain,
		R.drawable.weather_icon_rainthunder,
		R.drawable.weather_icon_sleet,
		R.drawable.weather_icon_snow,
		R.drawable.weather_icon_snowthunder,
		R.drawable.weather_icon_fog
	};
	
	public static final int WEATHER_ICON_DAY_SUN = 1;
	public static final int WEATHER_ICON_NIGHT_SUN = 1;
	public static final int WEATHER_ICON_POLAR_SUN = 1;
	public static final int WEATHER_ICON_DAY_POLAR_LIGHTCLOUD = 2;
	public static final int WEATHER_ICON_NIGHT_LIGHTCLOUD = 2;
	public static final int WEATHER_ICON_DAY_PARTLYCLOUD = 3;
	public static final int WEATHER_ICON_NIGHT_PARTLYCLOUD = 3;
	public static final int WEATHER_ICON_POLAR_PARTLYCLOUD = 3;
	public static final int WEATHER_ICON_CLOUD = 4;
	public static final int WEATHER_ICON_DAY_LIGHTRAINSUN = 5;
	public static final int WEATHER_ICON_NIGHT_LIGHTRAINSUN = 5;
	public static final int WEATHER_ICON_POLAR_LIGHTRAINSUN = 5;
	public static final int WEATHER_ICON_DAY_POLAR_LIGHTRAINTHUNDERSUN = 6;
	public static final int WEATHER_ICON_NIGHT_LIGHTRAINTHUNDERSUN = 6;
	public static final int WEATHER_ICON_DAY_POLAR_SLEETSUN = 7;
	public static final int WEATHER_ICON_NIGHT_SLEETSUN = 7;
	public static final int WEATHER_ICON_DAY_SNOWSUN = 8;
	public static final int WEATHER_ICON_NIGHT_SNOWSUN = 8;
	public static final int WEATHER_ICON_POLAR_SNOWSUN = 8;
	public static final int WEATHER_ICON_LIGHTRAIN = 9;
	public static final int WEATHER_ICON_RAIN = 10;
	public static final int WEATHER_ICON_RAINTHUNDER = 11;
	public static final int WEATHER_ICON_SLEET = 12;
	public static final int WEATHER_ICON_SNOW = 13;
	public static final int WEATHER_ICON_SNOWTHUNDER = 14;
	public static final int WEATHER_ICON_FOG = 15;
	
	public static final int BORDER_COLOR = 0;
	public static final int BACKGROUND_COLOR = 1;
	public static final int TEXT_COLOR = 2;
	public static final int PATTERN_COLOR = 3;
	public static final int DAY_COLOR = 4;
	public static final int NIGHT_COLOR = 5;
	public static final int GRID_COLOR = 6;
	public static final int GRID_OUTLINE_COLOR = 7;
	public static final int MAX_RAIN_COLOR = 8;
	public static final int MIN_RAIN_COLOR = 9;
	public static final int ABOVE_FREEZING_COLOR = 10;
	public static final int BELOW_FREEZING_COLOR = 11;
	
	public static final int TOP_TEXT_NEVER = 1;
	public static final int TOP_TEXT_LANDSCAPE = 2;
	public static final int TOP_TEXT_PORTRAIT = 3;
	public static final int TOP_TEXT_ALWAYS = 4;
	
	private AixUtils() {
		
	}
	
	public final static int even(int value) {
		return (value % 2 != 0) ? value - 1 : value;
	}
	
	public final static int odd(int value) {
		return (value % 2 != 0) ? value : value - 1;
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
	
	public final static long clamp(long value, long min, long max) {
		long result = value;
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
	
	public final static float clamp(float value, float min, float max) {
		float result = value;
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
	
	public static String convertStreamToString(InputStream is) throws IOException {
		if (is != null) {
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		} else {
			return "";
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
