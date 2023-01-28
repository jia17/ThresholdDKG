package com.uestc.thresholddkg.Server.handle;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.persist.ServPrfs;
import com.uestc.thresholddkg.Server.persist.ServPrfsPp;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsMapper;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsPPMapper;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServPrfsPpWR;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServPrfsWR;
import com.uestc.thresholddkg.Server.pojo.DKG_SysStr;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.IdHash1;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.concurrent.Executor;

/**
 * @author zhangjia
 * @date 2023-01-24 16:51
 */
@Slf4j
@AllArgsConstructor
public class EncHash1Pwd implements HttpHandler {
    private IdpServer idpServer;


    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){tline+=line;}
        IdHash1 idHash1=(IdHash1)Convert2Str.Json2obj(tline, IdHash1.class);
        String ID=idHash1.getId();
        ServPrfsPPMapper servPrfsPPMapper= ServPrfsPpWR.getMapper();
        ServPrfsPp servPrfsPp=servPrfsPPMapper.selectById(ID);

        ServPrfsMapper servPrfsMapper= ServPrfsWR.getMapper();
        QueryWrapper<ServPrfs> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("servId",idpServer.getServerId()).eq("userId",ID);
        var sec=servPrfsMapper.selectByIdAndF(idpServer.getServerId(),ID);
        //System.out.println(idpServer.getFlag());
        //cautious sec==null,not pp=null;
        if(sec==null&&(!idpServer.getFlag().containsKey(ID)||idpServer.getFlag().get(ID)!=2)) {//servPrfsPp==null&&
            //if(idpServer.getFlag().containsKey(ID)) System.out.println(idpServer.getServer().getAddress().toString()+"  "+idpServer.getFlag().get(ID));
            byte[] respContents = "str".getBytes("UTF-8");
            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(500, respContents.length);
            httpExchange.getResponseBody().write(respContents);
            httpExchange.close();return;
        }

        DKG_System dkg_system=null;BigInteger secretI=null;Integer exists=0;String CVery="";String len="0";
        if(sec==null){
            try {
                dkg_system=idpServer.getDkgParam().get(ID);
                secretI= Calculate.addsPow(idpServer.getFgRecv().get(ID),dkg_system.getQ());
            }catch (NullPointerException e){}
        }else{
            dkg_system=new DKG_System(new BigInteger(servPrfsPp.getP()),new BigInteger(servPrfsPp.getQ()),
                    new BigInteger(servPrfsPp.getG()),new BigInteger(servPrfsPp.getH()));
            secretI=new BigInteger(sec.getPriKeyi());
            CVery=sec.getVerify();
            exists=1;len="1";
        }
        //System.out.println(idpServer.getServerId()+"  "+ secretI);
        BigInteger Hash1Pwd=new BigInteger(idHash1.getPwdHash1());
        BigInteger EncHash1Pwd=Hash1Pwd.modPow(secretI,dkg_system.getP());
        len=String.valueOf(EncHash1Pwd.toString().length())+len;
        while (len.length()<8)len="0"+len;
        String res=len+EncHash1Pwd+CVery;//len+"1 or 0"+prfi+veri cautious
        byte[] respContents = res.getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
