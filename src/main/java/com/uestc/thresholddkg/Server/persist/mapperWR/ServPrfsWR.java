package com.uestc.thresholddkg.Server.persist.mapperWR;

import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.jnlp.ServiceManager;

/**
 * @author zhangjia
 * @date 2023-01-26 21:54
 */
@Component
@NoArgsConstructor
@Getter
public class ServPrfsWR {
    @Autowired
    private ServPrfsMapper servPrfsMapper1;

    private static ServPrfsMapper servPrfsMapper;

    @PostConstruct
    public void get(){servPrfsMapper=servPrfsMapper1;}

    public static ServPrfsMapper getMapper(){return servPrfsMapper;}
}
