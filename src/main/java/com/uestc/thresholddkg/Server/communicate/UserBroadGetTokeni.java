package com.uestc.thresholddkg.Server.communicate;

import com.uestc.thresholddkg.Server.Config.MyX509TrustManger;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangjia
 * @date 2023-02-04 18:16
 */
@Builder
@Slf4j
public class UserBroadGetTokeni implements Runnable{
    private String IpAndPort;
    private String message;
    private String mapper;

    private ConcurrentHashMap<Integer,String> resMap;
    private Integer index;
    private ConcurrentHashMap<String,String> lambdaMap;
    private CountDownLatch latch;
    @Override
    public void run() {
        String tline="";
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
            if(responseCode== HttpURLConnection.HTTP_OK){
                InputStream urlstream=httpurlconnection.getInputStream();
                BufferedReader reader=new BufferedReader(new InputStreamReader(urlstream));
                String line;tline="";
                while((line=reader.readLine())!=null){
                    tline+=line;
                }
                //String[] res=tline.split("|");
                resMap.put(index,tline);
                //lambdaMap.put(IpAndPort,res[1]);
                }
            latch.countDown();
        }catch (Exception e){
            latch.countDown();
            //fail too much
            e.printStackTrace();
        }
    }
}
