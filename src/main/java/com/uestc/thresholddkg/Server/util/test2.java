package com.uestc.thresholddkg.Server.util;


import com.uestc.thresholddkg.Server.pojo.TestConv;
import net.sf.json.JSONObject;

/**
 * @author zhangjia
 * @date 2023-01-02 23:34
 */
public class test2 implements ObjToJson{
    public test2(){}
    @Override
    public  String Obj2json(Object obj) {
        JSONObject object= JSONObject.fromObject((TestConv)obj);
        return object.toString();
    }

    @Override
    public Object Json2obj(String str) {
        JSONObject jsonobject = JSONObject.fromObject(str);
        return  JSONObject.toBean(jsonobject,TestConv.class);
    }
}
