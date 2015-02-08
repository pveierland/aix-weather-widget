package net.veierland.aix;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class ColorPickerView extends View {
	
	private static final int[] mColors = new int[] {
    	0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,
    	0xFFFFFF00, 0xFFFF0000
	};
	
	private boolean mTrackingCenter;
    private boolean mHighlightCenter;
    private int mAlpha = 100;
    private String mColorId;
    
    private OnColorSelectedListener mSelectedListener = null;
    private OnColorUpdatedListener mUpdatedListener = null;
    private Paint mPaint;
    private Paint mCenterPaint;
    
    private Dialog mDialog;
    
    public interface OnColorSelectedListener {
    	void colorSelected(String colorId, int color);
    }
    
    public interface OnColorUpdatedListener {
        void colorUpdated(int color);
    }

    public ColorPickerView(Context context) {
        super(context);
        initialize(context);
    }
    
    public ColorPickerView(Context context, AttributeSet attributeSet) {
    	super(context, attributeSet);
    	initialize(context);
    }
    
    public ColorPickerView(Context context, AttributeSet attributeSet, int defStyle) {
    	super(context, attributeSet, defStyle);
    	initialize(context);
    }

    private void initialize(Context context) {
        Shader s = new SweepGradient(0, 0, mColors, null);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setShader(s);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(32);

        mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCenterPaint.setStrokeWidth(5);
    }
    
    public void setColorId(String colorId) {
    	mColorId = colorId;
    }
    
    public void setDialog(Dialog dialog) {
    	mDialog = dialog;
    }

    public void setOnColorUpdatedListener(OnColorUpdatedListener listener) {
    	mUpdatedListener = listener;
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
    	mSelectedListener = listener;
    }
    
    public void setAlpha(int alpha) {
    	mCenterPaint.setAlpha(alpha);
    	mAlpha = alpha;
    	invalidate();
    }
    
    public void setColor(int color) {
    	mAlpha = Color.alpha(color);
    	mCenterPaint.setColor(color);
    	invalidate();
    }
    
    public int getColor() {
    	return mCenterPaint.getColor();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        float r = CENTER_X - mPaint.getStrokeWidth()*0.5f;

        canvas.translate(CENTER_X, CENTER_X);

        canvas.drawOval(new RectF(-r, -r, r, r), mPaint);
        canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);

        if (mTrackingCenter) {
            int c = mCenterPaint.getColor();
            mCenterPaint.setStyle(Paint.Style.STROKE);

            if (mHighlightCenter) {
                mCenterPaint.setAlpha(0xFF);
            } else {
                mCenterPaint.setAlpha(0x80);
            }
            canvas.drawCircle(0, 0,
                              CENTER_RADIUS + mCenterPaint.getStrokeWidth(),
                              mCenterPaint);

            mCenterPaint.setStyle(Paint.Style.FILL);
            mCenterPaint.setColor(c);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(CENTER_X*2, CENTER_Y*2);
    }

    private static final int CENTER_X = 100;
    private static final int CENTER_Y = 100;
    private static final int CENTER_RADIUS = 32;

    private int ave(int s, int d, float p) {
        return s + java.lang.Math.round(p * (d - s));
    }

    private int interpColor(int colors[], float unit) {
        if (unit <= 0) {
            return colors[0];
        }
        if (unit >= 1) {
            return colors[colors.length - 1];
        }

        float p = unit * (colors.length - 1);
        int i = (int)p;
        p -= i;

        // now p is just the fractional part [0...1) and i is the index
        int c0 = colors[i];
        int c1 = colors[i+1];
        int a = mAlpha; //ave(Color.alpha(c0), Color.alpha(c1), p);
        int r = ave(Color.red(c0), Color.red(c1), p);
        int g = ave(Color.green(c0), Color.green(c1), p);
        int b = ave(Color.blue(c0), Color.blue(c1), p);

        return Color.argb(a, r, g, b);
    }

    private static final float PI = 3.1415926f;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX() - CENTER_X;
		float y = event.getY() - CENTER_Y;
		boolean inCenter = java.lang.Math.sqrt(x * x + y * y) <= CENTER_RADIUS;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mTrackingCenter = inCenter;
			if (inCenter) {
				mHighlightCenter = true;
				invalidate();
				break;
			}
		case MotionEvent.ACTION_MOVE:
			if (mTrackingCenter) {
				if (mHighlightCenter != inCenter) {
					mHighlightCenter = inCenter;
					invalidate();
				}
			} else {
				float angle = (float) java.lang.Math.atan2(y, x);
				// need to turn angle [-PI ... PI] into unit [0....1]
				float unit = angle / (2 * PI);
				if (unit < 0) {
					unit += 1;
				}
				int color = interpColor(mColors, unit);
				if (mUpdatedListener != null) {
					mUpdatedListener.colorUpdated(color);
				}
				mCenterPaint.setColor(color);
				invalidate();
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mTrackingCenter) {
				if (inCenter && mSelectedListener != null) {
					mSelectedListener.colorSelected(mColorId, mCenterPaint.getColor());
					mDialog.dismiss();
				} else {
					mTrackingCenter = false; // so we draw w/o halo
					invalidate();
				}
			}
			break;
		}
		return true;
	}
    
	public static class ColorPickerDialogMediator {
		
		Context mContext;
		ColorPickerView mColorPicker;
		TextView mAlphaLabel;
		SeekBar mSeekBar;
		EditText mEditText;
		
		// This variable is set to true when a property is being changed, to avoid listeners
		// triggering new changes etc.
		boolean mChanging;
		
		public ColorPickerDialogMediator(Context context, ColorPickerView colorPicker,
				TextView alphaLabel, SeekBar seekBar, EditText editText)
		{
			mContext = context;
			mColorPicker = colorPicker;
			mAlphaLabel = alphaLabel;
			mSeekBar = seekBar;
			mEditText = editText;
		}
		
		public void updateColorPicker(int color) {
			mColorPicker.setColor(color);
		}
		
		public void updateAlphaLabel(int color) {
			mAlphaLabel.setText((Color.alpha(color) * 100 / 255) + "%");
		}

		public void updateSeekBar(int color) {
			mSeekBar.setProgress(Color.alpha(color) * 100 / 255);
		}
		
		public void updateEditText(int color) {
			mEditText.setText(String.format("#%08X", color));
		}
		
		public void colorPickerUpdate(int color) {
			if (!mChanging) {
				mChanging = true;
				updateEditText(color);
				mChanging = false;
			}
		}
		
		public void seekBarChanged(int alpha) {
			if (!mChanging) {
				mChanging = true;
				// Make sure that keyboard is closed when seeker is being used
				InputMethodManager imm = (InputMethodManager)mContext.getSystemService(
						Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
				mColorPicker.setAlpha(alpha);
				int color = mColorPicker.getColor();
				updateAlphaLabel(color);
				updateEditText(color);
				mChanging = false;
			}
		}
		
		public void editTextChanged(String s) {
			if (!mChanging) {
				mChanging = true;
				try {
					int color = Color.parseColor(s);
		    		updateColorPicker(color);
		    		updateAlphaLabel(color);
		    		updateSeekBar(color);
		    	} catch (Exception e) {
		    		/* Ignore exception */
		    	}
				mChanging = false;
			}
		}
		
		public void setColor(int color) {
			if (!mChanging) {
				mChanging = true;
				updateColorPicker(color);
				updateAlphaLabel(color);
				updateSeekBar(color);
				updateEditText(color);
				mChanging = false;
			}
		}
		
	}
	
	public static void showColorPickerDialog(final Context context, LayoutInflater inflater,
			String colorId, int initialColor, OnColorSelectedListener listener) {
		View content = inflater.inflate(R.layout.dialog_selectcolor, null);
		
		final ColorPickerView colorPicker = (ColorPickerView)content.findViewById(R.id.dialog_color_picker);
		final TextView alphaLabel = (TextView)content.findViewById(R.id.dialog_color_alpha_indicator);
		final SeekBar seekBar = (SeekBar)content.findViewById(R.id.dialog_color_alpha_seeker);
		final EditText colorValue = (EditText)content.findViewById(R.id.dialog_color_value);
		
		final ColorPickerDialogMediator mediator = new ColorPickerDialogMediator(
				context, colorPicker, alphaLabel, seekBar, colorValue);
		
		colorPicker.setColorId(colorId);
		colorPicker.setOnColorUpdatedListener(new OnColorUpdatedListener() {
			@Override
			public void colorUpdated(int color) {
				mediator.colorPickerUpdate(color);
			}
		});
		
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { }
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser)
			{
				mediator.seekBarChanged(progress * 255 / 100);
			}
		});
		
		colorValue.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) { }
			
			@Override
			public void afterTextChanged(Editable s) {
				mediator.editTextChanged(s.toString());
			}
		});
		
		if (listener != null) {
			colorPicker.setOnColorSelectedListener(listener);
		}
		
		mediator.setColor(initialColor);
		
		Dialog dialog = new AlertDialog.Builder(context)
				.setTitle(R.string.dialog_color_title)
				.setView(content)
				.create();
		
		colorPicker.setDialog(dialog);
		dialog.show();
	}

}