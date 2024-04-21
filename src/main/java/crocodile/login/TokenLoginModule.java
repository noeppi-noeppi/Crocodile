package crocodile.login;

import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class TokenLoginModule implements LoginModule {
    
    @Nullable private CallbackHandler callbackHandler = null;
    @Nullable private String token = null;
    
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.callbackHandler = callbackHandler;
        if (options.containsKey("token") && options.get("token") instanceof String tokenString) {
            this.token = tokenString;
        } else {
            this.token = null;
        }
    }

    @Override
    public boolean login() throws LoginException {
        if (this.callbackHandler == null) {
            throw new LoginException("login configuration error");
        }
        TokenCallback callback = new TokenCallback();
        try {
            this.callbackHandler.handle(new Callback[]{ callback });
        } catch (IOException | UnsupportedCallbackException e) {
            //
        }
        if (this.token == null || callback.getToken() == null || !Objects.equals(this.token, callback.getToken())) {
            throw new LoginException("invalid credentials");
        }
        return true;
    }

    @Override
    public boolean commit() {
        return true;
    }

    @Override
    public boolean abort() {
        return true;
    }

    @Override
    public boolean logout() {
        return true;
    }
}
