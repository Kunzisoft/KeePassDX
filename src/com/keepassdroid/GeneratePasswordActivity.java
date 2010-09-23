package com.keepassdroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.password.PasswordGenerator;

public class GeneratePasswordActivity extends LockCloseActivity {
	private static final int[] BUTTON_IDS = new int [] {R.id.btn_length4, R.id.btn_length6, R.id.btn_length8, R.id.btn_length12, R.id.btn_length16};
	
	public static void Launch(Activity act) {
		Intent i = new Intent(act, GeneratePasswordActivity.class);
		
		act.startActivityForResult(i, 0);
	}
	
	private OnClickListener lengthButtonsListener = new OnClickListener() {
	    public void onClick(View v) {
	    	Button button = (Button) v;
	    	
	    	EditText editText = (EditText) findViewById(R.id.length);
	    	editText.setText(button.getText());
	    }
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.generate_password);
		setResult(KeePass.EXIT_NORMAL);
		
		for (int id : BUTTON_IDS) {
        	Button button = (Button) findViewById(id);
        	button.setOnClickListener(lengthButtonsListener);
		}
		
		Button genPassButton = (Button) findViewById(R.id.generate_password_button);
        genPassButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String password = generatePassword();
				
				EditText txtPassword = (EditText) findViewById(R.id.password);
				txtPassword.setText(password);
			}
		});
        
        Button acceptButton = (Button) findViewById(R.id.accept_button);
        acceptButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				EditText password = (EditText) findViewById(R.id.password);
				
				Intent intent = new Intent();
				intent.putExtra("com.keepassdroid.password.generated_password", password.getText().toString());
				
				setResult(RESULT_OK, intent);
				
				finish();
			}
		});
        
        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				
				finish();
			}
		});
	}
	
    public String generatePassword() {
    	String password = "";
    	
    	try {
    		int length = new Integer(((EditText) findViewById(R.id.length)).getText().toString());
    		
    		((CheckBox) findViewById(R.id.cb_uppercase)).isChecked();
        	
        	PasswordGenerator generator = new PasswordGenerator(this);
       	
	    	password = generator.generatePassword(length,
	    			((CheckBox) findViewById(R.id.cb_uppercase)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_lowercase)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_digits)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_minus)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_underline)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_space)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_specials)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_brackets)).isChecked());
    	} catch (NumberFormatException e) {
    		Toast.makeText(this, R.string.error_wrong_length, Toast.LENGTH_LONG).show();
		} catch (IllegalArgumentException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
    	
    	return password;
    }
}
