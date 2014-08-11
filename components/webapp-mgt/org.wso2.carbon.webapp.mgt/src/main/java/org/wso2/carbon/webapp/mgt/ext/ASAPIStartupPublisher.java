package org.wso2.carbon.webapp.mgt.ext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.ServerStartupHandler;
import org.wso2.carbon.webapp.mgt.internal.APIDataHolder;

import java.util.Map;

public class ASAPIStartupPublisher implements ServerStartupHandler {

    private static final Log log = LogFactory.getLog(ASAPIStartupPublisher.class);

    @Override
    public void invoke() {

        log.info(" YYYYYYYYYYYYYY , ASAPIStartupPublisher.invoke() !!");
        Map<String, Map<String, String>> apiMap = APIDataHolder.getInstance().getInitialAPIInfoMap();
//        log.info(" YYYYYYYYYYYYYY , # of APIs to be published : " + apiMap.size());
    }
}
