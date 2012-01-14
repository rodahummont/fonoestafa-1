package org.dal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;


//import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.util.Log;


public class NetProto {
	public static final boolean USE_HTTPS = true;
	
	private static String prefix()
	{
		return USE_HTTPS ? "https://" : "http://";
	}
	
	public static class FullX509TrustManager implements javax.net.ssl.X509TrustManager {
	    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException 
	    {
	        // Oh, I am easy!
	    	Log.v(TAG, "checkclientTrusted");
	    }

	    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException 
	    {
	        // Oh, I am easy!
	    	Log.v(TAG, "checkServerTrusted");
	    }

	    public X509Certificate[] getAcceptedIssuers() 
	    {
	    	Log.v(TAG, "getAcceptedIssuers");
	        return null;
	    }
	    
	};
	
	public static class CustomSSLSocketFactory extends org.apache.http.conn.ssl.SSLSocketFactory
	{
		private javax.net.ssl.SSLSocketFactory FACTORY = HttpsURLConnection.getDefaultSSLSocketFactory();

		public CustomSSLSocketFactory () throws KeyManagementException, 
												NoSuchAlgorithmException, 
												KeyStoreException, 
												UnrecoverableKeyException
		{
			super(null);
			try
	        {
				SSLContext context = SSLContext.getInstance("TLS");
				TrustManager[] tm = new TrustManager[] { new FullX509TrustManager () };
				context.init(null, tm, new SecureRandom());
				FACTORY = context.getSocketFactory();
	        }
			catch (Exception e)
	        {
				e.printStackTrace();
	        }
		}

		public Socket createSocket() throws IOException
		{
			return FACTORY.createSocket();
		}
		
		/*
		public String[] getDefaultCipherSuites()
		{
			return FACTORY.getDefaultCipherSuites();
		}
		*/

		// TODO: add other methods like createSocket() and getDefaultCipherSuites().
		// Hint: they all just make a call to member FACTORY 
	}
	
	// -----------------------------
	// -----------------------------
	
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
	
	
	public static DefaultHttpClient makeHTTPClient()
	{
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "utf-8");
		params.setBooleanParameter("http.protocol.expect-continue", false);

		//registers schemes for both http and https
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		
		/*
		final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
		sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		registry.register(new Scheme("https", sslSocketFactory, 443));
		*/
		
		try {
			registry.register(new Scheme("https", new CustomSSLSocketFactory(), 443));
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
		return new DefaultHttpClient(manager, params);
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
		
		List<NameValuePair> keyval = new ArrayList<NameValuePair>(2);
		keyval.add(new BasicNameValuePair("number", number));
		keyval.add(new BasicNameValuePair("user", username));
		keyval.add(new BasicNameValuePair("password", password));
		
		String uri_str = prefix() + server_name + "/denounce?" + URLEncodedUtils.format(keyval, "utf-8");
		Log.v(TAG, "uri: " + uri_str);
		HttpGet request = new HttpGet(uri_str);
		
		try {
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
