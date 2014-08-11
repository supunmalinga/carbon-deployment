package org.wso2.carbon.webapp.mgt.ext;

import org.apache.catalina.core.StandardContext;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.scannotation.AnnotationDB;
import org.scannotation.WarUrlFinder;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.APIStatus;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.webapp.mgt.internal.APIDataHolder;

import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class APIPublisherLifecycleListener extends CarbonLifecycleListenerBase {

    private static final Log log = LogFactory.getLog(APIPublisherLifecycleListener.class);

    @Override
    public void lifecycleEvent(StandardContext context, AppInfo appInfo) {
        if (appInfo != null) {
            log.info("XXXXXXXXXXX " + context.getName() + " appInfo.isManagedApi : " + appInfo.isManagedApi() + " APIPublisherLifecycleListener.lifecycleEvent");
        } else {
            log.info("XXXXXXXXXXX " + context.getName() + " appInfo = NULL !!! , APIPublisherLifecycleListener.lifecycleEvent");
        }

        if (appInfo != null && appInfo.isManagedApi()) {
            log.info("XXXXXXXXXXXX " + context.getName() + " Scanning app for annotations." );
            scanStandardContext(context);

            //todo write properly
            addApi(context, appInfo);
        }
    }

    public void scanStandardContext(StandardContext context) {
//        Set<String> entityClasses = getAnnotatedClassesStandardContext(context, Path.class);
        Set<String> entityClasses = null;

        AnnotationDB db = new AnnotationDB();
        db.addIgnoredPackages("org.apache");
        db.addIgnoredPackages("org.codehaus");
        db.addIgnoredPackages("org.springframework");

        final String path = context.getRealPath("/WEB-INF/classes");
        //TODO follow the above line for "WEB-INF/lib" as well

        URL[] libPath = WarUrlFinder.findWebInfLibClasspaths(context.getServletContext());
        URL classPath = WarUrlFinder.findWebInfClassesPath(context.getServletContext());
        URL[] urls = (URL[]) ArrayUtils.add(libPath, libPath.length, classPath);

        try {
            db.scanArchives(urls);
            entityClasses = db.getAnnotationIndex().get(Path.class.getName());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (entityClasses != null && !entityClasses.isEmpty()) {
            for (String className : entityClasses) {
                try {

                    List<URL> fileUrls = convertToFileUrl(libPath, classPath, context.getServletContext());

                    URLClassLoader cl = new URLClassLoader(fileUrls.toArray(new URL[]{}), this.getClass().getClassLoader());

                    //Class<?> clazz = cl.loadClass(className);
                    Class<?> clazz = context.getServletContext().getClassLoader().loadClass(className);

                    showAPIinfo(context.getServletContext(), clazz);
                    cl = null;

                } catch (ClassNotFoundException e) {
                    log.error(e.getStackTrace());
                }
            }
        }
    }

    private List<URL> convertToFileUrl(URL[] libPath, URL classPath, ServletContext context) {

        if ((libPath != null || libPath.length == 0) && classPath == null) {
            return null;
        }

        List<URL> list = new ArrayList<URL>();
        if (classPath != null) {
            list.add(classPath);
        }

        if (libPath != null && libPath.length != 0) {
            final String libBasePath = context.getRealPath("/WEB-INF/lib");
            for (URL lib : libPath) {
                String path = lib.getPath();
                if (path != null) {
                    String fileName = path.substring(path.lastIndexOf(File.separator));
                    if (fileName != null) {
                        try {
                            list.add(new URL("jar:file://" + libBasePath + File.separator + fileName + "!/"));
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return list;
    }

    private static Set<String> getAnnotatedClassesStandardContext(
            StandardContext context, Class<?> annotation) {

        AnnotationDB db = new AnnotationDB();
        db.addIgnoredPackages("org.apache");
        db.addIgnoredPackages("org.codehaus");
        db.addIgnoredPackages("org.springframework");

        final String path = context.getRealPath("/WEB-INF/classes");
        //TODO follow the above line for "WEB-INF/lib" as well
        URL resourceUrl = null;
        URL[] urls = null;

        if (path != null) {
            final File fp = new File(path);
            if (fp.exists()) {
                try {
                    resourceUrl = fp.toURI().toURL();
                    urls = new URL[]{new URL(resourceUrl.toExternalForm())};

                    db.scanArchives(urls);
                    return db.getAnnotationIndex().get(annotation.getName());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }


    public void addApi(StandardContext context, AppInfo appInfo) {
        log.info(" ZZZZZZZ Creating API for : " + context.getName());
        if (appInfo != null && appInfo.isManagedApi()) {
            if (isAPIProviderReady()) {
                //todo check null
                // if null --> add to map
                APIProvider apiProvider = getAPIProvider();

                String username = CarbonContext.getCurrentContext().getUsername();

                log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXX UserName == " + username);
                log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXX apiProvider == " + apiProvider);

                if(username == null || username.equals("")) {
                    username = "admin";
                }

                APIIdentifier apiId = new APIIdentifier("admin", "test-api-1", "1.0.0");

                try {
                    if (apiProvider.isAPIAvailable(apiId)) {
                        apiProvider.deleteAPI(apiId);
                    }
                } catch (APIManagementException e) {
                    log.error(e.getStackTrace());
                    e.printStackTrace();
                }

                try {
                    API api = new API(apiId);
                    api.setStatus(APIStatus.CREATED);
                    //api.setApiOwner("admin");
                    api.setContext("/test-1");

                    Set<URITemplate> uriTemplates = new LinkedHashSet<URITemplate>();

                    URITemplate template = new URITemplate();
                    String uriTemp = "/test";
                    String uriTempVal = uriTemp.startsWith("/") ? uriTemp : ("/" + uriTemp);
                    template.setUriTemplate(uriTempVal);
                    String throttlingTier = "Gold";
                    template.setHTTPVerb("GET");
                    String authType = "Application & Application User";
                    if (authType.equals("Application & Application User")) {
                        authType = APIConstants.AUTH_APPLICATION_OR_USER_LEVEL_TOKEN;
                    }
                    if (authType.equals("Application User")) {
                        authType = "Application_User";
                    }
                    template.setThrottlingTier(throttlingTier);
                    template.setAuthType(authType);
                    template.setResourceURI("http://192.168.1.2:9763/jaxrs_basic/services/customers/customerservice/customers/123");
                    //template.setResourceSandboxURI(sandboxUrl);

                    uriTemplates.add(template);

                    api.setUriTemplates(uriTemplates);
                    apiProvider.addAPI(api);

                } catch (APIManagementException e) {
                    log.error(e.getStackTrace());
                }
            }
        }
    }


//    @Override
//    public void lifecycleEvent(LifecycleEvent lifecycleEvent) {
//
//        if (Lifecycle.AFTER_START_EVENT.equals(lifecycleEvent.getType())
//                && lifecycleEvent.getSource() instanceof StandardContext) {
//
//            System.out.println("############################## 2");
//
//            StandardContext context = (StandardContext) lifecycleEvent.getSource();
//            System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");
//
//            try {
//                if(isAPIProviderReady()){
//                APIProvider apiProvider = getAPIProvider();
//                 System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXX" + apiProvider);
//                }
//            } catch (APIManagementException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//
//            // APIScaner.scanStandardContext(context);
//
//
//
//
//        }
//    }


    private static void showAPIinfo(ServletContext context, Class<?> clazz) {

        Path rootCtx = clazz.getAnnotation(Path.class);
        if (rootCtx != null) {
            String root = rootCtx.value();
            log.info("======================== API INFO ======================= ");
            if (context != null) {
                log.info(" ############ Application Context root = " + context.getContextPath());
            }
            log.info(" ############ API Root  Context = " + root);
            log.info(" ############ API Sub Context List ");
            for (Method method : clazz.getDeclaredMethods()) {
                Path path = method.getAnnotation(Path.class);
                if (path != null) {
                    String subCtx = path.value();
                    log.info(" ############ " + root + "/" + subCtx);
                }
            }
        }
        log.info("===================================================== ");

        Map<String, String> map = new HashMap<String, String>();
        map.put("testKey", "testValue");
        APIDataHolder.getInstance().getInitialAPIInfoMap().put("test-1", map);
    }

    private APIProvider getAPIProvider() {
        try {
            //todo get current user
            return APIManagerFactory.getInstance().getAPIProvider("admin");
        } catch (APIManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isAPIProviderReady() {
        if (ServiceReferenceHolder.getInstance()
                .getAPIManagerConfigurationService() != null) {
            return true;
        }
        return false;
    }
}
