package com.uestc.thresholddkg.Server.persist;

import com.baomidou.mybatisplus.annotation.TableField;
import com.github.jeffreyning.mybatisplus.anno.MppMultiId;
import lombok.Builder;
import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;

/**
 * @author zhangjia
 * @date 2023-01-26 21:44
 */
@Data
@Builder
public class ServPrfs implements Serializable {
    @MppMultiId
    @Column(nullable = false )
    @TableField(value="servId")
    private Integer servId;
    @MppMultiId
    @Column(nullable = false )
    @TableField(value="userId")
    private String userId;

    private String priKeyi;
    private String prfi;
    private String verify;

}
