package com.uestc.thresholddkg.Server.DkgCommunicate;

import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCastMsg;
import com.uestc.thresholddkg.Server.pojo.Complain;
import com.uestc.thresholddkg.Server.util.Compl2Obj;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import net.sf.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-01-11 14:51
 */
@Slf4j
public class SendDKGComplain implements Runnable{
    private IdpServer idpServer;
    private final String[] ipPorts;
    private final String userId;
    public SendDKGComplain(IdpServer idpServer1,String userId){
        idpServer=idpServer1;ipPorts=IdpServer.addrS;this.userId=userId;
    }

    @Override
    public void run() {
        ExecutorService service = idpServer.getService();//Executors.newFixedThreadPool(ipPorts.length - 1);
        CountDownLatch latch=new CountDownLatch(ipPorts.length-1);
        String selfAddr=idpServer.getServer().getAddress().toString();
        ConcurrentHashMap<String,String> SendFalseMap=new ConcurrentHashMap<>();
        var convert=new Convert2Str();
        Complain complain=new Complain(idpServer.getFgRecvFalse().get(userId),userId,idpServer.getServer().getAddress().toString());
        String ComplainSet= convert.Obj2json(complain);
        if(idpServer.getFgRecvFalse().get(userId).isEmpty()) {
            log.error(userId+"serv"+idpServer.getServer().getAddress().toString()+"NO Compls");return;
        }//no invalid
        for (String s : ipPorts) {
            if (("/"+s).equals(selfAddr)) continue;
            service.submit(
                    BroadCastMsg.builder().latch(latch).message(ComplainSet).mapper("collComplain").IpAndPort(s)
                            .failsMap(SendFalseMap).build()
            );
        }
        try {
            latch.await();
            if(!SendFalseMap.isEmpty()){
                SendFalseMap.forEach((key, value) ->service.submit(
                        BroadCastMsg.builder().latch(latch).message(ComplainSet).mapper("collComplain").IpAndPort(key)
                                .failsMap(SendFalseMap).build()));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //service.shutdown();
    }
}
