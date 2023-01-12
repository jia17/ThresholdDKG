package com.uestc.thresholddkg.Server.DkgCommunicate;

import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCastMsg;
import com.uestc.thresholddkg.Server.pojo.DKG_SysStr;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.DKG;
import com.uestc.thresholddkg.Server.util.DkgSystem2Obj;
import com.uestc.thresholddkg.Server.util.RandomGenerator;
import lombok.AllArgsConstructor;
import lombok.var;
import net.sf.json.JSONObject;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.*;

/**
 * send twice invalid server;
 * @author zhangjia
 * @date 2023-01-11 21:58
 */
public class InvalidServBroad implements Runnable{
    private IdpServer idpServer;
    private String userId;
    private String Addr;
    private String[] ipAndPort;
    private Map<String, ConcurrentSkipListSet<String>> recvInvalid;

    public InvalidServBroad(IdpServer idpServer1,String userId1,String Addr1,Map<String, ConcurrentSkipListSet<String>> recvInvalid){
        idpServer=idpServer1;userId=userId1;Addr=Addr1;ipAndPort=IdpServer.addrS;
        this.recvInvalid=recvInvalid;
    }
    //invalidAddr
    @Override
    public void run() {
        int serversNum= ipAndPort.length;

        ExecutorService service = Executors.newFixedThreadPool(serversNum - 1);
        CountDownLatch latch=new CountDownLatch(0);
        Map<String,String> map=new HashMap<>();
        ConcurrentHashMap<String,String> resMap=new ConcurrentHashMap<>();
        String Addrs=Addr+"@"+idpServer.getServer().getAddress().toString();
        map.put(userId,Addrs);//"alice" "/172.168.."+"@"+"/localAddr" invalid+send
        String selfAddr=idpServer.getServer().getAddress().toString();
        if(!recvInvalid.containsKey(userId))recvInvalid.put(userId,new ConcurrentSkipListSet<>());
        ConcurrentSkipListSet<String> receivedInv=recvInvalid.get(userId);
        var convert=new Convert2Str();
        for (String s : ipAndPort) {
            //log.warn("send"+s);
            System.out.println("invalidNum"+receivedInv.size());
            if (("/" + s).equals(selfAddr)||receivedInv.contains("/"+s)) continue;
            service.submit(
                    BroadCastMsg.builder().latch(latch).message(JSONObject.fromObject(map).toString()).mapper("invalidAddr").IpAndPort(s)
                            .failsMap(resMap).build()
            );
        }
        try {
            latch.await();
            if(!resMap.isEmpty()){
                resMap.forEach((key, value) ->service.submit(
                        BroadCastMsg.builder().latch(latch).message(JSONObject.fromObject(map).toString()).mapper("invalidAddr").IpAndPort(key)
                                .failsMap(resMap).build()));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        service.shutdown();
    }
}
