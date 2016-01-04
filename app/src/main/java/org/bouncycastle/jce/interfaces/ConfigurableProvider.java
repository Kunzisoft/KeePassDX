package org.bouncycastle.jce.interfaces;

/**
 * Implemented by the BC provider. This allows setting of hidden parameters,
 * such as the ImplicitCA parameters from X.962, if used.
 */
public interface ConfigurableProvider
{
    static final String      THREAD_LOCAL_EC_IMPLICITLY_CA = "threadLocalEcImplicitlyCa";   
    static final String      EC_IMPLICITLY_CA = "ecImplicitlyCa";

    void setParameter(String parameterName, Object parameter);
}
