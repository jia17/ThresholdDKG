package com.uestc.thresholddkg.Server.handle.TokenHandle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BoradTest;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.*;
import com.uestc.thresholddkg.Server.user.TokenComm.GetTokenSi;
import com.uestc.thresholddkg.Server.user.startToken;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import com.uestc.thresholddkg.Server.util.RandomGenerator;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.var;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-02-28 15:02
 */
@AllArgsConstructor
public class TestTokenShow implements HttpHandler {
    private IdpServer idpServer;
    @Override
    @SneakyThrows
    public void handle(HttpExchange httpExchange) throws IOException {
        String userId=httpExchange.getRequestURI().toString().split("=")[1];
        System.out.println(userId);
        SendUri send = SendUri.builder().message(userId).mapper("getPubParam")//ID can't have "|"
                .IpAndPort(IdpServer.addrS[3])
                .build();
        String pubParam=send.SendMsg();
        PubParamToken pubParamToken=(PubParamToken) Convert2StrToken.Json2obj(pubParam, PubParamToken.class);
        DKG_SysStr dkg_sysStr=pubParamToken.getDkg_sysStr();
        DKG_System dkg_system=new DKG_System(new BigInteger(dkg_sysStr.getP()),new BigInteger(dkg_sysStr.getQ()),
                new BigInteger(dkg_sysStr.getG()),new BigInteger(dkg_sysStr.getH()));
        String[] ippStr=IdpServer.addrS;
        int serversNum=ippStr.length;
        var msg= TestUserMsg.builder().userId(userId).pwd("passwd").man(false).num(44434344).email("12148688688@qq,com").build();
        var msgStr= Convert2StrToken.Obj2json(msg);
        var bytes=msgStr.getBytes();
        String[] ipPorts= IdpServer.addrS;
        BigInteger p=dkg_system.getP();
        BigInteger msgBigIn=new BigInteger(1,bytes).mod(dkg_system.getP());
        var b1=new BigInteger(1, Arrays.copyOfRange(bytes,0, bytes.length/2));
        var b2=new BigInteger(1,Arrays.copyOfRange(bytes,bytes.length/2,bytes.length));
        var msgHash=(dkg_system.getG().modPow(b1,p).multiply(dkg_system.getH().modPow(b2,p))).mod(p);
        var user2Serv= UserMsg2Serv.builder().msg(msgBigIn.toString())
                .PwdHash1("null").msgHash(msgHash.toString()).userId(userId).build();

        final ExecutorService executor = Executors.newFixedThreadPool(serversNum);
        String[] isExist=startToken.getExistServ(executor);
        Integer[] addAddrs=new Integer[IdpServer.threshold];
        boolean[] exists=new boolean[isExist.length];
        for(int i=0;i<addAddrs.length;){
            SecureRandom random1=new SecureRandom();
            int servi= random1.nextInt(isExist.length);
            if(!exists[servi]){addAddrs[i]=IdpServer.addr2Index.get(isExist[servi]);i++;exists[servi]=true;}
        }
        user2Serv.setAddrIndex(addAddrs);
        final CountDownLatch latch  = new CountDownLatch(serversNum);//server self has a secretI
        ConcurrentHashMap<String,String> resPri=new ConcurrentHashMap<>();
        ConcurrentHashMap<String,String> resToken=new ConcurrentHashMap<>();
        int serverId=0;
        for (String addr:ippStr) {
            executor.submit(
                    BoradTest.builder().latch(latch).message(Convert2StrToken.Obj2json(user2Serv)).mapper("TestTokenApply").IpAndPort(addr)
                            .resmap(resPri).resPubMap(resToken).build()
            );
        }
        latch.await();
        BigInteger q=dkg_system.getQ();
        BigInteger g=dkg_system.getG();
        String sys="<h3 style=\"color:FF6633\">门限签名系统参数</h3>"+ MessageFormat.format("<h3>p:{0}  q:{1}</h3>",dkg_sysStr.getP(),dkg_sysStr.getQ());
        String[] res=new String[2];
        sys+=MessageFormat.format("<h3>addr:{0}</h3>",idpServer.getServer().getAddress().toString());
        res[0]=sys+ MessageFormat.format("<h3 style=\"color:FF6633\">用户名:<u>{0}</u></h3>",userId);
        res[0]+="<h4 style=\"color:FF6633\">公钥:"+pubParamToken.getY()+"</h4>";
        res[0]+="<h4 style=\"color:FF6633\">签名子密钥</h4>";
        resPri.forEach((k,v)->{
            res[0]+=MessageFormat.format("<h4>server:{0}  prfi:{1}</h4>",IdpServer.addr2Index.get(k),v);
        });
//        Integer threshold=IdpServer.threshold;
//        Integer[] Index=new Integer[threshold];
//        BigInteger[] value=new BigInteger[threshold];
//        Integer[] is={0};
//        resToken.forEach((k,v)->{if(is[0]<threshold){
//            int in=Integer.parseInt(String.valueOf(k.charAt(k.length()-2)));
//            Index[is[0]]=in;value[is[0]]=new BigInteger(v);is[0]=is[0]+1;}
//        });
//        Integer listMuls=1;
//        for (Integer val : Index) { listMuls*=val;  }
//        BigInteger secret2=BigInteger.ZERO;
//        BigInteger tempBigMul;
//        Integer tempMul=0;
//        for(int  i=0;i<threshold;i++){
//            tempMul=Index[i];
//            if(((threshold)&1)==0)tempMul*=-1;
//            for(int j=0;j<threshold;j++){
//                if(i!=j){tempMul*=(Index[i]-Index[j]);}
//            }
//            tempBigMul=value[i].multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)),q)).mod(q);
//            if(tempMul<0)secret2=(secret2.add(q.add(tempBigMul.negate()))).mod(q);
//            else secret2=secret2.add(tempBigMul).mod(q);
//        }
//        secret2=(secret2.multiply(BigInteger.valueOf(listMuls))).mod(q);
        res[0]+="<h4 style=\"color:FF6633\">子签名</h4>";
        BigInteger[] secret2=new BigInteger[]{BigInteger.ONE};
        resToken.forEach((k,v)-> {
            res[0]+=MessageFormat.format("<h4>server:{0}  =tokeni:{1}",IdpServer.addr2Index.get(k),v);
            secret2[0]=secret2[0].multiply(new BigInteger(v)).mod(p);});
        res[0]+="<h4 style=\"color:FF6633\">门限签名</h4><h4  style=\"color:33FF33\">token  =  "+secret2[0].toString();
        byte[] respContents = res[0].getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");

        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
