package zkgrails.redis;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.persistence.FlushModeType;

import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.core.DatastoreUtils;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.transactions.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;

public class OpenSessionInViewFilter extends OncePerRequestFilter {

    public static final String DEFAILT_DATASTORE_BEAN_NAME = "redisDatastore";

    private String springDatastoreBeanName = DEFAILT_DATASTORE_BEAN_NAME;

    private boolean singleSession = true;
    private FlushModeType flushMode = FlushModeType.AUTO;

    /**
     * Set whether to use a single session for each request. Default is "true".
     * <p>If set to "false", each data access operation or transaction will use
     * its own session (like without Open Session in View). Each of those
     * sessions will be registered for deferred close, though, actually
     * processed at request completion.
     * @see SessionFactoryUtils#initDeferredClose
     * @see SessionFactoryUtils#processDeferredClose
     */
    public void setSingleSession(boolean singleSession) {
        this.singleSession = singleSession;
    }

    /**
     * Return whether to use a single session for each request.
     */
    protected boolean isSingleSession() {
        return this.singleSession;
    }

    public void setFlushMode(FlushModeType flushMode) {
        this.flushMode = flushMode;
    }

    protected FlushModeType getFlushMode() {
        return this.flushMode;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Datastore datastore = lookupDatastore(request);
        boolean participate = false;

        if (isSingleSession()) {
            // single session mode
            if (TransactionSynchronizationManager.getResource(datastore) != null) {
                // Do not modify the Session: just set the participate flag.
                participate = true;
            } else {
                logger.debug("Opening single Spring datastore in OpenSessionInViewFilter");
                Session session = getSession(datastore);
                TransactionSynchronizationManager.unbindResourceIfPossible(datastore);
                TransactionSynchronizationManager.bindResource(datastore, new SessionHolder(session));
            }
        }
    
        // else {
            // deferred close mode
            //if (DatastoreUtils.isDeferredCloseActive(sessionFactory)) {
                // Do not modify deferred close: just set the participate flag.
                // participate = true;
            //}
            //else {
                // DatastoreUtils.initDeferredClose(datastore);
            //}
        // }

        try {
            filterChain.doFilter(request, response);
        }
        finally {
            if (!participate) {
                if (isSingleSession()) {
                    // single session mode
                    SessionHolder sessionHolder =
                            (SessionHolder) TransactionSynchronizationManager.unbindResource(datastore);
                    logger.debug("Closing single Datastore Session in OpenSessionInViewFilter");
                    closeSession(sessionHolder.getSession(), datastore);
                }
                else {
                    // deferred close mode
                    DatastoreUtils.processDeferredClose(datastore);
                }
            }
        }
    }

    /**
     * Look up the SessionFactory that this filter should use,
     * taking the current HTTP request as argument.
     * <p>The default implementation delegates to the {@link #lookupSessionFactory()}
     * variant without arguments.
     * @param request the current request
     * @return the SessionFactory to use
     */
    protected Datastore lookupDatastore(HttpServletRequest request) {
        return lookupDatastore();
    }

    /**
     * Look up the SessionFactory that this filter should use.
     * <p>The default implementation looks for a bean with the specified name
     * in Spring's root application context.
     * @return the SessionFactory to use
     * @see #getSessionFactoryBeanName
     */
    protected Datastore lookupDatastore() {
        if (logger.isDebugEnabled()) {
            logger.debug("Using Datastore '" + this.springDatastoreBeanName + "' for OpenSessionInViewFilter");
        }
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        return wac.getBean(this.springDatastoreBeanName, Datastore.class);
    }


    protected Session getSession(Datastore datastore) {
        Session session = DatastoreUtils.getSession(datastore, true);
        FlushModeType flushMode = getFlushMode();
        if (flushMode != null) {
            session.setFlushMode(flushMode);
        }
        return session;
    }

    protected void closeSession(Session session, Datastore datastore) {
        DatastoreUtils.closeSession(session);
    }

}
