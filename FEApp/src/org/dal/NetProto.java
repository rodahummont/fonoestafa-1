package org.dal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;


import android.util.Log;


public class NetProto {
	public static final boolean USE_HTTPS = false;
	
	private static String prefix()
	{
		return USE_HTTPS ? "https://" : "http://";
	}
	
	
	public static class Response
	{
		public boolean found;
		public String since;
		List<String> extra_numbers;
		List<String> extra_dates;
	
		public Response()
		{
			this.found = false;
			this.since = "";
			this.extra_numbers = new LinkedList<String>();
			this.extra_dates = new LinkedList<String>();
		}
	}
	
	//public static final String DEFAULT_SERVER = "fono.joaquinnunez.cl";
	public static final String DEFAULT_SERVER = "10.0.2.2:8000";
	
	public static final String TAG = "NetProto";
	public static final int CONNECTION_ERROR = -1;
	public static final int RESP_OK = 200;
	public static final String REALM = "fonoestafa";

	
	public static DefaultHttpClient makeClient(String userid, String userpass)
	{
		DefaultHttpClient client = new DefaultHttpClient();

		if ((userid != null) && (userpass != null) && !userid.equals("") && !userpass.equals(""))
		{
			Log.v(TAG, "usando credenciales");
			client.getCredentialsProvider().setCredentials(new AuthScope(null, -1, REALM), 
						new UsernamePasswordCredentials(userid, userpass));
		}
		return client;
	}
	
	
	public static String sendNumberRegistration(String server_name, String userid)
	{
		Log.v(TAG, "sendNumberRegistration userid: " + userid);
		HttpClient client = makeClient(null, null);
		
		String uri = prefix() + server_name + "/register";
		HttpPost request = new HttpPost(uri);
		Log.v(TAG, "enviando userid: " + userid);
		
		try {
			request.addHeader("userid", userid);
			HttpResponse resp = client.execute(request);
			if (resp.getStatusLine().getStatusCode() == 200)
			{
				String line = getResponseLine(resp);
				String[] fields = line.split(";");
				if ((fields.length == 2) && (fields[0].equals("OK")))
					return fields[1];
				else
					return null;
			}
			else
				return null;
		}
		catch (IOException e)
		{
			Log.v(TAG, "error: " + e.getMessage());
			return null;	
		}
	}
	
	public static boolean sendRegistrationConfirm(String server_name, String userid, String userpass)
	{
		Log.v(TAG, "sendRegistrationConfirm");
		
		HttpClient client = makeClient(userid, userpass);
		
		String uri = prefix() + server_name + "/register_confirm";

		HttpPost request = new HttpPost(uri);
		request.addHeader("userid", userid);
		try {
			HttpResponse resp = client.execute(request);
			return (resp.getStatusLine().getStatusCode() == 200);
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private static String getResponseLine(HttpResponse resp)
	{
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
			return reader.readLine();
		}
		catch (IOException e)
		{
			Log.v(TAG, "error: " + e.getMessage());
			return null;
		}
	}
	
	public static String queryStatus(String server_name)
	{
		Log.v(TAG, "queryStatus");
		HttpClient client = makeClient(null, null);
		
		String uri_str = prefix() + server_name + "/status";
		HttpGet request = new HttpGet(uri_str);
		try {
			Log.v(TAG, "ejecutando");
			HttpResponse resp = client.execute(request);
			Log.v(TAG, "leyendo");
			String line = getResponseLine(resp);
			Log.v(TAG, "respuesta a status: " + line);
			return line;
		}
		catch (IOException e)
		{
			Log.v(TAG, "error: " + e.getMessage());
			return "";
		}
	}

	
	
	public static int denounce_number(String number, String userid, String userpass, String server_name)
	{	
		HttpClient client = makeClient(userid, userpass);
		
		List<NameValuePair> keyval = new ArrayList<NameValuePair>();
		keyval.add(new BasicNameValuePair("number", "+" + number));
		
		String uri_str = prefix() + server_name + "/denounce";
		Log.v(TAG, "uri: " + uri_str);
		
		HttpPost request = new HttpPost(uri_str);
		
		try {
			request.setEntity(new UrlEncodedFormEntity(keyval, HTTP.UTF_8));
			
			HttpResponse resp = client.execute(request);
			final int status = resp.getStatusLine().getStatusCode();
			Log.v(TAG, "status: " + status);
			
			return status;
		}
		catch (IOException e)
		{
			Log.v(TAG, "error: " +  e.getMessage());
			return CONNECTION_ERROR;
		}
	}
	
	
	public static Response queryNumberAndGetUpdates(String number, String server_name)
	{
		HttpClient client = makeClient(null, null);
		
		List<NameValuePair> keyval = new ArrayList<NameValuePair>(1);
		keyval.add(new BasicNameValuePair("number", number));
		String uri_str = prefix() + server_name + "/lookup?" + URLEncodedUtils.format(keyval, "utf-8");
		
		Log.v(TAG, "uri: |" + uri_str + "|");
		
		HttpGet request = new HttpGet(uri_str);
		
		try {
			HttpResponse resp = client.execute(request);
			BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
			
			Response result = new Response();
			
			String first_line = reader.readLine();
			Log.v(TAG, "respuesta |" + first_line + "|");
			
			String[] fields = first_line.split(";");
			result.found = ((fields.length == 2) && (Integer.parseInt(fields[0]) == 1)); 
			
			if (result.found)
			{
				result.since = fields[1];
				result.extra_numbers.add(number);
				result.extra_dates.add(result.since);
			}
			
			String line;
			while ((line = reader.readLine()) != null)
			{
				Log.v(TAG, "linea update recibida: |" + line + "|");
				
				fields = line.split(";");
				if (fields.length == 2)
				{
					result.extra_numbers.add(fields[0]);
					result.extra_dates.add(fields[1]);
				}
			}
			
			return result;
		}
		catch (IOException e)
		{
			Log.v(TAG, "error de conexion: " + e.getMessage());
			return null;
		}
	}
	
	public static Response getUpdatesForToday(String server_name)
	{
		HttpClient client = makeClient(null, null);
		String uri_str = prefix() + server_name + "/updates";
		
		Log.v(TAG, "consultando: |" + uri_str + "|");
		
		HttpGet request = new HttpGet(uri_str);
		
		try {
			HttpResponse resp = client.execute(request);
			BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
			
			Response result = new Response();
			
			String line;
			while ((line = reader.readLine()) != null)
			{
				Log.v(TAG, "linea update recibida: |" + line + "|");
				
				String[] fields = line.split(";");
				if (fields.length == 2)
				{
					result.extra_numbers.add(fields[0]);
					result.extra_dates.add(fields[1]);
				}
			}
			
			result.found = (result.extra_numbers.size() > 0);
			return result;
		}
		catch (IOException e)
		{
			Log.v(TAG, "error de conexion: " + e.toString());
			return null;
		}
	}
}
