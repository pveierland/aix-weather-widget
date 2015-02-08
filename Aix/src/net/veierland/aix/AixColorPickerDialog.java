package net.veierland.aix;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

public class AixColorPickerDialog extends Dialog {

	public AixColorPickerDialog(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.dialog_edittext);
		setTitle("Ohaihai");
	}

}
