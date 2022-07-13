package com.xu1an.mrapp;

import com.xu1an.common.KeyValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WordCount implements IMapReduce{
    public List<KeyValue> map(String contents) {
        List<String> words = Arrays.stream(contents.split("[^a-zA-Z]+"))
                                   .filter(word -> !"".equals(word))
                                   .collect(Collectors.toList());
        List<KeyValue> keyValues = new ArrayList<>(words.size());
        for (String word : words) {
            KeyValue keyValue = new KeyValue(word, "1");
            keyValues.add(keyValue);
        }
        return keyValues;
    }

    public String reduce(List<String> values) {
        return String.valueOf(values.size());
    }
}
