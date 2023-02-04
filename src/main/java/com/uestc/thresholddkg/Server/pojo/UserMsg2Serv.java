package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjia
 * @date 2023-02-04 16:43
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserMsg2Serv {
    String userId;
    String msg;
    String msgHash;
    Integer[] addrIndex;
}
