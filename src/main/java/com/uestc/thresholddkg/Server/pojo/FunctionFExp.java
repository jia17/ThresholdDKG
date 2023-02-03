package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjia
 * @date 2023-02-03 21:24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionFExp {
    private String sendAddr;// "/172.168.."
    private String[] fExp;
    private String userId;
    private Integer serverId;//[1-n]
}
