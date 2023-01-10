package com.uestc.thresholddkg.Server.util;

import com.uestc.thresholddkg.Server.pojo.DkgSysMsg;
import com.uestc.thresholddkg.Server.pojo.FunctionGHvals;
import lombok.NoArgsConstructor;
import net.sf.json.JSONObject;

/**
 * @author zhangjia
 * @date 2023-01-10 15:40
 */
@NoArgsConstructor
public class FuncGH2Obj implements ObjToJson{
    @Override
    public String Obj2json(Object obj) {
        JSONObject object= JSONObject.fromObject((FunctionGHvals)obj);
        return object.toString();
    }

    @Override
    public Object Json2obj(String str) {
        JSONObject jsonobject = JSONObject.fromObject(str);
        return  JSONObject.toBean(jsonobject, FunctionGHvals.class);
    }
}
