/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.workflow.handler.distribution;

import static org.junit.Assert.assertEquals;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.security.api.Organization;
import org.opencastproject.workflow.api.WorkflowOperationException;

import org.easymock.EasyMock;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class PublishEngageWorkflowOperationHandlerTest {
  private Organization org;

  @Test
  public void testPlayerUrl() throws WorkflowOperationException, URISyntaxException {
    URI engageURI = new URI("http://engage.org");
    String mpId = "mp-id";

    MediaPackage mp = EasyMock.createNiceMock(MediaPackage.class);
    Id id = new IdImpl(mpId);
    EasyMock.expect(mp.getIdentifier()).andStubReturn(id);
    MediaPackageElement element = EasyMock.createNiceMock(MediaPackageElement.class);
    EasyMock.replay(element, mp);

    // Test configured organization player path
    PublishEngageWorkflowOperationHandler publishEngagePublish = new PublishEngageWorkflowOperationHandler();
    URI result = publishEngagePublish.createEngageUri(engageURI, mp);
    assertEquals(engageURI.toString() + "/play/" + mpId, result.toString());
  }

  @Test
  public void testDefaultPlayerPath() throws URISyntaxException {
    URI engageURI = new URI("http://engage.org");
    String mpId = "mp-id";

    MediaPackage mp = EasyMock.createNiceMock(MediaPackage.class);
    Id id = new IdImpl(mpId);
    EasyMock.expect(mp.getIdentifier()).andStubReturn(id);
    MediaPackageElement element = EasyMock.createNiceMock(MediaPackageElement.class);
    EasyMock.replay(element, mp);

    // Test default player path
    PublishEngageWorkflowOperationHandler publishEngagePublish = new PublishEngageWorkflowOperationHandler();
    URI result = publishEngagePublish.createEngageUri(engageURI, mp);
    assertEquals(engageURI.toString() + PublishEngageWorkflowOperationHandler.PLAYER_PATH + mpId,
            result.toString());

  }

  // Util to set org properties with player path
  private Organization getOrgWithPlayerPath() {
    org = EasyMock.createNiceMock(Organization.class);
    EasyMock.replay(org);
    return org;
  }

  // Util to set org properties without player path
  public Organization getOrgWithoutPlayerPath() {
    Map<String, String> properties = new HashMap<String, String>();
    org = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(org.getProperties()).andStubReturn(properties);
    EasyMock.replay(org);
    return org;
  }

}
