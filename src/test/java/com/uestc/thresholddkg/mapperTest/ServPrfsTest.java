package com.uestc.thresholddkg.mapperTest;

import com.uestc.thresholddkg.Server.persist.ServPrfs;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsPPMapper;
import com.uestc.thresholddkg.Server.persist.mapperWR.ServPrfsWR;
import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsMapper;
import com.uestc.thresholddkg.Server.pojo.DKG_System;
import lombok.var;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;

/**
 * @author zhangjia
 * @date 2023-01-26 22:13
 */
@SpringBootTest
public class ServPrfsTest {
    @Autowired
    ServPrfsMapper servPrfsMapper;

    @Autowired
    ServPrfsPPMapper servPrfsPPMapper;

    @Test
    void addAndSelect(){
    ServPrfsMapper servPrfsMapper1= ServPrfsWR.getMapper();
    servPrfsPPMapper.selectById("sa");
    servPrfsMapper1.insert(ServPrfs.builder().servId(2332).userId("sas").prfi("sssssssss").build());
    servPrfsMapper.selectByIdAndF(22,"sas");
    var servPrfsPp=servPrfsPPMapper.selectById("alice");
    var param0=new DKG_System(new BigInteger(servPrfsPp.getP()),new BigInteger(servPrfsPp.getQ()),
                new BigInteger(servPrfsPp.getG()),new BigInteger(servPrfsPp.getH()));
        System.out.println(param0);
    }
}
