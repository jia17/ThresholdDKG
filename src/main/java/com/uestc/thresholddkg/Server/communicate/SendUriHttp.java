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

/**
 * @author zhangjia
 * @date 2023-03-26 15:28
 */
@Builder
@Slf4j
public class SendUriHttp {
    private String IpAndPort;
    private String message;
    private String mapper;
    public String SendMsg(){
        boolean success=false;
        String tline="";
        String http="http://"+IpAndPort+"/"+mapper;
        try {
            URL url = new URL(http);
            HttpURLConnection httpurlconnection = (HttpURLConnection) url.openConnection();
            httpurlconnection.setDoOutput(true);
            httpurlconnection.setRequestMethod("POST");
            httpurlconnection.setConnectTimeout(30000);
            httpurlconnection.setRequestProperty("Connection","Keep-Alive");
            httpurlconnection.setRequestProperty("MaxConnections","7");
            //设置输出流。
            OutputStreamWriter writer = new OutputStreamWriter(httpurlconnection.getOutputStream(), "utf-8");
            //传递的参数，中间使用&符号分割。
            writer.write(message);
            writer.flush();
            writer.close();
            int responseCode = httpurlconnection.getResponseCode();
            //表示请求成功
            if(responseCode== HttpURLConnection.HTTP_OK){
                InputStream urlstream=httpurlconnection.getInputStream();
                BufferedReader reader=new BufferedReader(new InputStreamReader(urlstream));
                String line;
                while((line=reader.readLine())!=null){
                    tline+=line;
                }
                //没有返回的数据
                success=true; }else{
                log.error("SendUri RespCode error "+http);
            }
        }catch (Exception e){
            log.error(" Connect to FAIL" +http);
            e.printStackTrace();
        }
        return tline;
    }
}
