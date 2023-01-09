package com.uestc.thresholddkg.Server.pojo;

import lombok.*;

import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-01-09 20:27
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DKG_System {
    private BigInteger p;
    private BigInteger q;
    private BigInteger g;
    private BigInteger h;
}
