package org.wso2.carbon.webapp.mgt.ext;

import org.wso2.carbon.core.ServerStartupHandler;
import org.wso2.carbon.webapp.mgt.internal.APIDataHolder;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: sagara
 * Date: 5/22/14
 * Time: 8:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class ASAPIStartupPublisher  implements ServerStartupHandler {
    @Override
    public void invoke() {

        Map<String, Map<String, String>>  apiMap = APIDataHolder.getInstance().getInitialAPIInfoMap();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>> " + apiMap);
    }
}
