package com.uestc.thresholddkg.Server.user;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCheckS;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.*;
import com.uestc.thresholddkg.Server.user.TokenComm.GetTokenSi;
import com.uestc.thresholddkg.Server.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.omg.PortableInterceptor.INACTIVE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangjia
 * @date 2023-02-03 20:38
 */
@Slf4j
@AllArgsConstructor
public class startToken implements HttpHandler {
    private ExecutorService service;

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var req=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(req));
        String tline="";
        String line;
        String res="";
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        log.error("startTokens"+tline);
        if(!tline.equals("")){
        IdPwd stu = new Gson().fromJson(tline, IdPwd.class);
        res=getToken(stu,service);
        }
        var expiresTime = new Date(System.currentTimeMillis() + 600 * 1000-8*1000*3600);
        byte[] respContents = res.getBytes("UTF-8");
        System.out.println(res);
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin","http://127.0.0.1:8083");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods","*");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Credentials","true");
        httpExchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers","content-type");
        httpExchange.getResponseHeaders().add("Set-Cookie","TokenS="+res+";expires="+expiresTime);
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }

    public static String getToken(IdPwd stu,ExecutorService service){
        String user=stu.getId(),Passwd=stu.getPasswd();
        //String user="sunny",Passwd="12345678";
        var userToken=new TokenUser();
        HashMap<String,String> paraMap=new HashMap<>();
        //check exists
        String[] isExist;
        String res=Convert2StrToken.Obj2json(new TokenUser());
        int times=0;
        do{
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {throw new RuntimeException(e);}
            isExist=getExistServ(service);times++;
            log.error("isExist "+isExist.length);
        }while(isExist.length<IdpServer.threshold&&times<7);
        SecureRandom random=new SecureRandom();
        int servI= random.nextInt(isExist.length);
        var dkg_systemT=StartPRF.getPwdHash1(user,Passwd,paraMap,isExist);
        var msg= TestUserMsg.builder().userId(user).pwd(RandomGenerator.genarateRandom(1024).toString()+":"+new Date().getTime()).man(false).num(333).email("12148688688@qq,com").build();
        var msgStr= Convert2StrToken.Obj2json(msg);
        var bytes=msgStr.getBytes();
        String[] ipPorts= IdpServer.addrS;
        SendUri send = SendUri.builder().message(user+"|"+"Passwd").mapper("startDkgT")//ID can't have "|"
                .IpAndPort(isExist[servI])
                .build();
        String dkg_Sys=send.SendMsg();
        DKG_SysStr dkg_sysStr=(DKG_SysStr) Convert2Str.Json2obj(dkg_Sys, DKG_SysStr.class);
        DKG_System dkg_system=new DKG_System(new BigInteger(dkg_sysStr.getP()),new BigInteger(dkg_sysStr.getQ()),
                new BigInteger(dkg_sysStr.getG()),new BigInteger(dkg_sysStr.getH()));
        BigInteger p=dkg_system.getP();
        BigInteger msgBigIn=new BigInteger(1,bytes).mod(dkg_system.getP());
        var b1=new BigInteger(1, Arrays.copyOfRange(bytes,0, bytes.length/2));
        var b2=new BigInteger(1,Arrays.copyOfRange(bytes,bytes.length/2,bytes.length));
        var msgHash=(dkg_system.getG().modPow(b1,p).multiply(dkg_system.getH().modPow(b2,p))).mod(p);
        while(times < 7){times++;
            String[] sendAddrs=new String[IdpServer.threshold];
            boolean[] exists=new boolean[isExist.length];
            for(int i=0;i<sendAddrs.length;){
                SecureRandom random1=new SecureRandom();
                int servi= random1.nextInt(isExist.length);
                if(!exists[servi]){sendAddrs[i]=isExist[servi];i++;exists[servi]=true;}
            }
            var getTokenS=(new GetTokenSi(sendAddrs,userToken,user, UserMsg2Serv.builder().msg(msgBigIn.toString())
                    .PwdHash1(paraMap.get("pwdHash1")).msgTime(String.valueOf(new Date().getTime())).msgHash(msgHash.toString()).userId(user).build(),paraMap.get("randR"),dkg_systemT,Passwd,service));
            try {
                boolean success=getTokenS.call();
                if(success)break;
                else {
                    try {Thread.sleep(900);//cautious
                    } catch (InterruptedException e) {throw new RuntimeException(e);}
                }
            }catch (RuntimeException e){times=7;}
        }
        if(times==7){res=Convert2StrToken.Obj2json(new TokenUser());}else{
            res=Convert2StrToken.Obj2json(userToken);
        }
        return res;
    }
    public  static String[] getExistServ( ExecutorService service){
        String[] ipPorts = IdpServer.addrS;
        Integer serversNum = ipPorts.length;
        CountDownLatch latch = new CountDownLatch(ipPorts.length);
        ConcurrentMap<String, String> resMap = new ConcurrentHashMap<>();
        final AtomicInteger failureCounter = new AtomicInteger(0);
        final int maximumFailures = serversNum - (serversNum >> 1 + 1);

        for (String s:ipPorts) {
           service.submit(BroadCheckS.builder().resmap(resMap).maxFail(maximumFailures).failCount(failureCounter)
                   .mapper("isExist").message("null").latch(latch).IpAndPort(s).build());
        }
        try {
            latch.await();
            String[] res=new String[resMap.size()];int []in=new int[]{0};
            resMap.forEach((k,v)->{res[in[0]]=k;in[0]++;});
            return res;
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
