package com.xu1an.message;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Xu1Aan
 * @Date: 2022/07/13/10:57
 * @Description:
 */
@Data
public class TestMessage extends Message{
    private String message;
    @Override
    public int getMessageType() {
        return 7;
    }

    public TestMessage(String message) {
        this.message = message;
    }
}
