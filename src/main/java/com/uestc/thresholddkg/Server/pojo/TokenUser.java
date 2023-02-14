package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjia
 * @date 2023-02-11 16:28
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TokenUser {
    String user;
    String sign;
    String y;
}
