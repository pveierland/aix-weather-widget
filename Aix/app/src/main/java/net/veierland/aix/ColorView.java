package net.veierland.aix;

import static net.veierland.aix.AixUtils.clamp;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorView extends View {
	
	private final static int MODE_COLOR = 0;
	private final static int MODE_ALPHA = 1;
	private final static int MODE_HUE = 2;
	private final static int MODE_SV = 3;
	
	private final static int ORIENTATION_LEFT = 1;
	private final static int ORIENTATION_UP = 2;
	private final static int ORIENTATION_RIGHT = 4;
	private final static int ORIENTATION_DOWN = 8;
	
	private static final int[] HUE_COLORS = new int[] {
    	0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
	};

	private int mBorderColor;
	private int mColor;
	private int mMarkerColor;
	private int mMarkerContourColor;
	private int mMaxHeight;
	private int mMaxWidth;
	private int mMinHeight;
	private int mMinWidth;
	private int mMode;
	private int mOrientation;
	
	private float mBorderWidth;
	private float mDensity;
	private float mMarkerContourThickness;
	private float mMarkerRounding;
	private float mMarkerSize;
	private float mMarkerThickness;
	private float mMarkerWidth;
	
	private float[] mHSV;
	private float[] mValue;
	
	private Bitmap mTransparencyBitmap;
	
	private OnValueChangeListener mOnValueChangeListener;
	
	private Paint mBorderPaint;
	private Paint mColorAreaPaint;
	private Paint mMarkerContourPaint;
	private Paint mMarkerPaint;
	
	private RectF mColorAreaRect = new RectF();
	private RectF mMarkerRect = new RectF();
	
	private Shader mShader;
	
	public ColorView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		init(context, attributeSet);
	}

	public ColorView(Context context, AttributeSet attributeSet, int defStyle) {
		super(context, attributeSet);
		init(context, attributeSet);
	}
	
	private void init(Context context, AttributeSet attributeSet) {
		TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.ColorView);
		
		mMode = a.getInteger(R.styleable.ColorView_mode, MODE_COLOR);
		mOrientation = a.getInteger(R.styleable.ColorView_orientation,
				(mMode == MODE_SV)
					? ORIENTATION_RIGHT | ORIENTATION_UP
					: ORIENTATION_RIGHT);
		
		mColor = a.getColor(R.styleable.ColorView_color, Color.WHITE);
		
		mBorderColor = a.getColor(R.styleable.ColorView_borderColor, Color.WHITE);
		mBorderWidth = a.getDimension(R.styleable.ColorView_borderWidth, 0.0f);
		
		mMarkerColor = a.getColor(R.styleable.ColorView_markerColor, Color.WHITE);
		mMarkerContourColor = a.getColor(R.styleable.ColorView_markerContourColor, Color.BLACK);
		mMarkerContourThickness = a.getDimension(R.styleable.ColorView_markerContourThickness, 0.0f);
		mMarkerRounding = a.getDimension(R.styleable.ColorView_markerRounding, 0.0f);
		mMarkerSize = a.getDimension(R.styleable.ColorView_markerSize, 0.0f);
		mMarkerThickness = a.getDimension(R.styleable.ColorView_markerThickness, 0.0f);
		mMarkerWidth = a.getDimension(R.styleable.ColorView_markerWidth, 1.0f);
		
		mMaxHeight = a.getDimensionPixelSize(R.styleable.ColorView_android_maxHeight, Integer.MAX_VALUE);
		mMinHeight = a.getDimensionPixelSize(R.styleable.ColorView_android_minHeight, 0);
		mMaxWidth = a.getDimensionPixelSize(R.styleable.ColorView_android_maxWidth, Integer.MAX_VALUE);
		mMinWidth = a.getDimensionPixelSize(R.styleable.ColorView_android_minWidth, 0);
		
		a.recycle();
		
		mDensity = context.getResources().getDisplayMetrics().density;
		
		if (mBorderWidth > 0.0f) {
			mBorderPaint = new Paint() {{
				setColor(mBorderColor);
				setStrokeWidth(mBorderWidth);
				setStyle(Paint.Style.STROKE);
			}};
		}
		
		mColorAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mColorAreaPaint.setStyle(Paint.Style.FILL);
		
		if (mMode == MODE_COLOR) {
			mColorAreaPaint.setColor(mColor);
		} else if (mMode == MODE_ALPHA || mMode == MODE_HUE) {
			mShader = buildShader(
					mMode == MODE_ALPHA ? buildAlphaColorArray(mColor) : HUE_COLORS);
			mColorAreaPaint.setShader(mShader);
		} else if (mMode == MODE_SV) {
			mShader = buildSvShader();
			mHSV = new float[] { 0.0f, 1.0f, 1.0f };
		}
		
		if (mMode == MODE_ALPHA || mMode == MODE_HUE) {
			mValue = new float[] { 0.5f };
		} else {
			mValue = new float[] { 0.5f, 0.5f };
		}
		
		if (mMode == MODE_ALPHA || mMode == MODE_HUE || mMarkerSize > 0.0f) {
			// Using marker always in slider mode, or if it has been explicitly specified
			mMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mMarkerPaint.setColor(mMarkerColor);
			
			if (mMarkerThickness > 0.0f) {
				mMarkerPaint.setStrokeWidth(mMarkerThickness);
				mMarkerPaint.setStyle(Paint.Style.STROKE);
			} else {
				mMarkerPaint.setStyle(Paint.Style.FILL);
			}
			
			if (mMarkerContourThickness > 0.0f) {
				mMarkerContourPaint = new Paint() {{
					setAntiAlias(true);
					setColor(mMarkerContourColor);
					setStyle(Paint.Style.STROKE);
				}};
			}
		}
	}
	
	private int[] buildAlphaColorArray(int color) {
		return new int[] { color & 0x00FFFFFF, color | 0xFF000000 };
	}
	
	private Shader buildShader(int[] colors) {
		return new LinearGradient(
				mOrientation == ORIENTATION_LEFT  ? 1.0f : 0.0f,
				mOrientation == ORIENTATION_UP    ? 1.0f : 0.0f,
				mOrientation == ORIENTATION_RIGHT ? 1.0f : 0.0f,
				mOrientation == ORIENTATION_DOWN  ? 1.0f : 0.0f,
				colors, null, Shader.TileMode.CLAMP);
	}
	
	private Shader buildSvShader() {
		return new LinearGradient(
				(mOrientation & ORIENTATION_DOWN) != 0 ? 1.0f : 0.0f,
				(mOrientation & ORIENTATION_LEFT) != 0 ? 1.0f : 0.0f,
				(mOrientation & ORIENTATION_LEFT) != 0 ? 1.0f : 0.0f,
				(mOrientation & ORIENTATION_UP)   != 0 ? 1.0f : 0.0f,
				Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// Draw transparency background
		if ((mMode == MODE_COLOR || mMode == MODE_ALPHA) && mColorAreaRect.width() > 0 && mColorAreaRect.height() > 0) {
			if (mTransparencyBitmap == null) {
				
				int n = 1;
				while ((mColorAreaRect.width() / n > 8.0f * mDensity) ||
					   (mColorAreaRect.height() / n > 8.0f * mDensity))
				{
					n++;
				}
				
				int xcells, ycells;
				if (mColorAreaRect.width() > mColorAreaRect.height()) {
					xcells = n;
					ycells = (int)Math.ceil(mColorAreaRect.height() / (mColorAreaRect.width() / xcells));
				} else {
					ycells = n;
					xcells = (int)Math.ceil(mColorAreaRect.width() / (mColorAreaRect.height() / ycells));
				}
				
				Paint blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				blockPaint.setStyle(Paint.Style.FILL);
				blockPaint.setColor(Color.rgb(191, 191, 191));
				
				Bitmap transparencyBitmap = Bitmap.createBitmap((int)mColorAreaRect.width(), (int)mColorAreaRect.height(), Config.ARGB_8888);
				
				Canvas c = new Canvas(transparencyBitmap);
				c.drawColor(Color.WHITE);
				
				for (int i = 0; i < ycells; i++) {
					for (int j = 0; j < xcells; j++) {
						if (((i % 2 == 0) && (j % 2 == 0)) || ((i % 2 != 0) && (j % 2 != 0))) {
							c.drawRect(
									Math.round(j * (mColorAreaRect.width() / xcells)),
									Math.round(i * (mColorAreaRect.height() / ycells)),
									Math.round((j + 1) * (mColorAreaRect.width() / xcells)),
									Math.round((i + 1) * (mColorAreaRect.height() / ycells)),
									blockPaint);
						}
					}
				}
				
				mTransparencyBitmap = transparencyBitmap;
			}
			canvas.drawBitmap(mTransparencyBitmap, mColorAreaRect.left, mColorAreaRect.top, null);
		}

		if (mMode == MODE_COLOR) {
			canvas.drawRect(mColorAreaRect, mColorAreaPaint);
		} else {
			if (mMode == MODE_SV) {
				Shader colorShader = new LinearGradient(
						(mOrientation & ORIENTATION_DOWN) != 0 ? 1.0f : 0.0f,
						(mOrientation & ORIENTATION_LEFT) != 0 ? 1.0f : 0.0f,
						(mOrientation & ORIENTATION_RIGHT) != 0 ? 1.0f : 0.0f,
						(mOrientation & ORIENTATION_DOWN) != 0 ? 1.0f : 0.0f,
						Color.WHITE, Color.HSVToColor(mHSV), Shader.TileMode.CLAMP);
	        	ComposeShader s = new ComposeShader(mShader, colorShader, PorterDuff.Mode.MULTIPLY);
	        	mColorAreaPaint.setShader(s);
			}

			canvas.save();
			canvas.translate(mColorAreaRect.left, mColorAreaRect.top);
			canvas.scale(mColorAreaRect.width(), mColorAreaRect.height());
			canvas.drawRect(0.0f, 0.0f, 1.0f, 1.0f, mColorAreaPaint);
			canvas.restore();
		}
		
		if (mBorderPaint != null) {
			canvas.drawRect(mColorAreaRect, mBorderPaint);
		}
		
		if (mMarkerPaint != null) {
			if (mMode == MODE_ALPHA || mMode == MODE_HUE) {
				if ((mOrientation & (ORIENTATION_LEFT | ORIENTATION_RIGHT)) != 0) {
					float position = ((mOrientation & ORIENTATION_LEFT) != 0
									  ? 1.0f - mValue[0] : mValue[0])
							* mColorAreaRect.width() + mColorAreaRect.left;
					mMarkerRect.left = position - mMarkerWidth / 2.0f;
					mMarkerRect.right = position + mMarkerWidth / 2.0f;
				} else {
					float position = ((mOrientation & ORIENTATION_UP) != 0
									  ? 1.0f - mValue[0] : mValue[0])
							* mColorAreaRect.height() + mColorAreaRect.top;
					mMarkerRect.top = position - mMarkerWidth / 2.0f;
					mMarkerRect.bottom = position + mMarkerWidth / 2.0f;
				}
				
				canvas.drawRoundRect(mMarkerRect, mMarkerRounding, mMarkerRounding, mMarkerPaint);
				if (mMarkerContourPaint != null) {
					canvas.drawRoundRect(
							mMarkerRect, mMarkerRounding, mMarkerRounding, mMarkerContourPaint);
				}
			} else {
				float xpos = ((mOrientation & ORIENTATION_RIGHT) != 0
							  ? mValue[0] : 1.0f - mValue[0])
						* mColorAreaRect.width() + mColorAreaRect.left;
				float ypos = ((mOrientation & ORIENTATION_DOWN) != 0
							  ? mValue[1] : 1.0f - mValue[1])
						* mColorAreaRect.height() + mColorAreaRect.top;
				
				canvas.drawCircle(xpos, ypos, mMarkerSize / 2.0f, mMarkerPaint);
				if (mMarkerContourPaint != null) {
					if (mMarkerThickness > 0.0f) {
						// Marker is an annulus
						canvas.drawCircle(
								xpos, ypos,
								mMarkerSize / 2.0f - mMarkerThickness / 2.0f,
								mMarkerContourPaint);
						canvas.drawCircle(
								xpos, ypos,
								mMarkerSize / 2.0f + mMarkerThickness / 2.0f,
								mMarkerContourPaint);
					} else {
						// Marker is solid
						canvas.drawCircle(xpos, ypos, mMarkerSize / 2.0f, mMarkerContourPaint);
					}
				}
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(
				clamp(MeasureSpec.getSize(widthMeasureSpec), mMinWidth, mMaxWidth),
				clamp(MeasureSpec.getSize(heightMeasureSpec), mMinHeight, mMaxHeight));
		
		if (mMode == MODE_ALPHA || mMode == MODE_HUE) {
			float lateralPadding = Math.max(mMarkerWidth + mMarkerThickness, mBorderWidth) / 2.0f;
			float longitudinalPadding = Math.max(2.0f * mMarkerSize + mMarkerThickness, mBorderWidth) / 2.0f;
			
			if ((mOrientation & (ORIENTATION_LEFT | ORIENTATION_RIGHT)) != 0) {
				mColorAreaRect.left = Math.max(getPaddingLeft(), lateralPadding);
				mColorAreaRect.top = Math.max(getPaddingTop(), longitudinalPadding);
				mColorAreaRect.right = getMeasuredWidth() - Math.max(getPaddingRight(), lateralPadding);
				mColorAreaRect.bottom = getMeasuredHeight() - Math.max(getPaddingBottom(), longitudinalPadding);
				
				mMarkerRect.top = mColorAreaRect.top - mMarkerSize;
				mMarkerRect.bottom = mColorAreaRect.bottom + mMarkerSize;
			} else {
				mColorAreaRect.left = Math.max(getPaddingLeft(), longitudinalPadding);
				mColorAreaRect.top = Math.max(getPaddingTop(), lateralPadding);
				mColorAreaRect.right = getMeasuredWidth() - Math.max(getPaddingRight(), longitudinalPadding);
				mColorAreaRect.bottom = getMeasuredHeight() - Math.max(getPaddingBottom(), lateralPadding);
				
				mMarkerRect.left = mColorAreaRect.left - mMarkerSize;
				mMarkerRect.right = mColorAreaRect.right + mMarkerSize;
			}
		} else {
			float padding = Math.max(mMarkerSize + mMarkerThickness, mBorderWidth) / 2.0f;
			mColorAreaRect.left = Math.max(getPaddingLeft(), padding);
			mColorAreaRect.top = Math.max(getPaddingTop(), padding);
			mColorAreaRect.right = getMeasuredWidth() - Math.max(getPaddingRight(), padding);
			mColorAreaRect.bottom = getMeasuredHeight() - Math.max(getPaddingBottom(), padding);
		}
		
		if (mBorderWidth == 1.0f) {
			mColorAreaRect.right -= 1;
			mColorAreaRect.bottom -= 1;
		}
		
		mTransparencyBitmap = null;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float xval = clamp((event.getX() - mColorAreaRect.left) / mColorAreaRect.width(), 0.0f, 1.0f);
		float yval = clamp((event.getY() - mColorAreaRect.top) / mColorAreaRect.height(), 0.0f, 1.0f);
		
		if (mMode == MODE_ALPHA || mMode == MODE_HUE) {
			if ((mOrientation & ORIENTATION_LEFT) != 0) {
				mValue[0] = 1.0f - xval;
			} else if ((mOrientation & ORIENTATION_UP) != 0) {
				mValue[0] = 1.0f - yval;
			} else if ((mOrientation & ORIENTATION_RIGHT) != 0) {
				mValue[0] = xval;
			} else if ((mOrientation & ORIENTATION_DOWN) != 0) {
				mValue[0] = yval;
			}
		} else {
			mValue[0] = (mOrientation & ORIENTATION_LEFT) != 0 ? 1.0f - xval : xval;
			mValue[1] = (mOrientation & ORIENTATION_UP) != 0 ? 1.0f - yval : yval;
		}
		
		if (mOnValueChangeListener != null) {
			mOnValueChangeListener.updateValue(mValue, this);
		}
		
		invalidate();
		
		return true;
	}
	
	public void setColor(int color) {
		mColor = color;
		if (mMode == MODE_COLOR) {
			mColorAreaPaint.setColor(mColor);
			invalidate();
		} else if (mMode == MODE_ALPHA) {
			mShader = buildShader(
					mMode == MODE_ALPHA ? buildAlphaColorArray(mColor) : HUE_COLORS);
			mColorAreaPaint.setShader(mShader);
			invalidate();
		}
	}
	
	public void setHue(float hue) {
		if (mMode == MODE_SV) {
			mHSV[0] = hue;
			invalidate();
		}
	}
	
	public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
		mOnValueChangeListener = onValueChangeListener;
	}
	
	public void setValue(float[] value) {
		mValue = value;
		invalidate();
	}
	
	public interface OnValueChangeListener {
		
		public void updateValue(float[] value, Object source);
		
	}
	
}