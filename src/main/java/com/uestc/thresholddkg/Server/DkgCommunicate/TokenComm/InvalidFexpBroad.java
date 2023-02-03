package com.uestc.thresholddkg.Server.DkgCommunicate.TokenComm;

import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCastMsg;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import net.sf.json.JSONObject;

import javax.persistence.Id;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author zhangjia
 * @date 2023-02-03 23:06
 */
@AllArgsConstructor
@Slf4j
public class InvalidFexpBroad implements Runnable{
    private IdpServer idpServer;
    private String userId;
    private String remoteAddr;

    @Override
    public void run() {
        String[] ipAndPort=IdpServer.addrS;
        int serversNum= ipAndPort.length;
        ExecutorService service = Executors.newFixedThreadPool(serversNum - 1);
        CountDownLatch latch=new CountDownLatch(0);
        Map<String,String> map=new HashMap<>();
        ConcurrentHashMap<String,String> resMap=new ConcurrentHashMap<>();
        String Addrs=remoteAddr+"@"+idpServer.getServer().getAddress().toString();
        map.put(userId,Addrs);//"alice" "/172.168.."+"@"+"/localAddr" invalid+send
        String selfAddr=idpServer.getServer().getAddress().toString();
        for (String s : ipAndPort) {
            //log.warn("send"+s);
            if (("/" + s).equals(selfAddr)) continue;
            service.submit(
                    BroadCastMsg.builder().latch(latch).message(JSONObject.fromObject(map).toString()).mapper("invalidAddrFExp").IpAndPort(s)
                            .failsMap(resMap).build()
            );
        }
        try {
            latch.await();
            if(!resMap.isEmpty()){
                resMap.forEach((key, value) ->service.submit(
                        BroadCastMsg.builder().latch(latch).message(JSONObject.fromObject(map).toString()).mapper("invalidAddrFExp").IpAndPort(key)
                                .failsMap(resMap).build()));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        service.shutdown();
    }
}
