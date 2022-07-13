package com.xu1an.message;

import lombok.Data;
import lombok.ToString;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Xu1Aan
 * @Date: 2022/07/12/19:24
 * @Description:
 */
@Data
@ToString(callSuper = true)
public class RequestTaskMessage extends Message{

    private String WorkerName;

    private int status;

    @Override
    public int getMessageType() {
        return TASK_REQUEST;
    }
}
