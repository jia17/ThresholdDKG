package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.FunctionFExp;
import com.uestc.thresholddkg.Server.pojo.FunctionGHvals;
import com.uestc.thresholddkg.Server.pojo.ReGetGF;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import com.uestc.thresholddkg.Server.util.DKG;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-02-03 21:30
 */
public class ReSendFExp implements HttpHandler {
    private IdpServer idpServer;
    private String[] ipPorts;
    public ReSendFExp(IdpServer idpServer1){
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
        ReGetGF reGetGF=(ReGetGF)Convert2Str.Json2obj(mess,ReGetGF.class);
        String userId=reGetGF.getUserId();
        byte[] respContents = "ReSendFexp".getBytes("UTF-8");
        String addr=reGetGF.getSendAddr().substring(1);
        String selfAddr=idpServer.getServer().getAddress().toString();
        int fIndex=0;
        for (int i=0;i<ipPorts.length;i++){
            if(addr.equals(ipPorts[i])){fIndex=i+1;break;}
        }
        // if(selfAddr.equals("/127.0.0.10:9050")&&i==6)gVal[i]=gVal[i].add(BigInteger.ONE); cautious testServer Fail,broadFunc
        //if(addr.equals("127.0.0.10:9060"))idpServer.getGValue().get(userId)[fIndex-1]=idpServer.getGValue().get(userId)[fIndex-1].subtract(BigInteger.ONE);
        var DkgPara=idpServer.getDkgParam().get(userId);
        BigInteger g=DkgPara.getG();
        BigInteger p=DkgPara.getP();
        BigInteger[]F=idpServer.getFParam().get(userId);
        BigInteger[]FExp=new BigInteger[F.length];
        for(int i=0;i<FExp.length;i++){
            FExp[i]=g.modPow(F[i],p);
        }
        var message= FunctionFExp.builder().fExp(DKG.bigInt2Str(FExp)).userId(userId).sendAddr(selfAddr).serverId(fIndex).build();
        SendUri send = SendUri.builder().message(Convert2StrToken.Obj2json(message)).mapper("verifyFExp").IpAndPort(addr).build();
        send.SendMsg();
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
