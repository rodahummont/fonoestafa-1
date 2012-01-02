package org.dal;

import android.content.Context;
import android.database.Cursor;
import android.provider.Contacts;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

@SuppressWarnings("deprecation")
public class ContactsListOldAPI extends ContactsList {
	public static final String TAG = "ContactsListOldAPI";
	
	@Override
	public boolean isNumberInContacts(Context context, String number) 
	{
		String normalized_number = PhoneNumberUtils.formatNumber(number);
		Log.v(TAG, "number: " + number + ", normalized: " + normalized_number);
		
		String cols[] = { Contacts.PhonesColumns.NUMBER };
		String where = Contacts.PhonesColumns.NUMBER + " = '" + normalized_number + "'";
		Cursor c = context.getContentResolver().query(Contacts.Phones.CONTENT_URI, cols, where, null, null);
		Log.v(TAG, "encontrado " + normalized_number + " " + c.getCount() + " veces");
		return c.getCount() > 0;
	}
	
	@Override
	public void dumpContacts(Context context)
	{
		Log.v(TAG, "listando contactos");
		String cols[] = { Contacts.PhonesColumns.NUMBER_KEY, Contacts.PhonesColumns.NUMBER };
		Cursor c = context.getContentResolver().query(Contacts.Phones.CONTENT_URI, cols, null, null, null);
		Log.v(TAG, "contactos: " + c.getCount());
		if (c.getCount() > 0)
		{
			c.moveToFirst();
			for (int i=0; i<c.getCount(); i++)
			{
				Log.v(TAG, "number key: " + c.getString(0) + ", number: " + c.getString(1));
				c.moveToNext();
			}
		}
	}
}
