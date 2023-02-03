package com.uestc.thresholddkg.Server.DkgCommunicate;

import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.FunctionFExp;
import com.uestc.thresholddkg.Server.pojo.FunctionGHvals;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import com.uestc.thresholddkg.Server.util.DKG;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import javax.persistence.Id;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-02-03 20:26
 */
@Slf4j
public class PubKeyBroad implements Runnable{
    private IdpServer idpServer;
    private String userId;
    private final String[] ipPorts;
    public PubKeyBroad(IdpServer idpServer1, String userId1){
        idpServer=idpServer1;userId=userId1;ipPorts= IdpServer.addrS;
    }
    @Override
    public void run() {
        String selfAddr=idpServer.getServer().getAddress().toString();
        if(!idpServer.getFExpRecv().containsKey(userId)){idpServer.getFExpRecv().put(userId,new HashMap<>());
        idpServer.getFExpFalse().put(userId,new HashSet<>());}
        if(!idpServer.getFgRecv().get(userId).containsKey(selfAddr))return;
        System.out.println(selfAddr+"Token FEXXP");
        var DkgPara=idpServer.getDkgParam().get(userId);
        BigInteger g=DkgPara.getG();
        BigInteger p=DkgPara.getP();
        BigInteger[]F=idpServer.getFParam().get(userId);
        BigInteger[]FExp=new BigInteger[F.length];
        for(int i=0;i<FExp.length;i++){
            FExp[i]=g.modPow(F[i],p);
        }
        idpServer.getFExpRecv().get(userId).put(selfAddr,FExp[0]);
        ExecutorService service= Executors.newFixedThreadPool(ipPorts.length-1);
        for (int i=0;i<ipPorts.length;i++) {
            var message= FunctionFExp.builder().fExp(DKG.bigInt2Str(FExp)).userId(userId).sendAddr(selfAddr).serverId(i+1).build();
            String s=ipPorts[i];
            if ((s).equals(selfAddr)) {
              continue;
            }
            SendUri send = SendUri.builder().message(Convert2StrToken.Obj2json(message)).mapper("verifyFExp").IpAndPort(s).build();
            service.submit(send::SendMsg);
        }
        service.shutdown();
    }
}
