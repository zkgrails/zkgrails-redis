package zkgrails;

import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.util.ExecutionInit;
import org.zkoss.zk.ui.util.ExecutionCleanup;

import java.util.List;

import javax.servlet.ServletContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.persistence.FlushModeType;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.core.DatastoreUtils;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.transactions.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;

public class OpenSessionInViewListener implements ExecutionInit, ExecutionCleanup {

    private Datastore datastore=null;

    public void setDatastore(Datastore datastore) {
        this.datastore = datastore;
    }

    public Datastore getDatastore(Execution ex) {
        if(datastore == null) {
       ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(
            (ServletContext)ex.getDesktop().getWebApp().getNativeContext()
       );
       datastore = (Datastore)ctx.getBean("springDatastore");
       }
       return datastore;
    }

    public void init(Execution exec, Execution parent) {
        if(parent != null) return;
        if(hasSessionBound(exec)) return;

        Session session = DatastoreUtils.getSession(getDatastore(exec), true);
        session.setFlushMode(FlushModeType.AUTO);
        if(!hasSessionBound(exec)) {
            TransactionSynchronizationManager.bindResource(
                this.datastore,
                new SessionHolder(session)
            );
        }
    }

    protected boolean hasSessionBound(Execution ex) {
        return TransactionSynchronizationManager.getResource(getDatastore(ex)) != null;
    }

    public void cleanup(Execution exec, Execution parent, List errs) {
        if(parent != null) return;
        if(errs != null && !errs.isEmpty()) return;

        if(hasSessionBound(exec)) {
            // single session mode
            SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.unbindResource(getDatastore(exec));
            DatastoreUtils.closeSession(sessionHolder.getSession());
        }
    }
}

