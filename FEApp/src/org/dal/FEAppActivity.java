package org.dal;

import java.text.DateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.PhoneNumberUtils;


public class FEAppActivity extends ListActivity {
	public static final String TAG = "FEActivity";
	public static final boolean QUERY_STATUS = false;
	public static final String PREFS_NAME = "FEApp";
	
	public static final int DO_REGISTER = 1;
	
	
	
	private class CallEntryAdapter extends CursorAdapter {
		public CallEntryAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final int col_number = cursor.getColumnIndex(Calls.NUMBER);
			final int col_date = cursor.getColumnIndex(Calls.DATE);
			final String number = cursor.getString(col_number);
			Date date = new Date(cursor.getLong(col_date));
			DateFormat df = DateFormat.getDateTimeInstance();
			final String date_str = df.format(date);
			
			TextView number_view = (TextView)view.findViewById(android.R.id.text1);
			TextView date_view = (TextView)view.findViewById(android.R.id.text2);
			
			number_view.setText(PhoneNumberUtils.formatNumber(number));
			date_view.setText(date_str);
			
			view.setOnTouchListener(new View.OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					if (FEAppActivity.this.appIsConfirmed())
					{
						confirmDenounceNumber(number);
					}
					else
					{
						Toast toast = Toast.makeText(FEAppActivity.this, "no estamos confirmados", Toast.LENGTH_SHORT);
						toast.show();
					}
					return false;
				}
			});
		}

		
		public void confirmDenounceNumber(String number)
		{
			final String denounced_number = number;
			
			Context ctx = FEAppActivity.this;
			
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder.setMessage(ctx.getString(R.string.denounce_question_format, number));
			builder.setCancelable(false);
			builder.setPositiveButton(ctx.getText(R.string.do_denounce), new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   FEAppActivity.this.doDenounce(denounced_number);
			   			   FEAppActivity.this.finish();
			           }
			       });
			builder.setNegativeButton(ctx.getText(R.string.ignore), new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();
		}
	 
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);
			View v = inflater.inflate(android.R.layout.two_line_list_item, parent, false);
			bindView(v, context, cursor);
			return v;
		}
	}
	
	public void doDenounce(String number)
	{
		AsyncTask<Object, Void, Integer> denounce_task = new AsyncTask<Object, Void, Integer>() {
			String server_name, username, password, phone_number;
			Context context = null;
			
			@Override
			protected void onPreExecute()
			{
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				server_name = settings.getString("server", "localhost");
				username = settings.getString("userid", "");
				password = settings.getString("userpass", "");
			}
			
			@Override
			protected Integer doInBackground(Object... args)
			{
				phone_number = (String)args[0];
				context = (Context)args[1];
				int status = NetProto.denounce_number(phone_number, username, password, server_name);
				Log.v(TAG, "status: " + status);
				return new Integer(status);
			}
			
			@Override
			protected void onPostExecute(Integer result)
			{
				int status = result;
				if (true)
				{
					CharSequence contentTitle, contentText, tickerText;
					
					switch (status) {
						case NetProto.RESP_OK:
							tickerText = context.getText(R.string.denounce_done);
							contentTitle = context.getString(R.string.the_number_NNN, 
															PhoneNumberUtils.formatNumber(phone_number));
							contentText = context.getText(R.string.has_been_denounced);
							break;
					
						default:
							tickerText = context.getText(R.string.connection_error);
							contentTitle = tickerText;
							contentText = "";
					}
										
					int icon = R.drawable.ic_notif;
					long when = System.currentTimeMillis();
					Notification notification = new Notification(icon, tickerText, when);
					Intent notificationIntent = new Intent(context, PhoneStateReceiver.class);
					PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
					notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
					NotificationManager notif_manager = 
							(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
					notif_manager.notify(1, notification);
				}
			}
		};
		
		denounce_task.execute(number, this);
	}
	
	/*
	private boolean preferencesInitialized()
	{
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		
		if (!settings.contains("server") || settings.getString("server", "").equals(""))
		{
			Log.v(TAG, "aplicacion no tiene config");
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("server", NetProto.DEFAULT_SERVER);
			editor.putString("userid", "");
			editor.putString("userpass", "");
			editor.putBoolean("registering", false);
			editor.putBoolean("confirmed", false);
			editor.commit();
			return false;
		}
		
		return true;
	}
	*/
	
	
	private void initializePreferences()
	{
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		
		if (!settings.contains("server") || settings.getString("server", "").equals(""))
		{
			Log.v(TAG, "aplicacion no tiene config");
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("server", NetProto.DEFAULT_SERVER);
			editor.putString("userid", "");
			editor.putString("userpass", "");
			editor.putBoolean("registering", false);
			editor.putBoolean("confirmed", false);
			editor.commit();
		}
	}
	
	
	public boolean appIsRegistering()
	{
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		return settings.getBoolean("registering", false);
	}
	
	public boolean appIsConfirmed()
	{
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		return settings.getBoolean("confirmed", false);
	}
	
	
	@Override
	public void onResume()
	{
		Log.v(TAG, "onResume!!!!");
		super.onResume();
		
		if (!appIsRegistering())
        {
        	Log.v(TAG, "hay que registrarse");
        	Intent myIntent = new Intent(this, RegisterActivity.class);
            startActivityForResult(myIntent, DO_REGISTER);
            Log.v(TAG, "....");
        }
        else
        	Log.v(TAG, "ya estamos registrados/regsitrando");
        
        setContentView(R.layout.main);
        
        Cursor cursor = getContentResolver().query(Calls.CONTENT_URI,
        		new String[] {Calls._ID, Calls.NUMBER, Calls.DATE}, 
        		Calls.TYPE + " = " + Calls.INCOMING_TYPE, 
        		null, Calls.DEFAULT_SORT_ORDER + " LIMIT 5");
        startManagingCursor(cursor);
        
        ListAdapter adapter = new CallEntryAdapter(this, cursor);
        setListAdapter(adapter);
        
        LocalDB db = new LocalDB(this);
        
        if (QUERY_STATUS)
        {
        	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        	String server_name = settings.getString("server", "localhost");
        	
        	String remote_status = NetProto.queryStatus(server_name);
        	if (remote_status.equals("EMPTY"))
        	{
        		Log.v(TAG, "limpiando la tabla local");
        		db.cleanDatabase();
        	}
        }
        
        db.close();
	}
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        initializePreferences();
        
        /*
        if (!appIsRegistering())
        {
        	Log.v(TAG, "hay que registrarse");
        	Intent myIntent = new Intent(this, RegisterActivity.class);
            startActivityForResult(myIntent, DO_REGISTER);
        }
        else
        	Log.v(TAG, "ya estamos registrados/regsitrando");
        
        setContentView(R.layout.main);
        
        Cursor cursor = getContentResolver().query(Calls.CONTENT_URI,
        		new String[] {Calls._ID, Calls.NUMBER, Calls.DATE}, 
        		Calls.TYPE + " = " + Calls.INCOMING_TYPE, 
        		null, Calls.DEFAULT_SORT_ORDER + " LIMIT 5");
        startManagingCursor(cursor);
        
        ListAdapter adapter = new CallEntryAdapter(this, cursor);
        setListAdapter(adapter);
        
        LocalDB db = new LocalDB(this);
        
        if (QUERY_STATUS)
        {
        	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        	String server_name = settings.getString("server", "localhost");
        	
        	String remote_status = NetProto.queryStatus(server_name);
        	if (remote_status.equals("EMPTY"))
        	{
        		Log.v(TAG, "limpiando la tabla local");
        		db.cleanDatabase();
        	}
        }
        
        db.close();
        */
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.history:
        	Log.v(TAG, "selecionada historial");
        	return true;
        	
        case R.id.preferences:
        	Log.v(TAG, "seleccionada preferencias");
        	Intent i = new Intent(this, Preferences.class);
            startActivity(i);
        	return true;
        	
        default:
        	return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	switch (requestCode)
    	{
    	case DO_REGISTER:
    		switch (resultCode) {
    		case RegisterActivity.REGISTRATION_OK:
    			Log.v(TAG, "registracion ok");
    			break;
    			
    		case RegisterActivity.REGISTRATION_CANCELED:
    			Log.v(TAG, "registracion cancelada");
    			break;
    			
    		case RegisterActivity.REGISTRATION_FAILED:
    			Log.v(TAG, "registracion fallada");
    			break;
    			
    		default:
    			// nadaremos
    		}
    		break;
    		
    	default:
    		// nadaremos
    	}
    }
}