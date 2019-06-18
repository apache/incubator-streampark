package com.streamxhub.spark.monitor.common.utils;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

@Slf4j
public class HttpUtil {

    private static final String USER_AGENT = "user-agent";
    private static final String USER_AGENT_VALUE = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)";
    private static final String CONNECTION = "connection";
    private static final String CONNECTION_VALUE = "Keep-Alive";
    private static final String ACCEPT = "accept";
    private static final String UTF8 = "utf-8";
    private static final String ACCEPT_CHARSET = "Accept-Charset";
    private static final String CONTENTTYPE = "contentType";
    private static final String SSL = "ssl";

    protected HttpUtil() {

    }

    /**
     * 向指定 URL 发送GET方法的请求
     *
     * @param url   发送请求的 URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) throws IOException {
        String urlNameString = url;
        if (StringUtils.isNotBlank(param))
            urlNameString += "?" + param;
        URL realUrl = new URL(urlNameString);
        URLConnection connection = realUrl.openConnection();
        StringBuilder result = new StringBuilder();
        connection.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
        connection.setRequestProperty(CONNECTION, CONNECTION_VALUE);
        connection.setRequestProperty(ACCEPT, "*/*");
        connection.connect();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            log.error("发送GET请求出现异常！", e);
        }
        return result.toString();
    }

    /**
     * 向指定 URL 发送GET方法的请求
     *
     * @param url 发送请求的 URL
     * @return 所代表远程资源的响应结果
     */
    public static String sendGet(String url) throws IOException {
        return sendGet(url, null);
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url   发送请求的 URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) throws IOException {
        StringBuilder result = new StringBuilder();

        String urlNameString = url + "?" + param;
        URL realUrl = new URL(urlNameString);
        URLConnection conn = realUrl.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty(CONTENTTYPE, UTF8);
        conn.setRequestProperty(ACCEPT_CHARSET, UTF8);
        conn.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
        conn.setRequestProperty(CONNECTION, CONNECTION_VALUE);
        conn.setRequestProperty(ACCEPT, "*/*");
        try (PrintWriter out = new PrintWriter(conn.getOutputStream()); BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            out.flush();
            out.print(param);
        } catch (Exception e) {
            log.error("发送 POST 请求出现异常！", e);
        }
        return result.toString();
    }

    public static String sendSSLPost(String url, String param) {
        StringBuilder result = new StringBuilder();
        String urlNameString = url + "?" + param;
        try {
            SSLContext sc = SSLContext.getInstance(SSL);
            sc.init(null, new TrustManager[]{new TrustAnyTrustManager()}, new java.security.SecureRandom());
            URL console = new URL(urlNameString);
            HttpsURLConnection conn = (HttpsURLConnection) console.openConnection();
            conn.setRequestProperty(ACCEPT, "*/*");
            conn.setRequestProperty(CONNECTION, CONNECTION_VALUE);
            conn.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
            conn.setRequestProperty(ACCEPT_CHARSET, UTF8);
            conn.setRequestProperty(CONTENTTYPE, UTF8);
            conn.setDoOutput(true);
            conn.setDoInput(true);

            conn.setSSLSocketFactory(sc.getSocketFactory());
            conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedReader indata = new BufferedReader(new InputStreamReader(is));
            String ret = "";
            while (ret != null) {
                ret = indata.readLine();
                if (ret != null && !ret.trim().equals("")) {
                    result.append(ret);
                }
            }
            conn.disconnect();
            indata.close();
        } catch (Exception e) {
            log.error("发送SSL POST 请求出现异常！", e);
        }
        return result.toString();
    }

    private static class TrustAnyTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            //trust anything
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            //trust anything
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }

    private static class TrustAnyHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

}
