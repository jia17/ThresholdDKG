package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.persist.ServPrfs;
import com.uestc.thresholddkg.Server.persist.ServPrfsPp;
import com.uestc.thresholddkg.Server.persist.ServTokenKey;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsMapper;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsPPMapper;
import com.uestc.thresholddkg.Server.persist.mapper.ServTokenKMapper;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServPrfsPpWR;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServPrfsWR;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServTokenKWR;
import com.uestc.thresholddkg.Server.pojo.DKG_SysStr;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.TokenSi;
import com.uestc.thresholddkg.Server.pojo.UserMsg2Serv;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import com.uestc.thresholddkg.Server.util.DKG;
import lombok.AllArgsConstructor;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

/**
 * @author zhangjia
 * @date 2023-02-28 15:02
 */
@AllArgsConstructor
public class TestTokenApply implements HttpHandler {
    private IdpServer idpServer;
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String[] ipPorts= IdpServer.addrS;
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        String res="";
        UserMsg2Serv userMsg=(UserMsg2Serv) Convert2StrToken.Json2obj(tline, UserMsg2Serv.class);
        ServTokenKMapper tokenKMapper= ServTokenKWR.getMapper();
        ServTokenKey tokenKey=tokenKMapper.selectById(idpServer.getServerId());
        if(tokenKey==null) {
            byte[] respContents = "<h4>please register first</h4> ".getBytes("UTF-8");
            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(500, respContents.length);
            httpExchange.getResponseBody().write(respContents);
            httpExchange.close();return;
        }
        ServPrfsPPMapper servPrfsPPMapper= ServPrfsPpWR.getMapper();
        ServPrfsPp servPrfsPp=servPrfsPPMapper.selectById("userId");
        DKG_System dkg_system=new DKG_System(new BigInteger(servPrfsPp.getP()),new BigInteger(servPrfsPp.getQ()),
                new BigInteger(servPrfsPp.getG()),new BigInteger(servPrfsPp.getH()));
        int index=0;String userId=userMsg.getUserId();int threshold=IdpServer.threshold;
        idpServer.getUserMsg().put(userId,userMsg.getMsg());
        idpServer.getUserMsgHash().put(userId,userMsg.getMsgHash());
        for(;index<ipPorts.length;index++){
            if(idpServer.getServer().getAddress().toString().equals("/"+ipPorts[index])){index++;break;}
        }
        boolean isToken=false;
        for(int i=0;i<userMsg.getAddrIndex().length;i++){
            if(index==userMsg.getAddrIndex()[i]){isToken=true;break;}
        }
        res=tokenKey.getPriKeyi().toString()+"@";
        if(isToken) {
            BigInteger secretI = new BigInteger(tokenKey.getPriKeyi());
            BigInteger q = dkg_system.getQ();
            Integer listMuls = 1;
            Integer[] addrIndex = userMsg.getAddrIndex();
            for (Integer val : userMsg.getAddrIndex()) {
                listMuls *= val;
            }
            BigInteger Xi;
            Integer tempMul = 0;
            tempMul = index;
            if (((threshold) & 1) == 0) tempMul *= -1;
            for (int j = 0; j < addrIndex.length; j++) {
                if (index != addrIndex[j]) {
                    tempMul *= (index - addrIndex[j]);
                }
            }
            Xi = secretI.multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)), q)).mod(q);
            BigInteger Hi = new BigInteger(userMsg.getMsgHash()).multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)), q)).mod(q);
            if (tempMul < 0) Xi = q.add(Xi.negate()).mod(q);
            Xi = Xi.multiply(BigInteger.valueOf(listMuls)).mod(q);
            if (tempMul < 0) Hi = q.add(Hi.negate()).mod(q);
            Hi = Hi.multiply(BigInteger.valueOf(listMuls)).mod(q);
            BigInteger msgBnt = new BigInteger(userMsg.getMsg()).mod(q), msg2 = new BigInteger(userMsg.getMsgHash()).multiply(Xi).mod(q);
            BigInteger Vi = (msgBnt.add(q.add(msg2.negate())).mod(q)).multiply(Calculate.modInverse(BigInteger.valueOf(2), q)).mod(q);
            // BigInteger Vi=msg2.multiply(Calculate.modInverse(BigInteger.valueOf(2),q)).mod(q);

            BigInteger Wi = dkg_system.getG().modPow(Vi, dkg_system.getP());
            res+=Wi.toString();
        }else{res+="1";}
        byte[] respContents = res.getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
