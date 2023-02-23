package com.uestc.thresholddkg.Server.persist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.thresholddkg.Server.persist.ServTokenKey;
import org.springframework.stereotype.Repository;

@Repository
public interface ServTokenKMapper extends BaseMapper<ServTokenKey> {
}
