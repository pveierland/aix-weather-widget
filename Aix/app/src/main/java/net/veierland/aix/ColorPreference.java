package net.veierland.aix;

import net.veierland.aix.ColorView.OnValueChangeListener;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ColorPreference extends DialogPreference implements android.view.View.OnClickListener, OnValueChangeListener {

	// private final static String TAG = "AixColorPreference";
	
	private int mValue;
	private int mDefaultValue;
	
	private ColorView mColorView;
	private View mRevertView;
	
	private boolean showHexDialog = false;
	
	/* Dialog stuff */
	private ColorView mAlphaSlider;
	private ColorView mColorNew;
	private ColorView mColorOld;
	private ColorView mHueSlider;
	private ColorView mSvMap;
	
	private TextView mAlphaTextView;
	private TextView mHueTextView;
	private TextView mSaturationTextView;
	private TextView mValueTextView;
	
	private float[] mHSV = new float[3];
	private float mAlpha;
	
	private EditText mEditText;
	
    @SuppressWarnings("unused")
	public static class RevertHolder extends ImageView {
        public RevertHolder(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public void setPressed(boolean pressed) {
            // If the parent is pressed, do not set to pressed.
            if (pressed && ((View) getParent()).isPressed()) {
                return;
            }
            super.setPressed(pressed);
        }
    }
	
	public ColorPreference(Context context) {
		this(context, null);
	}

	public ColorPreference(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.colorPreferenceStyle);
	}
	
	public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected void onBindDialogView(View view) {
		if (showHexDialog) {
			int color = Color.HSVToColor(Math.round(mAlpha * 255.0f), mHSV);
			
			mEditText = (EditText)view.findViewById(R.id.edittext);
			mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			mEditText.setText(String.format("#%08X", color));
		} else {
			mAlphaSlider = (ColorView)view.findViewById(R.id.alphaSlider);
			mAlphaSlider.setOnValueChangeListener(this);
			mHueSlider = (ColorView)view.findViewById(R.id.hueSlider);
			mHueSlider.setOnValueChangeListener(this);
			
			mSvMap = (ColorView)view.findViewById(R.id.svMap);
			mSvMap.setOnValueChangeListener(this);
			
			mAlphaTextView = (TextView)view.findViewById(R.id.alphaText);
			mHueTextView = (TextView)view.findViewById(R.id.hueText);
			mSaturationTextView = (TextView)view.findViewById(R.id.saturationText);
			mValueTextView = (TextView)view.findViewById(R.id.valueText);
			
			mColorOld = (ColorView)view.findViewById(R.id.colorOld);
			mColorNew = (ColorView)view.findViewById(R.id.colorNew);
			
			setupDialogValues();
			updateLabels();
		}
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		
		mColorView = (ColorView)view.findViewById(R.id.color);
		mColorView.setColor(mValue);
		
		mRevertView = view.findViewById(R.id.revert);
		mRevertView.setOnClickListener(this);
		
		// Set listview item padding to 0 so revert button matches right edge
		((View)mRevertView.getParent().getParent().getParent()).setPadding(0, 0, 0, 0);
	}
	
	@Override
	protected void onClick() {
		showHexDialog = false;
		setDialogLayoutResource(R.layout.dialog_color);
		super.onClick();
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		super.onClick(dialog, which);
		
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			if (showHexDialog) {
				try {
            		setValue(Color.parseColor(mEditText.getText().toString()));
            	} catch (Exception e) {
            		Toast.makeText(getContext(), "Invalid color code entered", Toast.LENGTH_SHORT).show();
            	}
			} else {
				setValue(Color.HSVToColor(Math.round(mAlpha * 255.0f), mHSV));
			}
			break;
		case DialogInterface.BUTTON_NEUTRAL:
			mRevertView.postDelayed(new Runnable() {
				@Override
				public void run() {
					showHexDialog = true;
					setDialogLayoutResource(R.layout.dialog_edittext);
					showDialog(null);
				}
			}, 100);
			break;
		case DialogInterface.BUTTON_NEGATIVE:
			// TODO: End the world?
			break;
		}
	}

	/* mRevertView onClick() */
	@Override
	public void onClick(View v) {
		v.post(new Runnable() {
			@Override
			public void run() {
				setValue(mDefaultValue);
			}
		});
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		mDefaultValue = a.getInteger(index, 0);
		return mDefaultValue;
	}
	
	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		super.onPrepareDialogBuilder(builder);
		
		if (showHexDialog) {
			builder.setTitle("Input Hex Color");
		}
		
		builder.setPositiveButton(android.R.string.ok, this);
		builder.setNegativeButton(android.R.string.cancel, this);
		
		if (!showHexDialog) {
			builder.setNeutralButton("Hex Input", this);	
		}
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue)
	{
		setValue(restorePersistedValue ? getPersistedInt(mValue) : (Integer) defaultValue);
	}
	
	private void setupDialogValues() {
		mHSV = new float[3];
		
		Color.RGBToHSV(
				Color.red(mValue),
				Color.green(mValue),
				Color.blue(mValue),
				mHSV);
		
		mAlpha = (float)Color.alpha(mValue) / 255.0f;
		
		mHueSlider.setValue(new float[] { mHSV[0] / 360.0f });
		mAlphaSlider.setValue(new float[] { mAlpha });
		mAlphaSlider.setColor(mValue);
		mSvMap.setValue(new float[] { mHSV[1], mHSV[2] });
		mSvMap.setHue(mHSV[0]);
		mColorOld.setColor(mValue);
		mColorNew.setColor(mValue);
	}
	
	private void setValue(int value) {
		mValue = value;
		if (mColorView != null) {
			mColorView.setColor(value);
		}
		persistInt(value);
		notifyChanged();
	}
	
	private void updateLabels() {
		mAlphaTextView.setText(String.format("A: %.0f%%", mAlpha * 100.0f));
		mHueTextView.setText(String.format("H: %.0f\u00b0", mHSV[0]));
		mSaturationTextView.setText(String.format("S: %.0f%%", mHSV[1] * 100.0f));
		mValueTextView.setText(String.format("V: %.0f%%", mHSV[2] * 100.0f));
	}
	
	@Override
	public void updateValue(float[] value, Object source) {
		if (source == mAlphaSlider) {
			mAlpha = value[0];
		} else if (source == mHueSlider) {
			mHSV[0] = value[0] * 360.0f;
			mSvMap.setHue(mHSV[0]);
		} else if (source == mSvMap) {
			mHSV[1] = value[0];
			mHSV[2] = value[1];
		}
		
		int rgb = Color.HSVToColor(Math.round(mAlpha * 255.0f), mHSV);
		
		if (source != mAlphaSlider) {
			mAlphaSlider.setColor(rgb);
		}
		
		mColorNew.setColor(rgb);
		
		updateLabels();
	}

}
