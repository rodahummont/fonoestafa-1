package org.dal;

import android.content.Context;
import android.os.Build;

public abstract class ContactsList {	
	private static ContactsList sInstance = null;
	private static final String OldClassname = "ContactsListOldAPI";
	private static final String NewClassname = "ContactsListNewAPI";
	
	public static ContactsList getInstance()
	{
		if (sInstance == null)
		{
			int sdk_version = Integer.parseInt(Build.VERSION.SDK);
			String classname = (sdk_version < Build.VERSION_CODES.ECLAIR) ? 
									OldClassname 
								: 
									NewClassname;
			
			Class<? extends ContactsList> clase;
			try 
			{
				String full_classname = ContactsList.class.getPackage().getName() + "." + classname;
				clase = Class.forName(full_classname).asSubclass(ContactsList.class);
				sInstance = clase.newInstance();
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		
		return sInstance;
	}
	
	public abstract boolean isNumberInContacts(Context context, String number);
	public abstract void dumpContacts(Context context);
}
