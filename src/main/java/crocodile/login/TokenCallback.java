package crocodile.login;

import javax.annotation.Nullable;
import javax.security.auth.callback.Callback;
import java.io.Serializable;

public class TokenCallback implements Callback, Serializable {
    
    @Nullable private String token;
    
    public TokenCallback() {
        this.token = null;
    }
    
    public void setToken(@Nullable String token) {
        this.token = token;
    }
    
    @Nullable
    public String getToken() {
        return this.token;
    }
}
