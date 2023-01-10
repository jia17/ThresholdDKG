package com.uestc.thresholddkg.Server.util;

import com.uestc.thresholddkg.Server.pojo.DKG_System;
import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import com.uestc.thresholddkg.Server.pojo.TestConv;
import lombok.NoArgsConstructor;
import net.sf.json.JSONObject;

/**
 * @author zhangjia
 * @date 2023-01-10 11:08
 */
@NoArgsConstructor
public class DkgSystem2Obj implements ObjToJson {

    @Override
    public String Obj2json(Object obj) {
        JSONObject object= JSONObject.fromObject((DkgSysMsg)obj);
        return object.toString();
    }

    @Override
    public Object Json2obj(String str) {
        JSONObject jsonobject = JSONObject.fromObject(str);
        return  JSONObject.toBean(jsonobject,DkgSysMsg.class);
    }
}
