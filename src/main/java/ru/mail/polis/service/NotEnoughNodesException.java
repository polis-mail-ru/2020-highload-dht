package ru.mail.polis.service;

@SuppressWarnings("serial")
class NotEnoughNodesException extends Exception {
    NotEnoughNodesException(final String errorMessage) {
        super(errorMessage);
    }
}
