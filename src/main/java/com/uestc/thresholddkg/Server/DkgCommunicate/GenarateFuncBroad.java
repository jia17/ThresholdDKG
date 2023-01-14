package com.uestc.thresholddkg.Server.DkgCommunicate;

import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import com.uestc.thresholddkg.Server.pojo.FunctionGHvals;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.DKG;
import com.uestc.thresholddkg.Server.util.FuncGH2Obj;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.math.BigInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-01-10 14:52
 */
@Slf4j
public class GenarateFuncBroad implements Runnable{
    private IdpServer idpServer;
    private String userId;
    private final String[] ipPorts;
    private String selfAddr;
    public GenarateFuncBroad(IdpServer idpServer1,String userId1,String[] ipPorts1,String selfAddr1){
        idpServer=idpServer1;userId=userId1;ipPorts=ipPorts1;selfAddr=selfAddr1;
    }
    @Override
    public void run() {
        log.error(selfAddr+"start BroadDKG");
        BigInteger g=idpServer.getDkgParam().get(userId).getG();
        BigInteger h=idpServer.getDkgParam().get(userId).getH();
        BigInteger p=idpServer.getDkgParam().get(userId).getP();
        BigInteger q=idpServer.getDkgParam().get(userId).getQ();
        BigInteger[]F= DKG.generateFuncParam(idpServer.getSecretAndT().get(userId)[0],p);
        BigInteger[]G=DKG.generateFuncParam(idpServer.getSecretAndT().get(userId)[1],p);
        idpServer.getFParam().put(userId,F);
        BigInteger[] fVal=DKG.FunctionValue(F,q);
        BigInteger[] gVal=DKG.FunctionValue(G,q);
        BigInteger[] gMulH=DKG.FunctionGH(g,h,F,G,p);
        //idpServer.getFParam().put(userId,F);
        idpServer.getFValue().put(userId,fVal);
        idpServer.getGValue().put(userId,gVal);
        idpServer.getMulsGH().put(userId,gMulH);
        ExecutorService service= Executors.newFixedThreadPool(ipPorts.length-1);
        for (int i=0;i<ipPorts.length;i++) {
            FunctionGHvals message=FunctionGHvals.builder().gMulsH(DKG.bigInt2Str(gMulH)).sendAddr(selfAddr).userId(userId).item(idpServer.item).build();
            String s=ipPorts[i];
            //if(selfAddr.equals("/127.0.0.10:9050"))gVal[i]=gVal[i].add(BigInteger.ONE);//cautious test server Fail
            message.setFi(fVal[i].toString());message.setGi(gVal[i].toString());message.setServerId(i+1);
            if (("/" + s).equals(selfAddr)) {
                idpServer.getFgRecv().get(userId).put(selfAddr,fVal[i]);continue;//add f[self] to FRMap;self=/192.16.//cautious changed continue
            }
            var convert=new Convert2Str();
            SendUri send = SendUri.builder().message(convert.Obj2json(message)).mapper("verifyGH").IpAndPort(s).build();
            //send.SendMsg();
            service.submit(send::SendMsg);
        }
        service.shutdown();
    }
}
