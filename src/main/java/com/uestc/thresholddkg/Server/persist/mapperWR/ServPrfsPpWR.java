package com.uestc.thresholddkg.Server.persist.mapperWR;

import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsPPMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author zhangjia
 * @date 2023-01-26 22:55
 */
@Component
@NoArgsConstructor
@Getter
public class ServPrfsPpWR {
    @Autowired
    private ServPrfsPPMapper servPrfsPPMapper1;

    private static ServPrfsPPMapper servPrfsPPMapper;

    @PostConstruct
    public void get(){servPrfsPPMapper=servPrfsPPMapper1;}

    public static ServPrfsPPMapper getMapper(){return servPrfsPPMapper;}
}
