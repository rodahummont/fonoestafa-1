package org.dal;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
//import android.widget.Toast;

public class RegisterActivity extends Activity {
	public static final String TAG = "RegisterActivity";
	public static final int REGISTRATION_OK = 1;
	public static final int REGISTRATION_CANCELED = 2;
	public static final int REGISTRATION_FAILED = 3;
	
	public static final boolean SEND_REGISTRATION_NOW = true;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.register);
	    
	    Button button = (Button)findViewById(R.id.register_run);
	    button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				SharedPreferences settings = getSharedPreferences(FEAppActivity.PREFS_NAME, 0);
				final String server_name = settings.getString("server", "localhost");
				
				// TODO: conseguir hash del numero de telefono
				final String phone_number_hash = "69696969";
				
				String userpass = NetProto.sendNumberRegistration(server_name, phone_number_hash);
				if (userpass != null)
				{
					Log.v(TAG, "registracion enviada, recibido password: " + userpass);
					SharedPreferences.Editor edit = settings.edit();
					edit.putString("userid", phone_number_hash);
					edit.putString("userpass", userpass);
					edit.putBoolean("registering", true);
					
					if (SEND_REGISTRATION_NOW && 
						NetProto.sendRegistrationConfirm(server_name, phone_number_hash, userpass))
					{
						Log.v(TAG, "estamos confirmados");
						edit.putBoolean("confirmed", true);
					}
					
					edit.commit();
					Log.v(TAG, "si funciona");
					RegisterActivity.this.setResult(RegisterActivity.REGISTRATION_OK);
				}
				else
				{
					Log.v(TAG, "no funciono");
					RegisterActivity.this.setResult(RegisterActivity.REGISTRATION_FAILED);
				}
				
				RegisterActivity.this.finish();
			}
		});
	    
	    setResult(REGISTRATION_CANCELED);
	}

}
