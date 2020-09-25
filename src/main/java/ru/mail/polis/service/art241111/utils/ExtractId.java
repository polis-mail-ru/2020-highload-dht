package ru.mail.polis.service.art241111.utils;

public class ExtractId {
    /**
     * Method for getting the id value from a string.
     * @param query - String to get the id from.
     * @return id.
     * @throws IllegalArgumentException - If there are problems with processing the string.
     */
    public String extractId(final String query) throws IllegalArgumentException {
        if (query == null) {
            throw new IllegalArgumentException("Id is empty");
        } else {
            final String PREFIX = "id=";

            if (!query.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Id not set");
            }

            final String id = query.substring(PREFIX.length());

            if (id.isEmpty()) {
                throw new IllegalArgumentException("Id is empty");
            }

            return id;
        }
    }
}
