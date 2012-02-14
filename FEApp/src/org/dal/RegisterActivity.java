package org.dal;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class RegisterActivity extends Activity {
	public static final int REGISTRATION_OK = 1;
	public static final int REGISTRATION_CANCELED = 2;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.register);
	    
	    Button button = (Button)findViewById(R.id.register_run);
	    button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				RegisterActivity.this.setResult(RegisterActivity.REGISTRATION_OK);
				
				Toast toast = Toast.makeText(RegisterActivity.this, 
								RegisterActivity.this.getText(R.string.register_ok), Toast.LENGTH_SHORT);
				toast.show();
				
				RegisterActivity.this.finish();
			}
		});
	    
	    setResult(REGISTRATION_CANCELED);
	}

}
