package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author zhangjia
 * @date 2023-03-21 12:51
 */
@AllArgsConstructor
@Setter
@Getter
public class userMsgRedis implements Serializable {
    String msg;
    String msgHash;
    String msgTime;
}
