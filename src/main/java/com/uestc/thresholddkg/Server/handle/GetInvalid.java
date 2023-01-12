package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.FunctionGHvals;
import com.uestc.thresholddkg.Server.util.DKG;
import com.uestc.thresholddkg.Server.util.FuncGH2Obj;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author zhangjia
 * @date 2023-01-11 22:08
 */
@Slf4j
public class GetInvalid implements HttpHandler {
    private IdpServer idpServer;
    private Map<String, ConcurrentSkipListSet<String>> recvInvalid;

    public GetInvalid(IdpServer idpServer1,Map<String, ConcurrentSkipListSet<String>> map){idpServer=idpServer1;recvInvalid=map;}

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String InvalidMap="";
        String line;
        while((line=reader.readLine())!=null){
            InvalidMap+=line;
        }
        JSONObject jsonobject = JSONObject.fromObject(InvalidMap);
        Map<String,String> mapInvalid=(Map<String,String>) JSONObject.toBean(jsonobject,Map.class);
        mapInvalid.forEach((k,v)->{
            String[] Addrs=v.split("@");;//invalid addr+@+send addr;
            idpServer.getFgRecv().get(k).remove(Addrs[0]);
        log.error(httpExchange.getLocalAddress()+"Invalid map Remove"+v+" user:"+k+" size "+idpServer.getFgRecv().get(k).size());
        //received invalid for (user,addr),,if had a invalid,dont send Invalid
        if(!recvInvalid.containsKey(k)){recvInvalid.put(k,new ConcurrentSkipListSet<>());}
        recvInvalid.get(k).add(Addrs[1]);
        });
        byte[] respContents = "ReSendF".getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
