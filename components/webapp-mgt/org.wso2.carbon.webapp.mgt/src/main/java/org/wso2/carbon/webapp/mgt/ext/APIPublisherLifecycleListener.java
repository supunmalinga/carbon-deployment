package org.wso2.carbon.webapp.mgt.ext;

import org.apache.axis2.Constants;
import org.apache.catalina.core.StandardContext;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.scannotation.AnnotationDB;
import org.scannotation.WarUrlFinder;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.CurrentSession;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.webapp.mgt.internal.APIDataHolder;

import javax.cache.Cache;
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

    private static final String httpPort = "mgt.transport.http.port";
    private static final String hostName = "carbon.local.ip";

    @Override
    public void lifecycleEvent(StandardContext context, AppInfo appInfo) {
        if (appInfo != null) {
            log.info(" ## " + context.getName() + " appInfo.isManagedApi : " + appInfo.isManagedApi() + " APIPublisherLifecycleListener.lifecycleEvent");
        } else {
            log.info(" ## " + context.getName() + " appInfo = NULL !!! , APIPublisherLifecycleListener.lifecycleEvent");
        }

        if (appInfo != null && appInfo.isManagedApi()) {
            log.info(" ## " + context.getName() + " Scanning app for annotations." );
            scanStandardContext(context);

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
                log.info(" ## apiProvider == " + apiProvider);

                /*String username = CarbonContext.getCurrentContext().getUsername();
                if(username == null || username.equals("")) {
                    username = "admin";
                }
                log.info(" ## UserName == " + username);*/

                String provider = "admin"; //todo get correct provider(username) for tenants

                String apiVersion = appInfo.getVerion();

                String apiName = null;
                String apiContext = appInfo.getApiName();
                if (!apiContext.startsWith("/")) {
                    apiName = apiContext;
                    apiContext = "/" + apiContext;
                } else {
                    apiName = apiContext.substring(1);
                }


                String apiEndpoint = "http://" + System.getProperty(hostName) + ":" + System.getProperty(httpPort) + apiContext;

                String iconPath = "";
                String documentURL = "";
                String authType = "Any";

                APIIdentifier identifier = new APIIdentifier(provider, apiName, apiVersion);
                try {
                    if (apiProvider.isAPIAvailable(identifier)) {
                        //todo : do nothing ?? update API ?
//                        apiProvider.deleteAPI(identifier);
                        log.info("Skip adding duplicate API " + apiName);
                        return;
                    }
                } catch (APIManagementException e) {
                    log.error("Error while deleting existing API", e);
                }

                API api = createAPIModel(apiProvider, apiContext, apiEndpoint, authType, identifier);

                if (api != null) {
                    try {
                        apiProvider.createProductAPI(api);

                        /*Cache contextCache = APIUtil.getAPIContextCache();
                        if (APIUtil.isAPIManagementEnabled()) {
                            Boolean isExistingContext = null;
                            if (contextCache.get(api.getContext()) != null) {
                                isExistingContext = Boolean.parseBoolean(contextCache.get(api.getContext()).toString());
                            }
                            if (isExistingContext == null) {
                                contextCache.put(api.getContext(), true);
                            }
                        }*/
                        log.info(" ##### Successfully added the API ::= provider: " + provider + " name: " + apiName + " version:" + apiVersion);
                    } catch (APIManagementException e) {
                        log.error("Error while adding API", e);
                    }
                }
            } else {
                //todo add to map to wait until apiProvier is ready
                /*Map<String, String> map = new HashMap<String, String>();
                map.put("testKey", "testValue");
                APIDataHolder.getInstance().getInitialAPIInfoMap().put("test-1", map);*/
            }

                /*try {
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
                }*/
        }
    }

    private API createAPIModel(APIProvider apiProvider, String apiContext, String apiEndpoint, String authType, APIIdentifier identifier) {
        API api = null;
        try {
            api = new API(identifier);
            api.setContext(apiContext);
            api.setUrl(apiEndpoint);
            api.setUriTemplates(getURITemplates(apiEndpoint, authType));
            api.setVisibility(APIConstants.API_GLOBAL_VISIBILITY);
            api.addAvailableTiers(apiProvider.getTiers());
            api.setEndpointSecured(false);
            api.setStatus(APIStatus.PUBLISHED);
            api.setTransports(Constants.TRANSPORT_HTTP + "," + Constants.TRANSPORT_HTTPS);

            Set<Tier> tiers = new HashSet<Tier>();
            tiers.add(new Tier(APIConstants.UNLIMITED_TIER));
//            tiers.add(new Tier("gold"));
//            tiers.add(new Tier("silver"));
//            tiers.add(new Tier("bronze"));
            api.addAvailableTiers(tiers);
            api.setSubscriptionAvailability(APIConstants.SUBSCRIPTION_TO_ALL_TENANTS);
            api.setResponseCache(APIConstants.DISABLED);

            String endpointConfig = "{\"production_endpoints\":{\"url\":\" "+ apiEndpoint + "\",\"config\":null},\"endpoint_type\":\"http\"}";
            api.setEndpointConfig(endpointConfig);
//            api.setAsDefaultVersion(Boolean.TRUE);

            //todo add a proper icon
            /* Adding Icon*/
            /*File file = null;
            if (!APIStartupPublisherConstants.API_ICON_PATH_AND_DOCUMENT_URL_DEFAULT.equals(iconPath)) {
                file =new File(iconPath);
                String absolutePath = file.getAbsolutePath();
                Icon icon = new Icon(getImageInputStream(absolutePath), getImageContentType(absolutePath));
                String thumbPath = APIUtil.getIconPath(identifier);
                String thumbnailUrl = provider.addIcon(thumbPath, icon);
                api.setThumbnailUrl(APIUtil.prependTenantPrefix(thumbnailUrl, apiProvider));*/

            /*Set permissions to anonymous role for thumbPath*/
                /*APIUtil.setResourcePermissions(apiProvider, null, null, thumbPath);*/

        } catch (APIManagementException e) {
            log.error("Error while initializing API Provider", e);
        } /*catch (IOException e) {
            log.error("Error while reading image from icon path", e);
        }*/
        return api;
    }

    private Set<URITemplate> getURITemplates(String endpoint, String authType) {
        //todo improve to add sub context paths for uri templates as well
        Set<URITemplate> uriTemplates = new LinkedHashSet<URITemplate>();
        String[] httpVerbs = { "GET", "POST", "PUT", "DELETE", "OPTIONS" };

        if (authType.equals(APIConstants.AUTH_NO_AUTHENTICATION)) {
            for (int i = 0; i < 5; i++) {
                URITemplate template = new URITemplate();
                template.setAuthType(APIConstants.AUTH_NO_AUTHENTICATION);
                template.setHTTPVerb(httpVerbs[i]);
                template.setResourceURI(endpoint);
                template.setUriTemplate("/*");
                uriTemplates.add(template);
            }
        } else {
            for (int i = 0; i < 5; i++) {
                URITemplate template = new URITemplate();
                if (i != 4) {
                    template.setAuthType(APIConstants.AUTH_APPLICATION_OR_USER_LEVEL_TOKEN);
                } else {
                    template.setAuthType(APIConstants.AUTH_NO_AUTHENTICATION);
                }
                template.setHTTPVerb(httpVerbs[i]);
                template.setResourceURI(endpoint);
                template.setUriTemplate("/*");
                uriTemplates.add(template);
            }
        }

        return uriTemplates;
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
                log.info(" ## Application Context root = " + context.getContextPath());
            }
            log.info(" ## API Root  Context = " + root);
            log.info(" ## API Sub Context List ");
            for (Method method : clazz.getDeclaredMethods()) {
                Path path = method.getAnnotation(Path.class);
                if (path != null) {
                    String subCtx = path.value();
                    log.info(" ## " + root + "/" + subCtx);
                }
            }
        }
        log.info("===================================================== ");
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
