package com.xu1an.config;

import com.xu1an.protocol.Serializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Xu1Aan
 * @Date: 2022/07/12/14:32
 * @Description:
 */
public abstract class Config {
    static Properties properties;
    static {
        try (InputStream in = Config.class.getResourceAsStream("/application.properties")) {
            properties = new Properties();
            properties.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    public static int getServerPort() {
        String value = properties.getProperty("server.port");
        if(value == null) {
            return 8080;
        } else {
            return Integer.parseInt(value);
        }
    }
    public static Serializer.Algorithm getSerializerAlgorithm() {
        String value = properties.getProperty("serializer.algorithm");
        if(value == null) {
            return Serializer.Algorithm.Java;
        } else {
            return Serializer.Algorithm.valueOf(value);
        }
    }
    public static int getMapNums() {
        String value = properties.getProperty("map.nums");
        if(value == null) {
            return 1;
        } else {
            return Integer.parseInt(value);
        }
    }
    public static int getMaxFrameLength() {
        String value = properties.getProperty("netty.maxFrameLength");
        if(value == null) {
            return 1024;
        } else {
            return Integer.parseInt(value);
        }
    }
}
