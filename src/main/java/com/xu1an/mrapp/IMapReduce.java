package com.xu1an.mrapp;

import com.xu1an.common.KeyValue;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Xu1Aan
 * @Date: 2022/07/11/18:45
 * @Description:
 */
public interface IMapReduce {
    List<KeyValue> map(String contents);

    String reduce(List<String> values);
}
