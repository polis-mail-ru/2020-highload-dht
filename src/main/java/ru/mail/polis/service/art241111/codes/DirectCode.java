package ru.mail.polis.service.art241111.codes;

public enum DirectCode {
    STATUS ("/v0/status"),
    ENTITY ("/v0/entity");

    private final String code;

    DirectCode(String code){
        this.code = code;
    }

    public String getCode(){
        return code;
    }
}
