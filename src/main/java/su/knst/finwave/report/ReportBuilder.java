package su.knst.finwave.report;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.Record;
import su.knst.finwave.api.report.ReportDatabase;
import su.knst.finwave.api.report.data.ReportStatus;
import su.knst.finwave.api.report.data.ReportType;
import su.knst.finwave.api.transaction.TransactionDatabase;
import su.knst.finwave.api.transaction.filter.TransactionsFilter;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.general.ReportBuilderConfig;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.jooq.tables.records.ReportsRecord;
import su.knst.finwave.report.builders.AbstractReportBuilder;
import su.knst.finwave.report.builders.ByDaysReportBuilder;
import su.knst.finwave.report.builders.ByMonthsReportBuilder;
import su.knst.finwave.report.builders.ListReportBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static su.knst.finwave.api.ApiResponse.GSON;

@Singleton
public class ReportBuilder {
    protected TransactionDatabase transactionDatabase;
    protected ReportDatabase reportDatabase;
    protected ReportBuilderConfig config;
    protected DatabaseWorker worker;
    protected ExecutorService executor;

    @Inject
    public ReportBuilder(DatabaseWorker worker, Configs configs) {
        this.worker = worker;
        this.transactionDatabase = worker.get(TransactionDatabase.class);
        this.reportDatabase = worker.get(ReportDatabase.class);

        this.config = configs.getState(new ReportBuilderConfig());
        this.executor = Executors.newFixedThreadPool(config.threads);
    }

    public Future<?> buildAsync(String token) {
        return executor.submit(() -> {
            ReportStatus status = ReportStatus.FAILED;

            Optional<ReportsRecord> recordOptional = reportDatabase.getReport(token);

            if (recordOptional.isEmpty())
                return;

            ReportsRecord record = recordOptional.get();

            try {
                status = buildAndSave(record);
            } catch (Exception e) {
                e.printStackTrace();
            }

            reportDatabase.updateReport(token, status);
        });
    }

    protected ReportStatus buildAndSave(ReportsRecord record) throws IOException {
        int userId = record.getUserId();
        TransactionsFilter filter = GSON.fromJson(record.getFilter().data(), TransactionsFilter.class);

        if (filter == null)
            filter = TransactionsFilter.EMPTY;

        int offset = 0;
        int count = config.maxTransactionsPerCycle;

        AbstractReportBuilder builder = get(record);

        while (true) {
            List<Record> transactions = transactionDatabase.getTransactions(userId, offset, count, filter);

            if (transactions.isEmpty())
                break;

            offset += transactions.size();

            builder.consider(transactions);
        }

        builder.done();

        return ReportStatus.AVAILABLE;
    }

    protected AbstractReportBuilder get(ReportsRecord reportsRecord) throws IOException {
        ReportType type = ReportType.values()[reportsRecord.getType()];

        switch (type) {
            case BY_DAYS -> {
                return new ByDaysReportBuilder(reportsRecord, worker);
            }
            case BY_MONTHS -> {
                return new ByMonthsReportBuilder(reportsRecord, worker);
            }
            default -> {
                return new ListReportBuilder(reportsRecord, worker);
            }
        }
    }
}
