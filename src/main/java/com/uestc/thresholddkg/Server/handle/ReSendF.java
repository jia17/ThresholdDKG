package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.FunctionGHvals;
import com.uestc.thresholddkg.Server.pojo.ReGetGF;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.DKG;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-01-11 21:17
 */
public class ReSendF implements HttpHandler {
    private IdpServer idpServer;
    private String[] ipPorts;
    public ReSendF(IdpServer idpServer1){
        ipPorts=IdpServer.addrS;idpServer=idpServer1;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String mess="";
        String line;
        while((line=reader.readLine())!=null){
            mess+=line;
        }
        Convert2Str convert=new Convert2Str();
        ReGetGF reGetGF=(ReGetGF)convert.Json2obj(mess,ReGetGF.class);
        String userId=reGetGF.getUserId();
        byte[] respContents = "ReSendF".getBytes("UTF-8");
        String addr=reGetGF.getSendAddr().substring(1);
        String selfAddr=idpServer.getServer().getAddress().toString();
        int fIndex=0;
        for (int i=0;i<ipPorts.length;i++){
            if(addr.equals(ipPorts[i])){fIndex=i+1;break;}
        }
        BigInteger gVal=idpServer.getGValue().get(userId)[fIndex-1];
        //if(selfAddr.equals("/127.0.0.10:9050")&&addr.equals("127.0.0.10:9070"))gVal=gVal.subtract(BigInteger.ONE);// cautious testServer Fail,broadFunc
        //if(addr.equals("127.0.0.10:9060"))idpServer.getGValue().get(userId)[fIndex-1]=idpServer.getGValue().get(userId)[fIndex-1].subtract(BigInteger.ONE);
        FunctionGHvals message=FunctionGHvals.builder().gMulsH(DKG.bigInt2Str(idpServer.getMulsGH().get(userId)))
                .sendAddr(selfAddr).userId(userId).item(idpServer.item).build();
        message.setFi(idpServer.getFValue().get(userId)[fIndex-1].toString());
        message.setGi(gVal.toString());
        message.setServerId(fIndex);
        SendUri send = SendUri.builder().message(convert.Obj2json(message)).mapper("verifyGH").IpAndPort(addr).build();
        send.SendMsg();
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
