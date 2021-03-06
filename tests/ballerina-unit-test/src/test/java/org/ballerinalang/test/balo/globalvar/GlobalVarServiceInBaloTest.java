/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.test.balo.globalvar;

import org.ballerinalang.launcher.util.BServiceUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BJSON;
import org.ballerinalang.test.balo.BaloCreator;
import org.ballerinalang.test.services.testutils.HTTPTestRequest;
import org.ballerinalang.test.services.testutils.MessageUtils;
import org.ballerinalang.test.services.testutils.Services;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.transport.http.netty.message.HTTPCarbonMessage;
import org.wso2.transport.http.netty.message.HttpMessageDataStreamer;

import java.io.IOException;

/**
 * Tests for global variables in a BALO usage within services test cases.
 * 
 * @since 0.975.0
 */
public class GlobalVarServiceInBaloTest {

    CompileResult result;
    private static final String MOCK_ENDPOINT_NAME = "echoEP";

    @BeforeClass
    public void setup() throws IOException {
        BaloCreator.createAndSetupBalo("test-src/balo/test_projects/test_project", "testorg", "foo");
        result = BServiceUtil
                .setupProgramFile(this, "test-src/balo/test_balo/globalvar/test_global_var_service.bal");
    }

    @Test(description = "Test defining global variables in services")
    public void testDefiningGlobalVarInService() {
        HTTPTestRequest cMsg = MessageUtils.generateHTTPMessage("/globalvar/defined", "GET");
        HTTPCarbonMessage response = Services.invokeNew(result, MOCK_ENDPOINT_NAME, cMsg);
        Assert.assertNotNull(response);
        //Expected Json message : {"glbVarInt":800, "glbVarString":"value", "glbVarFloat":99.34323}
        BJSON bJson = new BJSON(new HttpMessageDataStreamer(response).getInputStream());
        Assert.assertEquals(bJson.value().get("glbVarInt").asText(), "800");
        Assert.assertEquals(bJson.value().get("glbVarString").asText(), "value");
        Assert.assertEquals(bJson.value().get("glbVarFloat").asText(), "99.34323");
    }

    @Test(description = "Test accessing global variables in service level")
    public void testAccessingGlobalVarInServiceLevel() {
        HTTPTestRequest cMsg = MessageUtils.generateHTTPMessage("/globalvar/access-service-level", "GET");
        HTTPCarbonMessage response = Services.invokeNew(result, MOCK_ENDPOINT_NAME, cMsg);
        Assert.assertNotNull(response);
        //Expected Json message : {"serviceVarFloat":99.34323}
        BJSON bJson = new BJSON(new HttpMessageDataStreamer(response).getInputStream());
        Assert.assertEquals(bJson.value().get("serviceVarFloat").asText(), "99.34323");
    }

    @Test(description = "Test accessing global arrays in resource level")
    public void testGlobalArraysInResourceLevel() {
        HTTPTestRequest cMsgChange = MessageUtils.generateHTTPMessage("/globalvar/arrays", "GET");
        HTTPCarbonMessage response = Services.invokeNew(result, MOCK_ENDPOINT_NAME, cMsgChange);

        Assert.assertNotNull(response);
        //Expected Json message : {"glbArrayElement":1, "glbSealedArrayElement":0,
        // "glbSealedArray2Element":3, "glbSealed2DArrayElement":0, "glbSealed2DArray2Element":2}
        BJSON bJson = new BJSON(new HttpMessageDataStreamer(response).getInputStream());
        Assert.assertEquals(bJson.value().get("glbArrayElement").asText(), "1");
        Assert.assertEquals(bJson.value().get("glbSealedArrayElement").asText(), "0");
        Assert.assertEquals(bJson.value().get("glbSealedArray2Element").asText(), "3");
        Assert.assertEquals(bJson.value().get("glbSealed2DArrayElement").asText(), "0");
        Assert.assertEquals(bJson.value().get("glbSealed2DArray2Element").asText(), "2");
    }

    @Test(description = "Test changing global variables in resource level")
    public void testChangingGlobalVarInResourceLevel() {
        HTTPTestRequest cMsg = MessageUtils.generateHTTPMessage("/globalvar/change-resource-level", "GET");
        HTTPCarbonMessage response = Services.invokeNew(result, MOCK_ENDPOINT_NAME, cMsg);
        Assert.assertNotNull(response);
        //Expected Json message : {"glbVarFloatChange":77.87}
        BJSON bJson = new BJSON(new HttpMessageDataStreamer(response).getInputStream());
        Assert.assertEquals(bJson.value().get("glbVarFloatChange").asText(), "77.87");
    }

    @Test(description = "Test accessing changed global var in another resource in same service")
    public void testAccessingChangedGlobalVarInAnotherResource() {
        HTTPTestRequest cMsgChange = MessageUtils.generateHTTPMessage("/globalvar/change-resource-level", "GET");
        Services.invokeNew(result, MOCK_ENDPOINT_NAME, cMsgChange);

        HTTPTestRequest cMsg = MessageUtils.generateHTTPMessage("/globalvar/get-changed-resource-level", "GET");
        HTTPCarbonMessage response = Services.invokeNew(result, MOCK_ENDPOINT_NAME, cMsg);
        Assert.assertNotNull(response);
        //Expected Json message : {"glbVarFloatChange":77.87}
        BJSON bJson = new BJSON(new HttpMessageDataStreamer(response).getInputStream());
        Assert.assertEquals(bJson.value().get("glbVarFloatChange").asText(), "77.87");
    }

    @Test(description = "Test accessing changed global var in another resource in different service")
    public void testAccessingChangedGlobalVarInAnotherResourceInAnotherService() {
        HTTPTestRequest cMsgChange = MessageUtils.generateHTTPMessage("/globalvar/change-resource-level", "GET");
        Services.invokeNew(result, MOCK_ENDPOINT_NAME, cMsgChange);

        HTTPTestRequest cMsg = MessageUtils.generateHTTPMessage("/globalvar-second/get-changed-resource-level",
                                                                  "GET");
        HTTPCarbonMessage response = Services.invokeNew(result, MOCK_ENDPOINT_NAME, cMsg);
        Assert.assertNotNull(response);
        //Expected Json message : {"glbVarFloatChange":77.87}
        BJSON bJson = new BJSON(new HttpMessageDataStreamer(response).getInputStream());
        Assert.assertEquals(bJson.value().get("glbVarFloatChange").asText(), "77.87");
    }

    @AfterClass
    public void tearDown() {
        BaloCreator.clearPackageFromRepository("testorg", "foo");
    }
}
