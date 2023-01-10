package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-01-10 11:45
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DkgSysMsg {
    private DKG_SysStr dkg_sysStr;
    private String Id;
    private String passwd;
    private Integer item;
}
