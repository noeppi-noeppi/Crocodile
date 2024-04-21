package crocodile.db;

import javax.annotation.Nullable;

public record DatabaseConfig(String host, int port, @Nullable String user, @Nullable String password) {
}
