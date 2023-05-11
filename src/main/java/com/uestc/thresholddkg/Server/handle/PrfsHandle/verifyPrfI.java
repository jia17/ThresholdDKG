package com.uestc.thresholddkg.Server.handle.PrfsHandle;

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
import com.uestc.thresholddkg.Server.pojo.FunctionGHvals;
import com.uestc.thresholddkg.Server.pojo.PrfValue;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import com.uestc.thresholddkg.Server.util.DKG;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import javax.persistence.Id;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author zhangjia
 * @date 2023-02-21 12:11
 */
@AllArgsConstructor
@Slf4j
public class verifyPrfI implements HttpHandler {
    private IdpServer idpServer;

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        byte[] respContents = "success".getBytes("UTF-8");
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        FunctionGHvals ghVals=(FunctionGHvals) Convert2Str.Json2obj(tline, FunctionGHvals.class);
        PrfValue prfValue=(PrfValue) Convert2Str.Json2obj(ghVals.getUserId(), PrfValue.class);
        DKG_SysStr dkg_sysStr=(DKG_SysStr)Convert2Str.Json2obj(ghVals.getSendAddr(),DKG_SysStr.class);
        DKG_System dkgSys=new DKG_System(new BigInteger(dkg_sysStr.getP()),new BigInteger(dkg_sysStr.getQ()),
                                             new BigInteger(dkg_sysStr.getG()),new BigInteger(dkg_sysStr.getH()));
        String userId=prfValue.getId();
        BigInteger p=dkgSys.getP();
        BigInteger verify1=((Calculate.modPow(dkgSys.getG(),new BigInteger(ghVals.getFi()),p)).multiply(Calculate.modPow(dkgSys.getH(),new BigInteger(ghVals.getGi()),p))).mod(p);
        boolean satisfy= DKG.VerifyGH(verify1,DKG.str2BigInt(ghVals.getGMulsH()),ghVals.getServerId(),p);
        if(satisfy){
            //写入server
            ServPrfsMapper servPrfsMapper= ServPrfsWR.getMapper();
            QueryWrapper<ServPrfs> queryWrapper=new QueryWrapper<>();
            queryWrapper.eq("servId",idpServer.getServerId()).eq("userId",userId);
            if(ghVals.getChangePwd()||servPrfsMapper.selectOne(queryWrapper)==null) {
                servPrfsMapper.delete(queryWrapper);
                BigInteger secretI = new BigInteger(ghVals.getFi());
                servPrfsMapper.insert(ServPrfs.builder().servId(idpServer.getServerId())
                        .userId(userId).priKeyi(secretI.toString()).prfi(prfValue.getPrfI()).verify(prfValue.getCveri()).build());
            }else{
                respContents = "false".getBytes("UTF-8");
            }
            ServPrfsPPMapper servPrfsPPMapper= ServPrfsPpWR.getMapper();
            synchronized (this){
            ServPrfsPp servPrfsPp=servPrfsPPMapper.selectById(userId);
            if(ghVals.getChangePwd()||servPrfsPp==null){
                servPrfsPPMapper.deleteById(userId);
                servPrfsPPMapper.insert( ServPrfsPp.builder().userId(userId).p(dkg_sysStr.getP())
                        .q(dkg_sysStr.getQ()).g(dkg_sysStr.getG()).h(dkg_sysStr.getH()).build());
            }}
        }else{
            log.error("Verify PrfI error "+idpServer.getServerId());
        }
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
