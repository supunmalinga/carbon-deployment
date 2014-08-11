package org.wso2.carbon.webapp.mgt.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Created with IntelliJ IDEA.
 * User: sagara
 * Date: 5/22/14
 * Time: 7:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class APIDataHolder {

    private static APIDataHolder instance = new APIDataHolder();

    private Map<String, Map<String, String>> initialAPIInfoMap;


    public static APIDataHolder getInstance() {
        return instance;
    }

    public Map<String, Map<String, String>> getInitialAPIInfoMap() {
        if(initialAPIInfoMap == null){
            initialAPIInfoMap = new HashMap<String, Map<String, String>>();
        }
        return initialAPIInfoMap;
    }

    public void setInitialAPIInfoMap(Map<String, Map<String, String>> initialAPIInfoMap) {
        this.initialAPIInfoMap = initialAPIInfoMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        APIDataHolder that = (APIDataHolder) o;

        if (initialAPIInfoMap != null ? !initialAPIInfoMap.equals(that.initialAPIInfoMap) : that.initialAPIInfoMap != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return initialAPIInfoMap != null ? initialAPIInfoMap.hashCode() : 0;
    }
}
