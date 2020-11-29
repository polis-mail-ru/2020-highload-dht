package ru.mail.polis.dao.stasyanoi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws IOException {
        List<String> collect = Stream.iterate(0, i -> ++i)
                .limit(500_000)
                .map(integer -> "v0/entities?start=" + integer+"&end=" + (integer +  (int) (Math.random() * 1000)))
                .collect(Collectors.toList());

        Files.write(Path.of("trace_scan.txt"), collect);
    }
}
