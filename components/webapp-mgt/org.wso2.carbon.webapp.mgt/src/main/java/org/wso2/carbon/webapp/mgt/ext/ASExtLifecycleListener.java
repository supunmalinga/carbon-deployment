package org.wso2.carbon.webapp.mgt.ext;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.catalina.core.StandardContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


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
                String appVersion = getAppVersion(context.getName());
                String appName = context.getName().substring(0, context.getName().indexOf(appVersion) - 1);
                AppInfo info = new AppInfo(appName, appVersion);

                //todo handle scenario for unversioned apps
                info.setManagedApi(true);
                log.info("XXXXX " + context.getName() + " webapp has managedAPI=true");
                setAppInfo(context, info);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private String getAppVersion(String appContext) {
        String versionString = appContext;
        if (versionString.startsWith("/t/")) {
            //remove tenant context
            versionString = versionString.substring(appContext.lastIndexOf("/webapps/") + 9);
        } else if(appContext.startsWith("/")) {
            versionString = versionString.substring(1);
        }
        if (versionString.contains("/")) {
            versionString = versionString.substring(versionString.indexOf("/") + 1);
            return versionString;
        } else {
            return "";
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
