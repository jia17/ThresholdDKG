package com.uestc.thresholddkg.Server.communicate;

import com.uestc.thresholddkg.Server.Config.MyX509TrustManger;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author zhangjia
 * @date 2023-01-10 20:49
 * broadcast message without reply,hope n success,latch=n
 */
@Builder
@Slf4j
public class BroadCastMsg implements Runnable{

    private String IpAndPort;
    private String message;
    private String mapper;

    private ConcurrentHashMap<String,String> failsMap;
    private CountDownLatch latch;
    @Override
    public void run() {
        String http="https://"+IpAndPort+"/"+mapper;
        try {
            TrustManager[] managers={new MyX509TrustManger()};
            SSLContext sslContext=SSLContext.getInstance("SSL");
            sslContext.init(null,managers,new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory=sslContext.getSocketFactory();
            URL url = new URL(http);
            HttpsURLConnection httpurlconnection = (HttpsURLConnection) url.openConnection();
            httpurlconnection.setDoOutput(true);
            httpurlconnection.setRequestMethod("POST");
            httpurlconnection.setConnectTimeout(30000);
            httpurlconnection.setSSLSocketFactory(sslSocketFactory);
            httpurlconnection.setRequestProperty("Connection","Keep-Alive");
            httpurlconnection.setRequestProperty("MaxConnections","7");
            httpurlconnection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            OutputStreamWriter writer = new OutputStreamWriter(httpurlconnection.getOutputStream(), "utf-8");
            writer.write(message);
            writer.flush();
            writer.close();
            int responseCode = httpurlconnection.getResponseCode();
            //表示请求成功
            if(responseCode== HttpURLConnection.HTTP_OK){
                latch.countDown(); }
        }catch (Exception e){
                latch.countDown();
                failsMap.put(IpAndPort,"FAIL BroadCast");
                e.printStackTrace();
        }
        return ;
    }
}
