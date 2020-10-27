package ru.mail.polis.service.boriskin;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;

final class Node {

    private final long token;

    @NotNull
    private String address;

    Node(final long token,
         @NotNull final String address) {
        this.token = token;
        this.address = address;
    }

    public long getToken() {
        return token;
    }

    @NotNull
    public String getAddress() {
        return address;
    }

    void setAddress(
            @NotNull final String address) {
        this.address = address;
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, address);
    }

    @Override
    public boolean equals(
            @NotNull final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Node)) {
            return false;
        }
        final Node vNode = (Node)obj;
        return vNode.token == token && vNode.address.equals(address);
    }
}
