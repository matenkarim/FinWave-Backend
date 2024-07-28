package app.finwave.backend.api.session;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.UserConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.ParamsValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static app.finwave.backend.utils.TokenGenerator.generateSessionToken;

@Singleton
public class SessionApi {
    protected SessionDatabase database;
    protected UserConfig config;

    @Inject
    public SessionApi(DatabaseWorker databaseWorker, Configs configs) {
        this.database = databaseWorker.get(SessionDatabase.class);
        this.config = configs.getState(new UserConfig());
    }

    public Object newSession(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        if (sessionRecord.getLimited()) {
            response.status(403);

            return ApiMessage.of("This session is limited");
        }

        int lifetimeDays = ParamsValidator
                .integer(request, "lifetimeDays")
                .range(1, Integer.MAX_VALUE)
                .require();

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxSessionDescriptionLength)
                .optional();

        String token = generateSessionToken();

        database.newSession(sessionRecord.getUserId(), token, lifetimeDays, description.orElse(null), true);

        response.status(200);

        return new NewSessionResponse(token);
    }

    public Object getSessions(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        List<UsersSessionsRecord> records = database.getUserSessions(sessionRecord.getUserId());

        response.status(200);

        return new GetSessionsResponse(records, sessionRecord.getId());
    }

    public Object deleteSession(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        if (sessionRecord.getLimited()) {
            response.status(403);

            return ApiMessage.of("This session is limited");
        }

        long sessionId = ParamsValidator
                .integer(request, "sessionId")
                .matches((id) -> database.userOwnSession(sessionRecord.getUserId(), id))
                .require();

        database.deleteSession(sessionId);

        response.status(200);

        return ApiMessage.of("Session deleted");
    }

    static class GetSessionsResponse extends ApiResponse {
        public final List<Entry> sessions;
        public final long currentId;

        public GetSessionsResponse(List<UsersSessionsRecord> sessions, long currentId) {
            this.sessions = sessions.stream()
                    .map(r -> new Entry(
                            r.getId(),
                            r.getLimited(),
                            r.getCreatedAt(),
                            r.getExpiresAt(),
                            r.getDescription()))
                    .toList();

            this.currentId = currentId;
        }

        record Entry(long sessionId, boolean limited, LocalDateTime createdAt, LocalDateTime expiresAt, String description) {}
    }

    static class NewSessionResponse extends ApiResponse {
        public final String token;

        public NewSessionResponse(String token) {
            this.token = token;
        }
    }
}
