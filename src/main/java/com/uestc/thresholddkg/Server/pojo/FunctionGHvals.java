package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-01-10 15:25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionGHvals {
    private String sendAddr;// "/172.168.."
    private BigInteger Fi;
    private BigInteger Gi;
    private BigInteger[] gMulsH;
    private String userId;
    private Integer serverId;//[1-n]
    private Integer item;
}
