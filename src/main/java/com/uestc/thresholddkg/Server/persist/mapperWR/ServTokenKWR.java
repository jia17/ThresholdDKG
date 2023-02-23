package com.uestc.thresholddkg.Server.persist.mapperWR;

import com.uestc.thresholddkg.Server.persist.mapper.ServPrfsMapper;
import com.uestc.thresholddkg.Server.persist.mapper.ServTokenKMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author zhangjia
 * @date 2023-02-21 13:19
 */
@Component
public class ServTokenKWR {
    private ServTokenKMapper servTokenKMapper1;

    private static ServTokenKMapper servTokenKMapper;

    @Autowired
    public ServTokenKWR(ServTokenKMapper servTokenKMapper1) {
        this.servTokenKMapper1 = servTokenKMapper1;
    }

    @PostConstruct
    public void get(){servTokenKMapper=servTokenKMapper1;}

    public static ServTokenKMapper getMapper(){return servTokenKMapper;}
}
