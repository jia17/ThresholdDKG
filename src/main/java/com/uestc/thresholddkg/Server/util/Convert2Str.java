package com.uestc.thresholddkg.Server.util;

import com.uestc.thresholddkg.Server.pojo.*;
import lombok.NoArgsConstructor;
import net.sf.json.JSONObject;

/**
 * @author zhangjia
 * @date 2023-01-12 09:47
 */
@NoArgsConstructor
public class Convert2Str {

    public static String Obj2json(Object obj) {
        JSONObject object=null;
        if(obj instanceof DkgSysMsg){
            object= JSONObject.fromObject((DkgSysMsg)obj);
        }else
        if(obj instanceof Complain){
            object= JSONObject.fromObject((Complain)obj);
        }else
        if(obj instanceof FunctionGHvals){
            object= JSONObject.fromObject((FunctionGHvals)obj);
        }else
        if(obj instanceof ReGetGF){
            object=JSONObject.fromObject((ReGetGF)obj);
        }else
        if(obj instanceof Secreti){
            object=JSONObject.fromObject((Secreti)obj);
        }else
        if(obj instanceof DKG_SysStr){
            object=JSONObject.fromObject((DKG_SysStr)obj);
        }else
        if(obj instanceof IdHash1){
            object=JSONObject.fromObject((IdHash1)obj);
        }else
        if(obj instanceof PrfValue){
            object=JSONObject.fromObject((PrfValue)obj);
        }
        return object.toString();
    }


    public static Object Json2obj(String str,Class t) {
        JSONObject jsonobject = JSONObject.fromObject(str);
        if(t== DkgSysMsg.class){
            return  JSONObject.toBean(jsonobject,DkgSysMsg.class);
        }else
        if(t== Complain.class){
            return  JSONObject.toBean(jsonobject,Complain.class);
        }else
        if(t== FunctionGHvals.class){
            return  JSONObject.toBean(jsonobject,FunctionGHvals.class);
        }else
        if(t== ReGetGF.class){
            return  JSONObject.toBean(jsonobject,ReGetGF.class);
        }else
        if(t== Secreti.class){
            return JSONObject.toBean(jsonobject,Secreti.class);
        }else
        if(t== DKG_SysStr.class){
            return JSONObject.toBean(jsonobject,DKG_SysStr.class);
        }else
        if(t== IdHash1.class){
            return JSONObject.toBean(jsonobject,IdHash1.class);
        }else
        if(t== PrfValue.class){
            return JSONObject.toBean(jsonobject,PrfValue.class);
        }
        return  JSONObject.toBean(jsonobject,DkgSysMsg.class);
    }
}
