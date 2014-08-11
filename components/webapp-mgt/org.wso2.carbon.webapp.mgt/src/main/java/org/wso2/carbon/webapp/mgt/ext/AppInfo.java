package org.wso2.carbon.webapp.mgt.ext;

/**
 * Created with IntelliJ IDEA.
 * User: sagara
 * Date: 5/20/14
 * Time: 9:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AppInfo  {

    private boolean managedApi;

    public boolean isManagedApi() {
        return managedApi;
    }

    public void setManagedApi(boolean managedApi) {
        this.managedApi = managedApi;
    }
}
