package com.uestc.thresholddkg.Server.persist;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.github.jeffreyning.mybatisplus.anno.MppMultiId;
import lombok.Builder;
import lombok.Data;

import javax.persistence.Column;

/**
 * @author zhangjia
 * @date 2023-01-26 22:53
 */
@Data
@Builder
public class ServPrfsPp {
    @TableId
    private String userId;

    private String p;
    private String q;
    private String g;
    private String h;
}
