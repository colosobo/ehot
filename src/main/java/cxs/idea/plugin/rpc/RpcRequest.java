package cxs.idea.plugin.rpc;

import java.io.Serializable;

/**
 * @author gwk_2
 * @date 2022/5/12 16:10
 */
public class RpcRequest <T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;

    private T data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
