package io.github.vrchatapi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class HttpUtil {
	
	public static String urlEncode(String s) {
		try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
	}
	
	public static String urlEncode(Map<?,?> map) {
		StringBuilder sb = new StringBuilder();
        for (Map.Entry<?,?> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s",
                urlEncode(entry.getKey().toString()),
                urlEncode(entry.getValue().toString())
            ));
        }
        return sb.toString();  
	}

}
