package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author zhangjia
 * @date 2023-01-11 20:10
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Complain {
    private Set<String> compls;
    private String userId;
    private String sendAddr;
}
