package com.uestc.thresholddkg.Server.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

/**
 * @author zhangjia
 * @date 2023-01-02 10:26
 */
@Getter
@Setter
@Configuration
//@ConfigurationProperties(prefix = "idpservers.ipandport")
public class IpAndPort {
    @Value("#{'${idpservers.ipandport.ServersIp}'.split(' ')}")
    private String[] ServersIp;
}
