package org.dal;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

public class ContactsListNewAPI extends ContactsList {
	public static final String TAG = "ContactsListNewAPI";
	
	@Override
	public boolean isNumberInContacts(Context context, String number) 
	{
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, number);
		String cols[] = { PhoneLookup.NUMBER };
		Cursor c = context.getContentResolver().query(uri, cols, null, null, null);
		Log.v(TAG, "encontrado " + number + " " + c.getCount() + " veces");
		return c.getCount() > 0;
	}
	
	
	@Override
	public void dumpContacts(Context context)
	{
		String cols[] = {Phone.NUMBER};
		Cursor c = context.getContentResolver().query(Phone.CONTENT_URI, cols, null, null, null);
		Log.v(TAG, "contactos: " + c.getCount());
		if (c.getCount() > 0)
		{
			c.moveToFirst();
			for (int i=0; i<c.getCount(); i++)
			{
				Log.v(TAG, "number:" + c.getString(0));
				c.moveToNext();
			}
		}
	}

}
