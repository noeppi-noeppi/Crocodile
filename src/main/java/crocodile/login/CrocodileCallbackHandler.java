package crocodile.login;

import javax.annotation.Nullable;
import javax.security.auth.callback.*;

public class CrocodileCallbackHandler implements CallbackHandler {
    
    @Nullable private final String user;
    @Nullable private final String password;
    @Nullable private final String token;

    public CrocodileCallbackHandler(@Nullable String user, @Nullable String password, @Nullable String token) {
        this.user = user;
        this.password = password;
        this.token = token;
    }

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            switch (callback) {
                case NameCallback nameCallback when this.user != null -> nameCallback.setName(this.user);
                case PasswordCallback passwordCallback when this.password != null -> passwordCallback.setPassword(this.password.toCharArray());
                case TokenCallback tokenCallback when this.token != null -> tokenCallback.setToken(this.token);
                default -> throw new UnsupportedCallbackException(callback);
            }
        }
    }
}
