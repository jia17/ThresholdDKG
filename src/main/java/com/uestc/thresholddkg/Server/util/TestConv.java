package com.uestc.thresholddkg.Server.util;

import lombok.*;

import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-01-02 23:32
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TestConv {
    BigInteger pri;
    BigInteger pub;
    String[]  text;
    int id;
    boolean flag;
}
