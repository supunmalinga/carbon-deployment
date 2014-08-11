package org.wso2.carbon.webapp.mgt.ext;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;

/**
 * Created with IntelliJ IDEA.
 * User: sagara
 * Date: 5/20/14
 * Time: 10:01 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CarbonLifecycleListenerBase  implements LifecycleListener {
    @Override
    public void lifecycleEvent(LifecycleEvent lifecycleEvent) {

//        System.out.println(" >>>>>>>>>>>>>>> CarbonLifecycleListenerBase lifecycleEvent :" + lifecycleEvent.getType());
        if (Lifecycle.AFTER_START_EVENT.equals(lifecycleEvent.getType())) {
            if (lifecycleEvent.getSource() instanceof StandardContext) {
                StandardContext context = (StandardContext) lifecycleEvent.getSource();
                lifecycleEvent(context, getAppInfo(context));
            }
        }
    }

    public abstract void lifecycleEvent(StandardContext context, AppInfo appInfo);

    protected  void setAppInfo(StandardContext context, AppInfo appInfo){
        context.getServletContext().setAttribute("APP_INFO", appInfo);
    }

    protected  AppInfo getAppInfo(StandardContext context){
       return (AppInfo) context.getServletContext().getAttribute("APP_INFO");
    }
}
