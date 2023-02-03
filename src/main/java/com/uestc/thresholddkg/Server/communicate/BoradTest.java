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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangjia
 * @date 2023-01-03 13:56
 */
@Builder
@Slf4j
public class BoradTest implements Runnable{
    private String IpAndPort;
    private String message;
    private String mapper;

    private ConcurrentHashMap<String,String> resmap;
    private ConcurrentHashMap<String,String> resPubMap;
    private AtomicInteger failCount;
    private Integer maxFail;
    private CountDownLatch latch;
    @Override
    public void run(){
        boolean success=false;
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
            httpurlconnection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            //设置输出流。
            OutputStreamWriter writer = new OutputStreamWriter(httpurlconnection.getOutputStream(), "utf-8");
            //传递的参数，中间使用&符号分割。
            writer.write(message);
            writer.flush();
            writer.close();
            int responseCode = httpurlconnection.getResponseCode();
            //表示请求成功
            if(responseCode==HttpURLConnection.HTTP_OK){
                //log.warn("send:"+http);
                InputStream urlstream=httpurlconnection.getInputStream();
                BufferedReader reader=new BufferedReader(new InputStreamReader(urlstream));
                String line;tline="";
                while((line=reader.readLine())!=null){
                    tline+=line;
                }
                //success
                latch.countDown();
                String[] PriPub=tline.split("@");
                resmap.put(IpAndPort,PriPub[0]);
                resPubMap.put(IpAndPort,PriPub[1]);
                success=true; }
        }catch (Exception e){
            //fail too much
            final int fail=failCount.incrementAndGet();
            if(fail==(maxFail+1)){while(latch.getCount()>0)latch.countDown();}
            e.printStackTrace();
        }
        return ;
    }
}
