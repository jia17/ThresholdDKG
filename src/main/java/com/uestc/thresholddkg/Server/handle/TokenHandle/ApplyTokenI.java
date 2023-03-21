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
import com.uestc.thresholddkg.Server.pojo.*;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import com.uestc.thresholddkg.Server.util.DKG;
import lombok.AllArgsConstructor;
import lombok.var;

import javax.persistence.Id;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

/**
 * @author zhangjia
 * @date 2023-02-04 18:24
 */
@AllArgsConstructor
public class ApplyTokenI implements HttpHandler {
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
        UserMsg2Serv userMsg=(UserMsg2Serv)Convert2StrToken.Json2obj(tline, UserMsg2Serv.class);
        ServTokenKMapper tokenKMapper= ServTokenKWR.getMapper();
        ServTokenKey tokenKey=tokenKMapper.selectById(idpServer.getServerId());
        if(tokenKey==null) {
            byte[] respContents = "str".getBytes("UTF-8");
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
        idpServer.getMsgTime().put(userMsg.getUserId(),userMsg.getMsgTime());
        for(;index<ipPorts.length;index++){
            if(idpServer.getServer().getAddress().toString().equals("/"+ipPorts[index])){index++;break;}
        }
        BigInteger secretI=new BigInteger(tokenKey.getPriKeyi());
        BigInteger q=dkg_system.getQ();
        Integer listMuls=1;Integer[] addrIndex= userMsg.getAddrIndex();
        for (Integer val : userMsg.getAddrIndex()) { listMuls*=val;  }
        BigInteger Xi;
        Integer tempMul=0;
        tempMul=index;
        if(((threshold)&1)==0)tempMul*=-1;
        for(int j=0;j<addrIndex.length;j++){
            if(index!=addrIndex[j]){tempMul*=(index-addrIndex[j]);}
        }
        Xi=secretI.multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)),q)).mod(q);
        BigInteger Hi=new BigInteger(userMsg.getMsgHash()).multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)),q)).mod(q);
        if(tempMul<0)Xi=q.add(Xi.negate()).mod(q);
        Xi=Xi.multiply(BigInteger.valueOf(listMuls)).mod(q);
        if(tempMul<0)Hi=q.add(Hi.negate()).mod(q);
        Hi=Hi.multiply(BigInteger.valueOf(listMuls)).mod(q);
        BigInteger msgBnt=new BigInteger(userMsg.getMsg()).mod(q),msg2=new BigInteger(userMsg.getMsgHash()).multiply(Xi).mod(q);
        BigInteger Vi=(msgBnt.add(q.add(msg2.negate())).mod(q)).multiply(Calculate.modInverse(BigInteger.valueOf(2),q)).mod(q);
       // BigInteger Vi=msg2.multiply(Calculate.modInverse(BigInteger.valueOf(2),q)).mod(q);

        BigInteger Wi=dkg_system.getG().modPow(Vi,dkg_system.getP());
        BigInteger Ui=dkg_system.getG().modPow(secretI,dkg_system.getP());
        //conn database
        ServPrfsMapper prfsMapper= ServPrfsWR.getMapper();
        ServPrfs prfI=prfsMapper.selectByIdAndF(index,userId);
        String EWi="";
        try {
             EWi=DKG.AESencrypt(Wi.toString(),prfI.getPrfi());
        } catch (NoSuchAlgorithmException e) {
            System.out.println(index+"error AESEnc");
            throw new RuntimeException(e);
        }

        DKG_SysStr dkg_sysStr=userMsg.getDkg_sysStrP();
        DKG_System dkg_systemP=new DKG_System(new BigInteger(dkg_sysStr.getP()),new BigInteger(dkg_sysStr.getQ()),
                new BigInteger(dkg_sysStr.getG()),new BigInteger(dkg_sysStr.getH()));
        BigInteger Hash1Pwd=new BigInteger(userMsg.getPwdHash1());
        String CVery=prfI.getVerify();
        BigInteger secretIP=new BigInteger(prfI.getPriKeyi());
        BigInteger EncHash1Pwd=Hash1Pwd.modPow(secretIP,dkg_systemP.getP());
        TokenSi tokenSi=TokenSi.builder().Ui(Ui.toString()).Wi(EWi).EncPwdHash1(EncHash1Pwd.toString()).Cvery(CVery).build();
        String str= Convert2StrToken.Obj2json(tokenSi);

        byte[] respContents = str.getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
