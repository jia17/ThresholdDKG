package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjia
 * @date 2023-03-26 15:04
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IdPwdChange {
    String id;
    String passwd;
    private String newPwd;
}
