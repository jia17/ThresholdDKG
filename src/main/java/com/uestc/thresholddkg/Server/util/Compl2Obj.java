package com.uestc.thresholddkg.Server.util;

import com.uestc.thresholddkg.Server.pojo.Complain;
import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import lombok.NoArgsConstructor;
import net.sf.json.JSONObject;

/**
 * @author zhangjia
 * @date 2023-01-11 20:11
 */
@NoArgsConstructor
public class Compl2Obj implements ObjToJson{
    @Override
    public String Obj2json(Object obj) {
        JSONObject object= JSONObject.fromObject((Complain)obj);
        return object.toString();
    }

    @Override
    public Object Json2obj(String str) {
        JSONObject jsonobject = JSONObject.fromObject(str);
        return  JSONObject.toBean(jsonobject,Complain.class);
    }
}
