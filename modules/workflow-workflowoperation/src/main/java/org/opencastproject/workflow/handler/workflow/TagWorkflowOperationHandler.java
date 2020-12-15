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

package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple implementation that holds for user-entered trim points.
 */
public class TagWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(TagWorkflowOperationHandler.class);
  private static final String PLUS = "+";
  private static final String MINUS = "-";

  /** Name of the configuration option that provides the source flavors we are looking for */
  public static final String SOURCE_FLAVORS_PROPERTY = "source-flavors";

  /** Name of the configuration option that provides the source tags we are looking for */
  public static final String SOURCE_TAGS_PROPERTY = "source-tags";

  /** Name of the configuration option that provides the target flavors we are looking for */
  public static final String TARGET_FLAVOR_PROPERTY = "target-flavor";

  /** Name of the configuration option that provides the target tags we are looking for */
  public static final String TARGET_TAGS_PROPERTY = "target-tags";

  /** Name of the configuration option that provides the copy boolean we are looking for */
  public static final String COPY_PROPERTY = "copy";

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    String configuredSourceFlavors = StringUtils
            .trimToEmpty(currentOperation.getConfiguration(SOURCE_FLAVORS_PROPERTY));
    String configuredSourceTags = StringUtils.trimToEmpty(currentOperation.getConfiguration(SOURCE_TAGS_PROPERTY));
    String configuredTargetFlavor = StringUtils.trimToNull(currentOperation.getConfiguration(TARGET_FLAVOR_PROPERTY));
    String configuredTargetTags = StringUtils.trimToEmpty(currentOperation.getConfiguration(TARGET_TAGS_PROPERTY));
    boolean copy = BooleanUtils.toBoolean(currentOperation.getConfiguration(COPY_PROPERTY));

    if (copy) {
      logger.info("Retagging mediapackage elements as a copy");
    } else {
      logger.info("Retagging mediapackage elements");
    }

    String[] sourceTags = StringUtils.split(configuredSourceTags, ",");
    String[] targetTags = StringUtils.split(configuredTargetTags, ",");
    String[] sourceFlavors = StringUtils.split(configuredSourceFlavors, ",");

    SimpleElementSelector elementSelector = new SimpleElementSelector();
    for (String flavor : sourceFlavors) {
      elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }

    List<String> removeTags = new ArrayList<String>();
    List<String> addTags = new ArrayList<String>();
    List<String> overrideTags = new ArrayList<String>();

    for (String tag : targetTags) {
      if (tag.startsWith(MINUS)) {
        removeTags.add(tag);
      } else if (tag.startsWith(PLUS)) {
        addTags.add(tag);
      } else {
        overrideTags.add(tag);
      }
    }

    for (String tag : sourceTags) {
      elementSelector.addTag(tag);
    }

    Collection<MediaPackageElement> elements = elementSelector.select(mediaPackage, false);
    for (MediaPackageElement e : elements) {
      MediaPackageElement element = e;
      if (copy) {
        element = (MediaPackageElement) e.clone();
        element.setIdentifier(null);
        element.setURI(e.getURI()); // use the same URI as the original
      }

      if (configuredTargetFlavor != null) {

        MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(configuredTargetFlavor);
        String targetFlavorType = targetFlavor.getType();
        String targetFlavorSubtype = targetFlavor.getSubtype();

        if (MediaPackageElementFlavor.WILDCARD.equals(targetFlavorType)) {
          targetFlavorType = element.getFlavor().getType();
        }

        if (MediaPackageElementFlavor.WILDCARD.equals(targetFlavorSubtype)) {
          targetFlavorSubtype = element.getFlavor().getSubtype();
        }

        String targetFlavorStr = targetFlavorType + MediaPackageElementFlavor.SEPARATOR + targetFlavorSubtype;
        element.setFlavor(MediaPackageElementFlavor.parseFlavor(targetFlavorStr));
      }

      if (overrideTags.size() > 0) {
        element.clearTags();
        for (String tag : overrideTags) {
          element.addTag(tag);
        }
      } else {
        for (String tag : removeTags) {
          element.removeTag(tag.substring(MINUS.length()));
        }
        for (String tag : addTags) {
          element.addTag(tag.substring(PLUS.length()));
        }
      }

      if (copy)
        mediaPackage.addDerived(element, e);
    }
    return createResult(mediaPackage, Action.CONTINUE);
  }
}
