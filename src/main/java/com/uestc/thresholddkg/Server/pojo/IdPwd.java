package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author zhangjia
 * @date 2023-02-11 22:32
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdPwd implements Serializable {
    String id;
    String passwd;
}

