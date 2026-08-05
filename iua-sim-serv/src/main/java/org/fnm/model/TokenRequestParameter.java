package org.fnm.model;

import org.fnm.helper.GrantType;
import org.jboss.logging.Logger;

public class TokenRequestParameter {

    private static final Logger LOG = Logger.getLogger(TokenRequestParameter.class);

    // common
    public String grantType; // mandatory
    public String clientId; // mandatory
    public String clientSecret; // mandatory

    // CC Flow
    public String scope;
    public String principal; // optional
    public String principalId; // mandatory for CC Flow
    public String personId; // optional. Indicates the token as a basic or an extended token.

    // AC Flow
    public String code; // mandatory
    public String clientAssertion; // mandatory
    public String clientAssertionType; // mandatory


    /**
     * @return true, if personId is set indicating a extended token
     */
    public boolean isBasic(){
        return isComplete() && personId == null;
    }

    /**
     * @return true, if all required fields are set. The required fields depend on the grant type.
     */
    public boolean isComplete() {

        if (grantType == null || grantType.isEmpty()) {
            return false;
        }

        if (grantType.equals(GrantType.clientCredentials)) {
            return (clientId != null && !clientId.isEmpty() &&
                    clientSecret != null && !clientSecret.isEmpty() &&
                    principalId != null && !principalId.isEmpty() &&
                    scope != null && !scope.isEmpty());
        }

        if (grantType.equals(GrantType.authorizationCode)) {
            return (clientId != null && !clientId.isEmpty() &&
                    clientSecret != null && !clientSecret.isEmpty() &&
                    code != null && !code.isEmpty() &&
                    clientAssertion != null && !clientAssertion.isEmpty() &&
                    clientAssertionType != null && !clientAssertionType.isEmpty());
        }

        return false;
    }
}
