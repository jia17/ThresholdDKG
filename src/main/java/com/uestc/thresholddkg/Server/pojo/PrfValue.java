package com.uestc.thresholddkg.Server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjia
 * @date 2023-01-25 17:15
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PrfValue {
    private String Id;
    private String PrfI;
    private String Cveri;
}
