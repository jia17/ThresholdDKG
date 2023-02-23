package com.uestc.thresholddkg.Server.persist;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Builder;
import lombok.Data;

/**
 * @author zhangjia
 * @date 2023-02-21 13:16
 */
@Data
@Builder
public class ServTokenKey {
    @TableId
    Integer servId;

    String priKeyi;
    String pubKey;
}
