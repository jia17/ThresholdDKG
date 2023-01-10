package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-01-10 23:35
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DKG_SysStr {
    private String p;
    private String q;
    private String g;
    private String h;
}
