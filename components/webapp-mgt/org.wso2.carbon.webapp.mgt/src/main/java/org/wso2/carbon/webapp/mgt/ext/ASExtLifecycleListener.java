package org.wso2.carbon.webapp.mgt.ext;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.catalina.ContainerListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.wso2.carbon.webapp.mgt.loader.LoaderConstants;


public class ASExtLifecycleListener extends CarbonLifecycleListenerBase {
    private static final Log log = LogFactory.getLog(ASExtLifecycleListener.class);

    @Override
    public void lifecycleEvent(StandardContext context, AppInfo appInfo) {
        //TODO - Write this properly
        log.info(" >>>>>>>>>>>>>> ASExtLifecycleListener lifecycleEvent");

        try {
            URL url = context.getServletContext().getResource("/META-INF/application.xml");

            log.info(" >>>>>>>>>> context : " + context.getName() + " , url : " + url);

            if(url != null){
                //TODO add more info into AppInfo eg: api version, API name, api contexts + params
                AppInfo info = new AppInfo();
                info.setManagedApi(true);
                log.info("XXXXX " + context.getName() + " webapp has managedAPI=true");
                setAppInfo(context, info);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

//	public void lifecycleEvent(LifecycleEvent event) {
//
//		if (Lifecycle.AFTER_START_EVENT.equals(event.getType())) {
//
//
//            System.out.println("############################## 1");
//
//			//System.out.println("=============================== EVENT :: " + event.getType());
//			//System.out.println("=============================== SOURCE :: " + event.getSource());
//			//before_start,configure_start,start,after_start,
//			//before_stop,stop,configure_stop,after_stop
//			//if("before_start".equals(event.getType())){
//			if (event.getSource() instanceof StandardContext) {
//				StandardContext context = (StandardContext) event.getSource();
//				configure(context);
//
//			}
//			//}
//
//		}
//
//	}

//	private void configure(StandardContext context) {
//
//
//
//        APIPublisherLifecycleListener cdiLikeListener = new APIPublisherLifecycleListener();
//
//        try {
//            URL url = context.getServletContext().getResource("/META-INF/application.xml");
//            if(url != null)  {
//                System.out.println("============================= " + url);
//               // context.addLifecycleListener(cdiLikeListener);
//        }
//
//
//        } catch (MalformedURLException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//
//
//
//
//	}

}
