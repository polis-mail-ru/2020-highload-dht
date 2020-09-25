package ru.mail.polis.service.art241111.codes;

public enum CommandsCode {
    ERR_STATUS (404),
    EMPTY_ID (400),
    GOOD_STATUS (200),
    OFFLINE_STATUS (503),
    DATA_IS_UPSET (201),
    DELETE_IS_GOOD (202),
    METHOD_NOT_ALLOWED (400);

    private final int code;

    CommandsCode(int code){
        this.code = code;
    }

    public int getCode(){
        return code;
    }
}
