package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjia
 * @date 2023-02-04 16:04
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TestUserMsg {
    String userId;
    String pwd;
    String email;
    String phone;
    Integer num;
    Boolean man;
}
