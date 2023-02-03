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
        }
        return object.toString();
    }


    public static Object Json2obj(String str,Class t) {
        JSONObject jsonobject = JSONObject.fromObject(str);
        if(t== FunctionFExp.class){
            return  JSONObject.toBean(jsonobject,FunctionFExp.class);
        }
        return  JSONObject.toBean(jsonobject,String.class);
    }
}
