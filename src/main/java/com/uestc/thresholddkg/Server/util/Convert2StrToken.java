package com.uestc.thresholddkg.Server.util;

import com.uestc.thresholddkg.Server.pojo.*;
import net.sf.json.JSONObject;

/**
 * @author zhangjia
 * @date 2023-02-03 21:18
 */
public class Convert2StrToken {
    public static String Obj2json(Object obj) {
        JSONObject object=null;
        if(obj instanceof FunctionFExp) {
            object = JSONObject.fromObject((FunctionFExp) obj);
        }else
        if(obj instanceof TestUserMsg){
            object = JSONObject.fromObject((TestUserMsg) obj);
        }else
        if(obj instanceof PubParamToken){
                object = JSONObject.fromObject((PubParamToken) obj);
        }else
        if(obj instanceof UserMsg2Serv){
            object = JSONObject.fromObject((UserMsg2Serv) obj);
        }else
        if(obj instanceof TokenSi){
            object = JSONObject.fromObject((TokenSi) obj);
        }else
        if(obj instanceof TokenUser){
            object = JSONObject.fromObject((TokenUser) obj);
        }
        return object.toString();
    }


    public static Object Json2obj(String str,Class t) {
        JSONObject jsonobject = JSONObject.fromObject(str);
        if(t== FunctionFExp.class){
            return  JSONObject.toBean(jsonobject,FunctionFExp.class);
        }else
        if(t == TestUserMsg.class){
            return   JSONObject.toBean(jsonobject,TestUserMsg.class);
        }else
        if(t == PubParamToken.class){
            return   JSONObject.toBean(jsonobject,PubParamToken.class);
        }else
        if(t == UserMsg2Serv.class){
            return   JSONObject.toBean(jsonobject,UserMsg2Serv.class);
        }else
        if(t == TokenSi.class){
            return   JSONObject.toBean(jsonobject,TokenSi.class);
        }else
        if(t == TokenUser.class){
            return   JSONObject.toBean(jsonobject,TokenUser.class);
        }
        return  JSONObject.toBean(jsonobject,String.class);
    }
}
