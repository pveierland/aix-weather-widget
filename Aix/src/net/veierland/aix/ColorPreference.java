package net.veierland.aix;

import net.veierland.aix.ColorView.OnValueChangeListener;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.Preference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ColorPreference extends Preference implements android.view.View.OnClickListener, OnValueChangeListener {

	private int mValue;
	private int mDefaultValue;
	
	private ColorView mColorView;
	private View mView;
	private View mRevertView;
	
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
	
    private static class RevertHolder extends ImageView {
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
	protected void onBindView(View view) {
		super.onBindView(view);
		
		mColorView = (ColorView)view.findViewById(R.id.color);
		mColorView.setColor(mValue);
		
		mRevertView = view.findViewById(R.id.revert);
		mRevertView.setOnClickListener(this);
		// Set listview item padding to 0 so revert button matches right edge
		((View)mRevertView.getParent().getParent().getParent()).setPadding(0, 0, 0, 0);
	}

	private void openColorDialog() {
		View content = LayoutInflater.from(getContext()).inflate(R.layout.dialog_color, null);
		
		mAlphaSlider = (ColorView)content.findViewById(R.id.alphaSlider);
		mAlphaSlider.setOnValueChangeListener(this);
		mHueSlider = (ColorView)content.findViewById(R.id.hueSlider);
		mHueSlider.setOnValueChangeListener(this);
		
		mSvMap = (ColorView)content.findViewById(R.id.svMap);
		mSvMap.setOnValueChangeListener(this);
		
		mAlphaTextView = (TextView)content.findViewById(R.id.alphaText);
		mHueTextView = (TextView)content.findViewById(R.id.hueText);
		mSaturationTextView = (TextView)content.findViewById(R.id.saturationText);
		mValueTextView = (TextView)content.findViewById(R.id.valueText);
		
		mColorOld = (ColorView)content.findViewById(R.id.colorOld);
		mColorNew = (ColorView)content.findViewById(R.id.colorNew);
		
		
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
		mColorOld.setColor(mValue); // Change to initial
		mColorNew.setColor(mValue); // Change to initial
		
		updateLabels();
		
		Dialog dialog = new AlertDialog.Builder(getContext())
						.setTitle("Select Color")
						.setView(content)
						.setNeutralButton("Hex Input", new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								hex(Color.HSVToColor(Math.round(mAlpha * 255.0f), mHSV));
							}
							
						})
						.setPositiveButton(android.R.string.ok, new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								setValue(Color.HSVToColor(Math.round(mAlpha * 255.0f), mHSV));
								AixConfigure.mDialog = null;
							}
							
						})
						.setNegativeButton(android.R.string.cancel, new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								AixConfigure.mDialog = null;
							}
							
						})
						.create();
		
		AixConfigure.mDialog = dialog;
		dialog.show();
	}
	
	private void hex(int color) {
		View content = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edittext, null);
		
		final EditText editText = (EditText) content.findViewById(R.id.edittext);
		editText.setInputType(InputType.TYPE_CLASS_TEXT);
		editText.setText("#" + Integer.toHexString(color));
		
		Dialog dialog = new AlertDialog.Builder(getContext())
				.setTitle("Input Hex Color")
                .setView(content)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    	try {
                    		setValue(Color.parseColor(editText.getText().toString()));
                    	} catch (Exception e) {
                    		Toast.makeText(getContext(), "Invalid color code entered", Toast.LENGTH_SHORT).show();
                    	}
                    	AixConfigure.mDialog = null;
                    }})
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                		AixConfigure.mDialog = null;
                    }})
                .create();
		
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN |
				WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

		AixConfigure.mDialog = dialog;
		
		dialog.show();
	}

	@Override
	protected void onClick() {
		super.onClick();
		openColorDialog();
	}

	@Override
	public void onClick(View v) {
		v.post(new Runnable() {
			@Override
			public void run() {
				setValue(mDefaultValue);
			}
		});
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
		
		updateLabels();
		mColorNew.setColor(rgb);
	}

	private void setValue(int value) {
		mValue = value;
		if (mColorView != null) {
			mColorView.setColor(value);
		}
		persistInt(value);
		notifyChanged();
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		mDefaultValue = a.getInteger(index, 0);
		return mDefaultValue;
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue) {
		setValue(restorePersistedValue ? getPersistedInt(mValue) : (Integer) defaultValue);
	}

}
