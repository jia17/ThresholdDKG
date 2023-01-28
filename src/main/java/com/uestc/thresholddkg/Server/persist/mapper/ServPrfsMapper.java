package com.uestc.thresholddkg.Server.persist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.thresholddkg.Server.persist.ServPrfs;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
public interface ServPrfsMapper extends BaseMapper<ServPrfs> {
    @Select("SELECT * FROM serv_prfs WHERE servId=#{ServId} AND userId=#{userName}" )
    ServPrfs selectByIdAndF(int ServId,String userName);
}
