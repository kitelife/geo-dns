package cn.xiayf.code.exception;

public class BadReqException extends Exception {

    public BadReqException() {
        super();
    }
    public BadReqException(String msg) {
        super(msg);
    }
}