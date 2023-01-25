package com.uestc.thresholddkg.Server.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.uestc.thresholddkg.Server.IdpServer;
import com.uestc.thresholddkg.Server.pojo.PrfValue;
import com.uestc.thresholddkg.Server.util.Calculate;
import com.uestc.thresholddkg.Server.util.Convert2Str;
import lombok.AllArgsConstructor;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentMap;

/**
 * @author zhangjia
 * @date 2023-01-25 17:21
 */
@AllArgsConstructor
public class GetPrfs implements HttpHandler {
    private IdpServer idpServer;
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        var Sender=httpExchange.getRequestBody();
        BufferedReader reader=new BufferedReader(new InputStreamReader(Sender));
        String tline="";
        String line;
        while((line=reader.readLine())!=null){
            tline+=line;
        }
        PrfValue prfValue=(PrfValue) Convert2Str.Json2obj(tline, PrfValue.class);
        String Id=prfValue.getId();
        idpServer.getPrfHi().put(Id, prfValue.getPrfI());
        idpServer.getPrfVerify().put(Id, prfValue.getCveri());

        BigInteger secretI= Calculate.addsPow(idpServer.getFgRecv().get(Id),idpServer.getDkgParam().get(Id).getQ());
        //System.out.println("___________Ki "+secretI.toString()+" Fi "+idpServer.getPrfHi().get(Id)+" vi "+idpServer.getPrfVerify().get(Id));
        byte[] respContents = "getPrfs".getBytes("UTF-8");
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        httpExchange.sendResponseHeaders(200, respContents.length);
        httpExchange.getResponseBody().write(respContents);
        httpExchange.close();
    }
}
