package org.wso2.carbon.webapp.mgt.ext;

public class AppInfo {

    private boolean managedApi;
    private String apiName;
    private String verion;

    public AppInfo(String appContext, String version){
        if(appContext.startsWith("/")){
            appContext = appContext.substring(1);
        }
        this.apiName = appContext;
        this.verion = version;
    }

    public boolean isManagedApi() {
        return managedApi;
    }

    public void setManagedApi(boolean managedApi) {
        this.managedApi = managedApi;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getVerion() {
        return verion;
    }

    public void setVerion(String verion) {
        this.verion = verion;
    }
}
