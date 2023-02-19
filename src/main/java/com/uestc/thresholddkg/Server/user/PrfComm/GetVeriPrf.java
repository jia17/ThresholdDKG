package com.uestc.thresholddkg.Server.user.PrfComm;

import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.communicate.SendUri;
import com.uestc.thresholddkg.Server.pojo.PrfValue;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.DKG;
import lombok.var;
import org.bouncycastle.util.encoders.Hex;

import javax.persistence.Id;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangjia
 * @date 2023-01-27 12:40
 */
public class GetVeriPrf implements Runnable{
    private String[] ipPorts;
    private String pwd;
    private String userId;
    private String hash1Pwd;
    private String Cveri;
    private ExecutorService service1;

    public GetVeriPrf(String Id,String pwd,String hash1,String Cveri,ExecutorService service1){
        userId=Id;this.pwd=pwd;hash1Pwd=hash1;ipPorts= IdpServer.addrS;this.Cveri=Cveri;this.service1=service1;
    }

    @Override
    public void run(){
        var hash2= DKG.HashRipe160(pwd+hash1Pwd);
        var hash3=DKG.HashSha3(hash2);
        var prfVery= Hex.toHexString(Arrays.copyOfRange(hash3,0,hash3.length/2));
        var prfServ=Hex.toHexString(Arrays.copyOfRange(hash3,hash3.length/2,hash3.length));
        var service= service1;//Executors.newFixedThreadPool(ipPorts.length);
        if(prfVery.equals(Cveri)){
            System.out.println(userId+" get true Prf "+prfServ);
        }else{
            System.out.println(Cveri+" "+prfVery+" false");
        }
        byte[] bytesI=new byte[16];
        for(int i = 0; i<ipPorts.length; i++){//0-n-1 for serv 1-n;serv :9010,90 2 0;
            bytesI[0]= (byte) i;
            var hi=DKG.HashBlake2bSalt(prfServ.getBytes(),bytesI);
            var megPrf=new PrfValue(userId,hi,prfVery);
            //System.out.println(String.valueOf(i)+" hi "+hi);
        }
        //service.shutdown();
    }
}
