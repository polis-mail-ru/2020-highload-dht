package ru.mail.polis.service;

@SuppressWarnings("serial")
class NotEnoughNodesException extends Exception {
    NotEnoughNodesException(String errorMessage) {
        super(errorMessage);
    }
}
