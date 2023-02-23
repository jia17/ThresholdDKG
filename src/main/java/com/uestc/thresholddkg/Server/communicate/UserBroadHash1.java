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
 * @date 2023-01-24 17:07
 */
@Builder
@Slf4j
public class UserBroadHash1 implements Runnable{
    private String IpAndPort;
    private String message;
    private String mapper;

    private ConcurrentHashMap<String,String> resmap;
    private ConcurrentSkipListSet<String> verySet;
    private AtomicInteger failCount;
    private Integer maxFail;
    private CountDownLatch latch;
    private AtomicInteger DbCount;
    @Override
    public void run(){
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
                int len=Integer.parseInt(tline.substring(0,7));
                resmap.put(String.valueOf(IpAndPort.charAt(IpAndPort.length()-2)),tline.substring(8,len+8));//cautious serv id
                if("1".equals(String.valueOf(tline.charAt(7)))){verySet.add(tline.substring(len+8,tline.length()));}
                latch.countDown();}
            else{
                final int fail=failCount.incrementAndGet();
                if(fail==(maxFail+1)){while(latch.getCount()>0)latch.countDown();}
            }
        }catch (Exception e){
            //fail too much
            final int fail=failCount.incrementAndGet();
            if(fail==(maxFail+1)){while(latch.getCount()>0)latch.countDown();}
            e.printStackTrace();
        }
    }
}
