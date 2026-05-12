package com.bankmgmt.utils;

import com.bankmgmt.model.BankTransaction;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/** CSV export for filtered transaction grids (audit / resume artifact). */
public final class TransactionReportExporter {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TransactionReportExporter() {
    }

    public static void exportCsv(Path file, java.util.List<BankTransaction> rows) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("id,receipt_ref,type,amount,created_at,description\n");
            for (BankTransaction r : rows) {
                w.write(String.valueOf(r.getId()));
                w.write(',');
                w.write(csvEscape(r.getReceiptRef()));
                w.write(',');
                w.write(r.getTransactionType().name());
                w.write(',');
                w.write(r.getAmount().toPlainString());
                w.write(',');
                w.write(csvEscape(r.getCreatedAt().format(FMT)));
                w.write(',');
                w.write(csvEscape(r.getDescription() == null ? "" : r.getDescription()));
                w.write('\n');
            }
        }
    }

    private static String csvEscape(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
