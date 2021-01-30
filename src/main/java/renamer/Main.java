package renamer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private static String prefix = "";
    private static String extension = null;
    private static int begin = 1;
    private static int width = 4;
    private static Path directory = null;

    public static void main(String[] args) {
        List<String> options = List.of(args);
        if (options.contains("-h") || options.contains("--help")) {
            printHelp();
        }

        try {
            initOptions(options);
        } catch (InvalidOptionException e) {
            System.err.println(e.getMessage());
            return;
        }

        List<File> files = Stream.of(directory.toFile().listFiles())
                .filter(File::isFile)
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            System.err.println("対象ディレクトリ内に、リネーム対象となるファイルは存在しません");
            return;
        }

        String message = "以下の条件で" + files.size() + "件のファイルをリネームします。\n" +
                "プレフィックス : " + prefix + "\n" +
                "拡張子 : " + (extension == null ? "元ファイルのまま" : extension) + "\n" +
                "開始連番 : " + begin + "\n" +
                "連番桁数 : " + width + "\n" +
                "対象ディレクトリ : " + directory + "\n" +
                "\n" +
                "よろしいですか？(y/n) > ";
        String line = System.console().readLine(message);
        if (line.equals("y")) {
            rename(files);
        }
    }

    private static void rename(List<File> files) {
        String format = prefix + "%0" + width + "d";
        int sequence = begin;
        int counter = 0;

        for (File file : files) {
            String newName;
            if (extension != null) {
                newName = String.format(format + "." + extension, sequence);
            } else {
                int lastIndex = file.getName().lastIndexOf(".");
                String originalExtension = file.getName().substring(lastIndex);
                newName = String.format(format + originalExtension, sequence);
            }

            Path dist = directory.resolve(newName);
            try {
                Files.move(file.toPath(), dist);
                System.out.println("rename from " + file.getName() + " to " + dist.toFile().getName());
            } catch (IOException e) {
                throw new UncheckedIOException("リネームに失敗しました file=" + file, e);
            }

            sequence++;
            counter++;
        }

        System.out.println(counter + "件リネームしました");
    }

    private static void initOptions(List<String> options) {
        Iterator<String> ite = options.iterator();
        while (ite.hasNext()) {
            String option = ite.next();
            if (option.equals("-p") || option.contains("--prefix")) {
                if (!ite.hasNext()) {
                    throw new InvalidOptionException("プレフィックスの値が指定されていません");
                }
                prefix = ite.next();
            } else if (option.equals("-e") || option.equals("--extension")) {
                if (!ite.hasNext()) {
                    throw new InvalidOptionException("拡張子の値が指定されていません");
                }
                extension = ite.next();
            } else if (option.equals("-b") || option.equals("--begin")) {
                if (!ite.hasNext()) {
                    throw new InvalidOptionException("開始番号の値が指定されていません");
                }
                String strBegin = ite.next();
                try {
                    begin = Integer.parseInt(strBegin);
                } catch (NumberFormatException e) {
                    throw new InvalidOptionException("開始番号は正数で指定してください begin=" + strBegin);
                }
                if (begin < 1) {
                    throw new InvalidOptionException("開始番号は1以上を指定してください begin=" + begin);
                }
            } else if (option.equals("-w") || option.equals("--width")) {
                if (!ite.hasNext()) {
                    throw new InvalidOptionException("連番桁数の値が指定されていません");
                }
                String strWidth = ite.next();
                try {
                    width = Integer.parseInt(strWidth);
                } catch (NumberFormatException e) {
                    throw new InvalidOptionException("連番桁数は正数で指定してください begin=" + strWidth);
                }
                if (width < 1) {
                    throw new InvalidOptionException("連番桁数は1以上を指定してください begin=" + begin);
                }
            } else if (option.startsWith("-")) {
                throw new InvalidOptionException("不明なオプションです option=" + option);
            } else {
                directory = Path.of(option);
                break;
            }
        }

        if (directory == null) {
            throw new InvalidOptionException("処理対象のディレクトリが指定されていません");
        }
        if (!Files.exists(directory)) {
            throw new InvalidOptionException("指定されたディレクトリは存在しません directory=" + directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new InvalidOptionException("指定されたパスはディレクトリではありません directory=" + directory);
        }
    }

    private static void printHelp() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Main.class.getResourceAsStream("/help.txt").transferTo(out);
            String text = new String(out.toByteArray(), StandardCharsets.UTF_8);
            System.out.println(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
