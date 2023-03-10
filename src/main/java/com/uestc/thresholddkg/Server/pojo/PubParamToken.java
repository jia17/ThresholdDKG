package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjia
 * @date 2023-02-04 16:32
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PubParamToken {
    private String userId;
    private DKG_SysStr dkg_sysStr;
    private String y;
}
