package com.uestc.thresholddkg.Server.user;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.*;
import com.uestc.thresholddkg.Server.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.bouncycastle.util.encoders.Hex;

import javax.persistence.Id;
import javax.xml.ws.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

/**
 * @author zhangjia
 * @date 2023-01-23 09:53
 */
@Slf4j
@AllArgsConstructor
public class StartPRF implements HttpHandler {
    private HttpServer userServer;
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
        if(!tline.equals("")) {
            IdPwd stu = new Gson().fromJson(tline, IdPwd.class);
            String ID=stu.getId(),Passwd=stu.getPasswd();
            VSS_Pwd(ID,Passwd,service);
            HashMap<String, String> paraMap = new HashMap<>();
        }
        byte[] respContents = "sPrf".getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin","http://127.0.0.1:8083");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods","*");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Credentials","true");
        httpExchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers","content-type");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }

    public static void VSS_Pwd(String ID,String passwd,ExecutorService service1){
        var param=DKG.initDLog();
        String[] ipPorts=IdpServer.addrS;
        BigInteger[] secrets=new BigInteger[2];
        secrets[0]=RandomGenerator.genaratePositiveRandom(param.getP());
        secrets[1]=RandomGenerator.genaratePositiveRandom(param.getP());
        BigInteger g=param.getG();
        BigInteger h=param.getH();
        BigInteger p=param.getP();
        BigInteger q=param.getQ();
        BigInteger[]F= DKG.generateFuncParam(secrets[0],p);
        BigInteger[]G=DKG.generateFuncParam(secrets[1],p);
        BigInteger[] fVal=DKG.FunctionValue(F,q);
        BigInteger[] gVal=DKG.FunctionValue(G,q);
        BigInteger[] gMulH=DKG.FunctionGH(g,h,F,G,p);
        byte[] bytes= DKG.HashSha3(passwd);
        var b1=new BigInteger(1, Arrays.copyOfRange(bytes,0,DKG.KeyLen/16));
        var b2=new BigInteger(1,Arrays.copyOfRange(bytes,DKG.KeyLen/16,DKG.KeyLen/8));
        var pwdHash1=(g.modPow(b1,p).multiply(h.modPow(b2,p))).mod(p);
        String temps="";int hash1Len=(int)(DKG.KeyLen*0.30103);
        if(pwdHash1.toString().length()<hash1Len){//154=512*0.302=*ln(2)/ln(10)
            StringBuilder blank= new StringBuilder();
            for(int i=hash1Len-pwdHash1.toString().length();i>0;i--)blank.append("0");
            temps=blank.toString()+pwdHash1.toString();
            pwdHash1=new BigInteger(temps);
        }
        BigInteger pwdHash1Pow=pwdHash1.modPow(secrets[0].mod(q),p);
        var hash2= DKG.HashRipe160(passwd+pwdHash1Pow);
        var hash3=DKG.HashSha3(hash2);
        var prfVery= Hex.toHexString(Arrays.copyOfRange(hash3,0,hash3.length/2));
        var prfServ=Hex.toHexString(Arrays.copyOfRange(hash3,hash3.length/2,hash3.length));
        byte[] bytesI=new byte[16];
        ExecutorService service=service1;
        DKG_SysStr dkg_sysStr=new DKG_SysStr(param.getP().toString(),param.getQ().toString(),param.getG().toString(),param.getH().toString());
        for(int i = 0; i<ipPorts.length; i++){
            bytesI[0]= (byte) i;
            var hi=DKG.HashBlake2bSalt(prfServ.getBytes(),bytesI);
            var megPrf=new PrfValue(ID,hi,prfVery);
            FunctionGHvals message=FunctionGHvals.builder()
                    .gMulsH(DKG.bigInt2Str(gMulH))
                    .sendAddr(Convert2Str.Obj2json(dkg_sysStr))//cautious
                    .userId(Convert2Str.Obj2json(megPrf)).item(1).build();
            String s=ipPorts[i];
            message.setFi(fVal[i].toString());message.setGi(gVal[i].toString());message.setServerId(i+1);
            SendUri send = SendUri.builder().message(Convert2Str.Obj2json(message)).mapper("verifyGetPrfI").IpAndPort(s).build();
            service.submit(send::SendMsg);
        }
        //service.shutdown();
    }
    public static DKG_System getPwdHash1(String ID,String Passwd,HashMap<String,String> paraMap,String[] isExist){
        SecureRandom random=new SecureRandom();
        int servI= random.nextInt(isExist.length);
        SendUri send = SendUri.builder().message(ID+"|"+"Passwd").mapper("startDkg")//ID can't have "|"
                .IpAndPort(isExist[servI])
                .build();
        String dkg_Sys=send.SendMsg();
        DKG_SysStr dkg_sysStr=(DKG_SysStr) Convert2Str.Json2obj(dkg_Sys, DKG_SysStr.class);
        DKG_System dkg_system=new DKG_System(new BigInteger(dkg_sysStr.getP()),new BigInteger(dkg_sysStr.getQ()),
                new BigInteger(dkg_sysStr.getG()),new BigInteger(dkg_sysStr.getH()));
        log.error("Uesr get PRF "+dkg_system);
        BigInteger p=dkg_system.getP();
        byte[] bytes= DKG.HashSha3(Passwd);
        var b1=new BigInteger(1, Arrays.copyOfRange(bytes,0,DKG.KeyLen/16));
        var b2=new BigInteger(1,Arrays.copyOfRange(bytes,DKG.KeyLen/16,DKG.KeyLen/8));
        var pwdHash1=(dkg_system.getG().modPow(b1,p).multiply(dkg_system.getH().modPow(b2,p))).mod(p);
        String temps="";int hash1Len=(int)(DKG.KeyLen*0.30103);
        if(pwdHash1.toString().length()<hash1Len){//154=512*0.302=*ln(2)/ln(10)
            StringBuilder blank= new StringBuilder();
            for(int i=hash1Len-pwdHash1.toString().length();i>0;i--)blank.append("0");
            temps=blank.toString()+pwdHash1.toString();
            pwdHash1=new BigInteger(temps);
        }
        System.out.println(pwdHash1.toString()+" Hash1len"+temps.length());
        BigInteger randR= RandomGenerator.genaratePositiveRandom(dkg_system.getQ());
        final BigInteger pwdHash11= pwdHash1.modPow(randR,p);
        paraMap.put("pwdHash1",pwdHash11.toString());
        paraMap.put("randR",randR.toString());
        return dkg_system;
    }
}
