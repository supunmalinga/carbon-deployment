/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.webapp.mgt;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Request;
import org.apache.catalina.util.SessionConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.http.mapper.MappingData;
import org.wso2.carbon.tomcat.api.CarbonTomcatService;
import org.wso2.carbon.url.mapper.HotUpdateService;
import org.wso2.carbon.utils.CarbonUtils;

import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of utility methods for interacting with Embedded Tomcat
 */
public class TomcatUtil {
    private static CarbonTomcatService carbonTomcatService;
    private static Map<String, TomcatGenericWebappsDeployer> webappsDeployers
        = new HashMap<String, TomcatGenericWebappsDeployer>();
    private static Log log = LogFactory.getLog(TomcatUtil.class);

    /**
     *  check unpack wars property
     * @return true if webapp unpacking enabled.
     */
    public static boolean checkUnpackWars() {
        CarbonTomcatService carbonTomcatService = DataHolder.getCarbonTomcatService();
        return carbonTomcatService.isUnpackWARs();
    }


    /**
     * Add a TomcatGenericWebappsDeployer
     *
     * @param webappsDir      The directory in which the webapps are found
     * @param webappsDeployer The corresponding TomcatGenericWebappsDeployer
     */
    @SuppressWarnings("unused")
    public static void addWebappsDeployer(String webappsDir,
                                          TomcatGenericWebappsDeployer webappsDeployer) {
        CarbonUtils.checkSecurity();
        webappsDeployers.put(webappsDir, webappsDeployer);
    }

    /**
     * Retrieve the list of registered webapps deployers
     *
     * @return an unmodifiable list of registered webapps deployers
     */
    @SuppressWarnings("unused")
    public static Map<String, TomcatGenericWebappsDeployer> getWebappsDeployers() {
        CarbonUtils.checkSecurity();
        return Collections.unmodifiableMap(webappsDeployers);
    }

    /**
     * This method is used in remapping a request with context at tomcat level. This is mainly used
     * with Lazy loading of tenants and Lazy loading of webapps, where we can remap a request for a
     * lazy loaded webapp so that any request (GET, POST) parameters will not get lost with the
     * first request.
     *
     * @param request - servlet request to be remapped for contexts
     * @throws Exception - on error
     */
    public static void remapRequest(HttpServletRequest request) throws Exception {
        Request connectorReq = (Request) request;

        MappingData mappingData = connectorReq.getMappingData();
        mappingData.recycle();

        connectorReq.getConnector().
                getMapper().map(connectorReq.getCoyoteRequest().serverName(),
                                connectorReq.getCoyoteRequest().decodedURI(), null,
                                mappingData);

        connectorReq.setContext((Context) connectorReq.getMappingData().context);
        connectorReq.setWrapper((Wrapper) connectorReq.getMappingData().wrapper);

        parseSessionCookiesId(connectorReq);
    }
    
    public static String getApplicationNameFromContext(String contextName) {
        String appName = null;
        if(contextName.contains(WebappsConstants.WEBAPP_PREFIX)
                || contextName.contains(WebappsConstants.JAGGERY_APPS_PREFIX)
                || contextName.contains(WebappsConstants.JAX_WEBAPPS_PREFIX)) {
            if(contextName.startsWith("#")) {
                String[] temp = contextName.split("#");
                appName = temp[temp.length - 1];
            } else if(contextName.startsWith("/")) {
                String[] temp = contextName.split("/");
                appName = temp[temp.length - 1];
            }
        } else {
            appName = contextName;
        }
        return appName;
    }

    public static Boolean isVirtualHostRequest(String requestedHostName) {
        Boolean isVirtualHostRequest = false;
        HotUpdateService hotUpdate = DataHolder.getHotUpdateService();
        //checking for whether the request is for virtual host or not, if the server installed with url-mappings only.
        if(hotUpdate != null && requestedHostName.endsWith(hotUpdate.getSuffixOfHost())) {
            //in case server url from carbon.xml used as a suffix, then localhost request won't get executed here
            isVirtualHostRequest = true;
        }
        return isVirtualHostRequest;
    }

    private static void parseSessionCookiesId(Request request) {

        // If session tracking via cookies has been disabled for the current context, don't go looking for a session ID
        // in a cookie as a cookie from a parent context with a session ID may be present which would overwrite
        // the valid session ID encoded in the URL
        Context context = (Context) request.getMappingData().context;
        if (context != null &&
            !context.getServletContext().getEffectiveSessionTrackingModes().contains(SessionTrackingMode.COOKIE)) {
            return;
        }

        // Parse session id from cookies
        Cookie[] serverCookies = request.getCookies();
        int count = serverCookies.length;
        if (count <= 0) {
            return;
        }

        String sessionCookieName = SessionConfig.getSessionCookieName(context);
        for (int i = 0; i < count; i++) {
            Cookie cookie = serverCookies[i];
            if (cookie.getName().equals(sessionCookieName)) {
                // Override anything requested in the URL
                if (!request.isRequestedSessionIdFromCookie()) {
                    // Accept only the first session id cookie
                    request.setRequestedSessionId(cookie.getValue());
                    request.setRequestedSessionCookie(true);
                    request.setRequestedSessionURL(false);
                    if (log.isDebugEnabled()) {
                        log.debug("Requested cookie session id is " + request.getRequestedSessionId());
                    }
                } else {
                    if (!request.isRequestedSessionIdValid()) {
                        // Replace the session id until one is valid
                        request.setRequestedSessionId(cookie.getValue());
                    }
                }
            }
        }
    }
}
