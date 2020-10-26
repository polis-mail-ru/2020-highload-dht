package ru.mail.polis.s3ponia;

import com.google.common.base.Splitter;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

public class Utility {
    
    public static class Header {
        public final String key;
        public final String value;
        
        public Header(@NotNull final String key, @NotNull final String value) {
            this.key = key;
            this.value = value;
        }
        
        public static Header getHeader(@NotNull final String key, @NotNull final Request response) {
            final var headers = response.getHeaders();
            final var headerCount = response.getHeaderCount();
            int keyLength = key.length();
            
            for (int i = 1; i < headerCount; ++i) {
                if (headers[i].regionMatches(true, 0, key, 0, keyLength)) {
                    final var value = headers[i].substring(headers[i].indexOf(':') + 1).stripLeading();
                    return new Header(headers[i], value);
                }
            }
            
            return null;
        }
        
        public static Header getHeader(@NotNull final String key, @NotNull final Response response) {
            final var headers = response.getHeaders();
            final var headerCount = response.getHeaderCount();
            int keyLength = key.length();
            
            for (int i = 1; i < headerCount; ++i) {
                if (headers[i].regionMatches(true, 0, key, 0, keyLength)) {
                    final var value = headers[i].substring(headers[i].indexOf(':') + 1).stripLeading();
                    return new Header(headers[i], value);
                }
            }
            
            return null;
        }
    }
    
    public static class ReplicationConfiguration {
        public final int ack;
        public final int from;
        
        public ReplicationConfiguration(final int ack, final int from) {
            this.ack = ack;
            this.from = from;
        }
        
        public static ReplicationConfiguration parse(@NotNull final String s) {
            final var splitStrings = Splitter.on('/').splitToList(s);
            if (splitStrings.size() != 2) {
                return null;
            }
            
            try {
                return new ReplicationConfiguration(Integer.parseInt(splitStrings.get(0)),
                        Integer.parseInt(splitStrings.get(1)));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
