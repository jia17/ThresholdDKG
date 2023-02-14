package com.uestc.thresholddkg.Server.user.TokenComm;

import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.BroadCastMsg;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.communicate.UserBroadGetTokeni;
import com.uestc.thresholddkg.Server.communicate.UserBroadHash1;
import com.uestc.thresholddkg.Server.pojo.*;
import com.uestc.thresholddkg.Server.user.PrfComm.GetVeriPrf;
import com.uestc.thresholddkg.Server.user.PrfComm.PrfBroad;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.Convert2StrToken;
import com.uestc.thresholddkg.Server.util.DKG;
import javafx.beans.binding.BooleanBinding;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import net.sf.json.JSONObject;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangjia
 * @date 2023-02-04 15:57
 * get token碎片 from servs
 */
@AllArgsConstructor
@Slf4j
public class GetTokenSi   {
    private String[] sendAddrs;
    private TokenUser tokenUser;
    private String user;
    private UserMsg2Serv userMsg2Serv;
    private String randR;
    private DKG_System dkg_systemP;
    private String passwd;
     public Boolean call() throws NullPointerException {
        BigInteger randRInv=Calculate.modInverse(new BigInteger(randR),dkg_systemP.getQ());
        DKG_SysStr dkg_sysStrP=new DKG_SysStr(dkg_systemP.getP().toString(),dkg_systemP.getQ().toString()
                ,dkg_systemP.getG().toString(),dkg_systemP.getH().toString());
        userMsg2Serv.setDkg_sysStrP(dkg_sysStrP);
        String[] ipPorts= IdpServer.addrS;
        Integer[] AddrIndex=new Integer[sendAddrs.length];
        Set<String> addrSet = new HashSet<>(Arrays.asList(sendAddrs));//遍历乱序,小心
        Map<String,Integer> addrMap=new HashMap<>();
        for(int i=0;i<sendAddrs.length;i++){addrMap.put(sendAddrs[i],i);}
        for(int i=0,t=0;i<ipPorts.length;i++){
           // if(addrMap.contains(ipPorts[i])){AddrIndex[t]=i+1;t++;}
            if(addrMap.containsKey(ipPorts[i])){AddrIndex[addrMap.get(ipPorts[i])]=i+1;}//cautious
        }
        userMsg2Serv.setAddrIndex(AddrIndex);
        ExecutorService executor = Executors.newFixedThreadPool(ipPorts.length);
        CountDownLatch latch=new CountDownLatch(sendAddrs.length);
        ConcurrentHashMap<Integer,String> resMap=new ConcurrentHashMap<>();
        ConcurrentHashMap<String,String> lambdaMap=new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> verySet=new ConcurrentSkipListSet<>();
        SecureRandom random=new SecureRandom();
        int servI= random.nextInt(ipPorts.length);
        SendUri send = SendUri.builder().message(userMsg2Serv.getUserId()).mapper("getPubParam")
                .IpAndPort(ipPorts[servI])
                .build();
        String PubPara=send.SendMsg();
        if(PubPara.equals("")){log.error("DKG token false");return false;}
        PubParamToken pubParamToken=(PubParamToken) Convert2StrToken.Json2obj(PubPara, PubParamToken.class);
        for (int i=0;i<ipPorts.length;i++) {
            if(addrMap.containsKey(ipPorts[i])){
            executor.submit(
                    UserBroadGetTokeni.builder().latch(latch).message(Convert2StrToken.Obj2json(userMsg2Serv)).mapper("sendTokenI").IpAndPort(ipPorts[i])
                            .resMap(resMap).index(AddrIndex[addrMap.get(ipPorts[i])]).lambdaMap(lambdaMap).build()
            );}else{
                executor.submit(
                        BroadCastMsg.builder().latch(new CountDownLatch(3)).message(Convert2StrToken.Obj2json(userMsg2Serv)).mapper("getMsg").IpAndPort(ipPorts[i])
                                .failsMap(new ConcurrentHashMap<>()).build());
            }
        }
        try {
            latch.await();
            executor.shutdown();
            Integer threshold=IdpServer.threshold;
            if(resMap.size()<threshold){log.error("ToKen size too less");return false;}
            BigInteger p=new BigInteger(pubParamToken.getDkg_sysStr().getP());
            BigInteger q=new BigInteger(pubParamToken.getDkg_sysStr().getQ());
            BigInteger g=new BigInteger(pubParamToken.getDkg_sysStr().getG());
            BigInteger y=new BigInteger(pubParamToken.getY());
            BigInteger gm=g.modPow(new BigInteger(userMsg2Serv.getMsg()),p);
            BigInteger W=BigInteger.ONE;
            Integer listMuls=1;
            String Cvery="";
            String[] EWI=new String[threshold],WI=new String[threshold];
            BigInteger[] UI=new BigInteger[threshold],HI=new BigInteger[threshold];
            Integer[] indexI=new Integer[threshold];int i=0;//index from [1,N]
            BigInteger PwdHashEncS=BigInteger.ONE;
            BigInteger qP=dkg_systemP.getQ(),pP=dkg_systemP.getP();
            for (Integer val : AddrIndex) { listMuls*=val;  }
            for (var val:resMap.entrySet()) {
                TokenSi tokenSi=(TokenSi) Convert2StrToken.Json2obj(val.getValue(), TokenSi.class);
                Cvery=tokenSi.getCvery();
                BigInteger pwdHashEncI=new BigInteger(tokenSi.getEncPwdHash1());
                int tempMul=0;int index=val.getKey();
                indexI[i]=index;
                System.out.println(index);
                tempMul=val.getKey();
                if(((threshold)&1)==0)tempMul*=-1;
                for(int j=0;j<AddrIndex.length;j++){
                    if(index!=AddrIndex[j]){tempMul*=(index-AddrIndex[j]);}
                }
                PwdHashEncS=PwdHashEncS.multiply(Calculate.modPow(pwdHashEncI,
                        Calculate.modInverse(BigInteger.valueOf(tempMul),qP).multiply(BigInteger.valueOf(listMuls)).mod(qP)
                        ,pP)).mod(pP);
                BigInteger Hi=new BigInteger(userMsg2Serv.getMsgHash()).multiply(Calculate.modInverse(BigInteger.valueOf(Math.abs(tempMul)),q)).mod(q);
                if(tempMul<0)Hi=q.add(Hi.negate()).mod(q);
                Hi=Hi.multiply(BigInteger.valueOf(listMuls)).mod(q);
                EWI[i]= tokenSi.getWi();
                HI[i]=Hi;UI[i]=new BigInteger(tokenSi.getUi());
                i++;
            }
            PwdHashEncS=PwdHashEncS.modPow(randRInv,pP);
            System.out.println("PWDHASH!" +PwdHashEncS);
            var hash2= DKG.HashRipe160(passwd+PwdHashEncS);
            var hash3=DKG.HashSha3(hash2);
            var prfVery= Hex.toHexString(Arrays.copyOfRange(hash3,0,hash3.length/2));
            var prfServ=Hex.toHexString(Arrays.copyOfRange(hash3,hash3.length/2,hash3.length));
            if(!prfVery.equals(Cvery))log.warn("VERY ERROR!!!!");
            byte[] bytesI=new byte[16];String[] DecHi=new String[ipPorts.length];
            for( i = 0; i<ipPorts.length; i++){
                bytesI[0]= (byte) i;
                var hi=DKG.HashBlake2bSalt(prfServ.getBytes(),bytesI);
                DecHi[i]=hi;
                //System.out.println(String.valueOf(i)+" hi "+hi);
            }
            for(i=0;i<threshold;i++){
                int index=indexI[i];
                try {
                    WI[i]=DKG.AESdecrypt(EWI[i],DecHi[index-1]);
                } catch (NoSuchAlgorithmException e) {
                    log.error("AES DEC ERROR");
                    throw new RuntimeException(e);
                }
                BigInteger Wi=new BigInteger(WI[i]);
                BigInteger uW=UI[i].modPow(HI[i],p)
                        .multiply(Wi.modPow(BigInteger.valueOf(2),p)).mod(p);
                if(!uW.equals(gm)){
                    log.error(String.valueOf(index)+" verify tokeni false"+" \n");//+tokenSi.getUi()+"\n"+tokenSi.getWi()+" \n"+Hi.toString()
                    return false;
                }else{ log.error(String.valueOf(index)+" verify tokeni True");}
                W=W.multiply(Wi).mod(p);
            }

            BigInteger YW=y.modPow(new BigInteger(userMsg2Serv.getMsgHash()),p);
            YW=YW.multiply(W.modPow(BigInteger.valueOf(2),p)).mod(p);
            BigInteger msg=new BigInteger(userMsg2Serv.getMsg()).mod(q);//cautious!!!mod
            BigInteger GkM=g.modPow(msg.multiply(BigInteger.valueOf(threshold)).mod(p),p);
            if(!YW.equals(GkM)){
                System.out.println("FFFFFFFFFFFFFFFFFFFFFFFFFFFFF YW");return false;
            }else{ System.out.println("tttttttttttttt");}//Token Success!
            tokenUser.setUser(user);tokenUser.setSign(W.toString());tokenUser.setY(y.toString());
            return true;
        }catch (InterruptedException e){
            executor.shutdown();
            throw new RuntimeException();
        }
    }
}
