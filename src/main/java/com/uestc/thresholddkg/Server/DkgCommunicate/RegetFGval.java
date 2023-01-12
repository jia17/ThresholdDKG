package com.uestc.thresholddkg.Server.DkgCommunicate;

import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCastMsg;
import com.uestc.thresholddkg.Server.pojo.Complain;
import com.uestc.thresholddkg.Server.pojo.ReGetGF;
import com.uestc.thresholddkg.Server.util.Compl2Obj;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-01-11 18:20
 */
@Slf4j
public class RegetFGval implements Runnable{
    private IdpServer idpServer;
    private String userId;
    private String[] ipPorts;
    public RegetFGval(IdpServer idpServer1,String userId1){idpServer=idpServer1;userId=userId1;ipPorts=IdpServer.addrS;}

    @Override
    public void run() {
        int regetNum=idpServer.getFgRecvFalse().get(userId).size();
        if(regetNum==0) {
            log.error(userId+"serv"+idpServer.getServer().getAddress().toString()+"NO ReSend");;return;
        }
        ExecutorService service = Executors.newFixedThreadPool(regetNum);
        CountDownLatch latch=new CountDownLatch(regetNum);
        ConcurrentHashMap<String,String> SendFalseMap=new ConcurrentHashMap<>();
        ReGetGF reGetGF=new ReGetGF(userId,idpServer.getServer().getAddress().toString());
        var convert=new Convert2Str();
        String message=convert.Obj2json(reGetGF);
        for (String addr : idpServer.getFgRecvFalse().get(userId)) {
            service.submit(
                    BroadCastMsg.builder().latch(latch).message(message).mapper("pushFval2").IpAndPort(addr.substring(1))
                            .failsMap(SendFalseMap).build()
            );
        }
        try {
            latch.await();
            if(!SendFalseMap.isEmpty()){
                SendFalseMap.forEach((key, value) ->service.submit(
                        BroadCastMsg.builder().latch(latch).message(message).mapper("pushFval2").IpAndPort(key)
                                .failsMap(SendFalseMap).build()));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        service.shutdown();
    }
}
