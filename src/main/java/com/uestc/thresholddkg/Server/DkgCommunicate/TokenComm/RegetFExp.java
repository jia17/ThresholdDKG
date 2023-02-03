package com.uestc.thresholddkg.Server.DkgCommunicate.TokenComm;

import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCastMsg;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.ReGetGF;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import lombok.AllArgsConstructor;
import lombok.var;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-02-03 23:06
 */
@AllArgsConstructor
public class RegetFExp implements Runnable{
    private IdpServer idpServer;
    private String userId;
    private String remoteAddr;

    @Override
    public void run() {
        var message=new ReGetGF(userId,idpServer.getServer().getAddress().toString());
        SendUri send = SendUri.builder().message(Convert2Str.Obj2json(message)).mapper("resendFExp").IpAndPort(remoteAddr.substring(1)).build();
        send.SendMsg();
    }

}
