package org.dal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import android.telephony.PhoneNumberUtils;


public class FEAppActivity extends ListActivity {
	public static final String TAG = "FEActivity";
	public static final boolean QUERY_STATUS = false;
	public static final String PREFS_NAME = "FEApp";
	
	
	public class CallEntryAdapter extends CursorAdapter {
		
		private int col_number, col_date;
		private Context ctx;
		
		public CallEntryAdapter(Context context, Cursor c) {
			super(context, c);
			this.ctx = context;
			this.col_number = c.getColumnIndex(Calls.NUMBER);
			this.col_date = c.getColumnIndex(Calls.DATE);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final String number = cursor.getString(this.col_number);
			Date date = new Date(cursor.getLong(this.col_date));
			DateFormat df = DateFormat.getDateTimeInstance();
			final String date_str = df.format(date);
			
			TextView number_view = (TextView)view.findViewById(android.R.id.text1);
			TextView date_view = (TextView)view.findViewById(android.R.id.text2);
			
			number_view.setText(PhoneNumberUtils.formatNumber(number));
			date_view.setText(date_str);
			
			view.setOnTouchListener(new View.OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					confirm_denounce_number(number);
					return false;
				}
			});
		}
		
		public void confirm_denounce_number(String number)
		{
			final String denounced_number = number;
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this.ctx);
			builder.setMessage(ctx.getString(R.string.denounce_question_format, number));
			builder.setCancelable(false);
			builder.setPositiveButton(ctx.getText(R.string.do_denounce), new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   FEAppActivity.this.do_denounce(denounced_number);
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
	
	public void do_denounce(String number)
	{
		AsyncTask<Object, Void, Integer> denounce_task = new AsyncTask<Object, Void, Integer>() {
			String server_name, username, password, phone_number;
			Context context = null;
			
			@Override
			protected void onPreExecute()
			{
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				server_name = settings.getString("server", "localhost");
				username = settings.getString("username", "");
				password = settings.getString("password", "");
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
	
	
	public void init_prefs()
	{
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		
		if (settings.contains("configured") && settings.getBoolean("configured", false))
		{
			Log.v(TAG, "aplicacion ya configurada");
		}
		else
		{
			Log.v(TAG, "aplicacion no tiene config");
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("configured", true);
			editor.putString("server", NetProto.DEFAULT_SERVER);
			editor.putString("username", "pordefecto");
			editor.putString("password", "pordefecto");
			editor.commit();
		}
	}
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        init_prefs();
        
        Date today = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.v(TAG, "today: " + fmt.format(today));
        
        Cursor cursor = getContentResolver().query(Calls.CONTENT_URI,
        		new String[] {Calls._ID, Calls.NUMBER, Calls.DATE}, 
        		Calls.TYPE + " = " + Calls.INCOMING_TYPE, 
        		null, Calls.DEFAULT_SORT_ORDER + " LIMIT 5");
        startManagingCursor(cursor);
        
        ListAdapter adapter = new CallEntryAdapter(this, cursor);
        setListAdapter(adapter);
        
        //ContactsList.getInstance().dumpContacts(this);
        
        LocalDB db = new LocalDB(this);
        //db.logTableContents();
 
        String last_date = db.queryLastDate();
        Log.v(TAG, "ultima fecha: " + last_date);
        
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
}