package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjia
 * @date 2023-01-12 23:17
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Secreti {
    private String addr;
    private String secret;
}
