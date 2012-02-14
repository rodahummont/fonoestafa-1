package org.dal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;


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
	
	public static final String DEFAULT_SERVER = "fono.joaquinnunez.cl";
	
	public static final String TAG = "NetProto";
	public static final int CONNECTION_ERROR = -1;
	public static final int RESP_OK = 200;
	public static final String REALM = "fonoestafa";
	
	
	public static void testDigestAuth()
	{
		Log.v(TAG, "testDigestAuth");
		
		DefaultHttpClient client = new DefaultHttpClient();
		client.getCredentialsProvider().setCredentials(new AuthScope(null, -1, REALM), 
				new UsernamePasswordCredentials("miusuario", "miclave"));
		
		String uri_str = "http://10.0.2.2:8000/status";
		HttpGet req1 = new HttpGet(uri_str);
		
		try {
			HttpResponse resp1 = client.execute(req1);

			ByteArrayOutputStream v2 = new ByteArrayOutputStream();
			resp1.getEntity().writeTo(v2);
			
			Log.v(TAG, "respuesta (" + resp1.getStatusLine().getStatusCode() + "): " + v2.toString());
		} catch (ClientProtocolException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	
	public static DefaultHttpClient makeHTTPClient()
	{
		DefaultHttpClient client = new DefaultHttpClient();
		
		return client;
	}
	
	
	public static String queryStatus(String server_name)
	{
		Log.v(TAG, "queryStatus");
		HttpClient client = makeHTTPClient();
		
		String uri_str = prefix() + server_name + "/status";
		HttpGet request = new HttpGet(uri_str);
		try {
			Log.v(TAG, "ejecutando");
			HttpResponse resp = client.execute(request);
			Log.v(TAG, "leyendo");
			BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
			String line = reader.readLine();
			Log.v(TAG, "respuesta a status: " + line);
			return line;
		}
		catch (IOException e)
		{
			Log.v(TAG, "error: " + e.getMessage());
			return "";
		}
	}

	
	
	public static int denounce_number(String number, String username, String password, String server_name)
	{
		Log.v(TAG, "denunciando { server: |" + server_name + "|, user: |" + username + "|, pass: |" + password + "|");
		
		HttpClient client = makeHTTPClient();
		
		List<NameValuePair> keyval = new ArrayList<NameValuePair>(3);
		keyval.add(new BasicNameValuePair("number", "+" + number));
		keyval.add(new BasicNameValuePair("the_hash", "0883850393838"));
		
		keyval.add(new BasicNameValuePair("user", username));
		keyval.add(new BasicNameValuePair("password", password));
		
		String uri_str = prefix() + server_name + "/denounce";
		Log.v(TAG, "uri: " + uri_str);
		
		HttpPost request = new HttpPost(uri_str);
	
		try {
			UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(keyval, "utf-8");
			Log.v(TAG, "uefe: " + uefe.toString());
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			request.setEntity(new UrlEncodedFormEntity(keyval, "utf-8"));
			
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
		HttpClient client = makeHTTPClient();
		
		List<NameValuePair> keyval = new ArrayList<NameValuePair>(2);
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
		HttpClient client = makeHTTPClient();
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
