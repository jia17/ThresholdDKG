package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjia
 * @date 2023-02-04 19:42
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TokenSi {
    private String Wi;
    private String Ui;
}
