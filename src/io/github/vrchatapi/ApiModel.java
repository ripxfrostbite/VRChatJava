package io.github.vrchatapi;

import io.github.vrchatapi.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

public class ApiModel {
	
	private static final String API_URL = "https://api.vrchat.cloud/api/1/";
	private static final String USERAGENT = "VRChatJava";
	protected static String apiKey;
	private static final CookieManager cookieManager = new CookieManager();
	
	static {
		// TODO: Maybe move this to somewhere else?
		try {
			CookieHandler.setDefault(cookieManager);
			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			fetchApiKey();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	private static void fetchApiKey() throws URISyntaxException {
		if(!VRCRemoteConfig.isInitialized()) {
			VRCRemoteConfig.init();
		}
		if(VRCRemoteConfig.isInitialized()) {
			apiKey = VRCRemoteConfig.getString("clientApiKey");

			if(apiKey == null || apiKey.isEmpty()) {
				Log.ERROR("Could not fetch client api key - unknown error.");
			}
		}else {
			Log.ERROR("Could not fetch client api key - config not initialized");
		}
	}
	
	public static JSONObject sendGetRequest(String endpoint, Map<String, Object> requestParams) {
		return sendRequest(endpoint, "get", requestParams);
	}
	
	public static JSONObject sendPostRequest(String endpoint, Map<String, Object> requestParams) {
		return sendRequest(endpoint, "post", requestParams);
	}

	public static JSONObject sendPutRequest(String endpoint, Map<String, Object> requestParams) {
		return sendRequest(endpoint, "put", requestParams);
	}

    public static JSONObject sendDeleteRequest(String endpoint, Map<String, Object> requestParams) {
        return sendRequest(endpoint, "delete", requestParams);
    }

    public static JSONObject sendRequest(String endpoint, String method, Map<String,Object> requestParams){
		return sendRequest(endpoint, method, requestParams, false);
	}
	
	protected static JSONObject sendRequest(String endpoint, String method, Map<String, Object> requestParams, boolean loginRequest) {
		if(requestParams != null && requestParams.size() == 0) requestParams = null;

		JSONObject resp = null;
		String apiUrl = getApiUrl();
		String uri = apiUrl + endpoint;

		String requestText = "";
		if(requestParams != null) {
			if(method.equalsIgnoreCase("get")) {
				uri += "?" + HttpUtil.urlEncode(requestParams);
			}else {
				requestText = new JSONObject(requestParams).toString();
			}
		}
		Log.INFO("Sending " + method + " request to " + uri);
		try {
			URL url = new URL(uri);
			HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
			conn.setRequestMethod(method.toUpperCase());
			conn.setRequestProperty("user-agent", USERAGENT);

			if(!loginRequest) {
				VRCCredentials.CheckTokenTimeout();
			} else {
				cookieManager.getCookieStore().removeAll();
				conn.setRequestProperty("Authorization", VRCCredentials.getWebCredentials());
			}

			if(!requestText.isEmpty()) {
				conn.setDoOutput(true);
				conn.getOutputStream().write(requestText.getBytes());
			}
			StringBuilder result = new StringBuilder();
			BufferedReader rd;
			if(conn.getResponseCode() != 200) {
				rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
			}else {
				rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			}
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();

			Map<String, List<String>> headerFields = conn.getHeaderFields();
			Set<String> headerFieldsSet = headerFields.keySet();
			Iterator<String> headerFieldsIter = headerFieldsSet.iterator();

			while (headerFieldsIter.hasNext() && loginRequest) {
				String headerFieldKey = headerFieldsIter.next();
				if ("Set-Cookie".equalsIgnoreCase(headerFieldKey)) {
					List<String> headerFieldValue = headerFields.get(headerFieldKey);
					for (String headerValue : headerFieldValue) {

						String[] fields = headerValue.split(";");
						String cookieValue = fields[0];

						if(cookieValue.startsWith("auth")){
							Log.INFO("Found AUTH cookie, storing key.");
							VRCCredentials.setAuthToken(cookieValue);
						}
					}

				}
			}
			Log.INFO(result.toString());
			if(!result.toString().startsWith("error code:")) {
				if (conn.getResponseCode() != 200) {
					String error = new JSONObject(result.toString()).optString("error");
					if (error == null || error.isEmpty()) {
						JSONObject errObj = new JSONObject(result.toString()).optJSONObject("error");
						if (errObj != null) {
							error = errObj.optString("message", "unknown");
						} else {
							error = "unknown";
						}
					}
					Log.ERROR("Error sending request - " + error);
					throw new VRCException(error);
				}
				resp = new JSONObject(result.toString());
			} else {
				//CloudFlare Error trigger reset of cf cookie?
				cookieManager.getCookieStore().removeAll();
				VRCCredentials.clear();

				String err = "CloudFlare Error Code: " + result.toString().replaceFirst("error code:", "").trim();
				Log.ERROR(err);
				throw new VRCException(err);
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
		return resp;
	}

	public static String sendGetRequestRaw(String endpoint, Map<String, Object> requestParams) {
		return sendRequestRaw(endpoint, "get", requestParams);
	}

	protected static String sendRequestRaw(String endpoint, String method, Map<String, Object> requestParams) {
		if (requestParams != null && requestParams.size() == 0) requestParams = null;

		String resp = null;
		String apiUrl = getApiUrl();
		String uri = apiUrl + endpoint;

		String requestText = "";
		if (requestParams != null) {
			if (method.equalsIgnoreCase("get")) {
				uri += "?" + HttpUtil.urlEncode(requestParams);
			} else {
				requestText = new JSONObject(requestParams).toString();
			}
		}
		Log.INFO("Sending " + method + " request to " + uri);
		try {
			URL url = new URL(uri);
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod(method.toUpperCase());
			conn.setRequestProperty("user-agent",USERAGENT);

			VRCCredentials.CheckTokenTimeout();

			if (!requestText.isEmpty()) {
				conn.setDoOutput(true);
				conn.getOutputStream().write(requestText.getBytes());
			}
			StringBuilder result = new StringBuilder();
			BufferedReader rd;
			if (conn.getResponseCode() != 200) {
				rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
			} else {
				rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			}
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();

			Log.INFO(result.toString());

			if(!result.toString().startsWith("error code:")) {
				if (conn.getResponseCode() != 200) {
					String error = new JSONObject(result.toString()).optString("error");
					if (error == null || error.isEmpty()) {
						JSONObject errObj = new JSONObject(result.toString()).optJSONObject("error");
						if (errObj != null) {
							error = errObj.optString("message", "unknown");
						} else {
							error = "unknown";
						}
					}
					Log.ERROR("Error sending request - " + error);
					throw new VRCException(error);
				}
				resp = result.toString();
			} else {
				//CloudFlare Error trigger reset of cf cookie?
				cookieManager.getCookieStore().removeAll();
				VRCCredentials.clear();

				String err = "CloudFlare Error Code: " + result.toString().replaceFirst("error code:", "").trim();
				Log.ERROR(err);
				throw new VRCException(err);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resp;
	}
	

	public static JSONArray sendGetRequestArray(String endpoint, Map<String, Object> requestParams) {
		return sendRequestArray(endpoint, "get", requestParams);
	}
	
	public static JSONArray sendPostRequestArray(String endpoint, Map<String, Object> requestParams) {
		return sendRequestArray(endpoint, "post", requestParams);
	}

	public static JSONArray sendPutRequestArray(String endpoint, Map<String, Object> requestParams) {
		return sendRequestArray(endpoint, "put", requestParams);
	}
	
	protected static JSONArray sendRequestArray(String endpoint, String method, Map<String, Object> requestParams) {
		if(requestParams != null && requestParams.size() == 0) requestParams = null;
		
		JSONArray resp = null;
		String apiUrl = getApiUrl();
		String uri = apiUrl + endpoint;

		String requestText = "";
		if(requestParams != null) {
			if(method.equalsIgnoreCase("get")) {
				uri += (uri.contains("?") ? "&" : "?") + HttpUtil.urlEncode(requestParams);
			}else {
				requestText = new JSONObject(requestParams).toString();
			}
		}
		Log.INFO("Sending " + method + " request to " + uri);
		try {
			URL url = new URL(uri);
			HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
			conn.setRequestMethod(method.toUpperCase());
			conn.setRequestProperty("user-agent", USERAGENT);

			VRCCredentials.CheckTokenTimeout();

			if(!requestText.isEmpty()) {
				conn.setDoOutput(true);
				conn.getOutputStream().write(requestText.getBytes());
			}
			StringBuilder result = new StringBuilder();
			BufferedReader rd;
			if(conn.getResponseCode() != 200) {
				rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
			}else {
				rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			}
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();
			if(!result.toString().startsWith("error code:")) {
				if (conn.getResponseCode() != 200) {
					String error = new JSONObject(result.toString()).optString("error");
					if (error == null || error.isEmpty()) {
						JSONObject errObj = new JSONObject(result.toString()).optJSONObject("error");
						if (errObj != null) {
							error = errObj.optString("message", "unknown");
						} else {
							error = "unknown";
						}
					}
					Log.ERROR("Error sending request - " + error);
					throw new VRCException(error);
				}
				resp = new JSONArray(result.toString());
			} else {
				//CloudFlare Error trigger reset of cf cookie?
				cookieManager.getCookieStore().removeAll();
				VRCCredentials.clear();

				String err = "CloudFlare Error Code: " + result.toString().replaceFirst("error code:", "").trim();
				Log.ERROR(err);
				throw new VRCException(err);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resp;
	}
	
	public static String getApiUrl() {
		return API_URL;
	}
	
}
