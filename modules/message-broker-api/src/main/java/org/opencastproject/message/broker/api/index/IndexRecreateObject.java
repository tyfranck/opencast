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

package org.opencastproject.message.broker.api.index;

import java.io.Serializable;

public final class IndexRecreateObject implements Serializable {

  private static final long serialVersionUID = 6076737478411640536L;

  public enum Status {
    Start, Update, End, Error
  }

  /**
   * New services may be added in arbitrary order since the elements are identified by name, not their ordinal.
   */
  public enum Service {
    Acl, AssetManager, Comments, Groups, Scheduler, Series, Themes, Workflow
  }

  private String indexName;
  private String message;
  private int total;
  private int current;
  private Status status;
  private Service service;

  /**
   * Constructor for a start or stop message.
   *
   * @param indexName
   *          The index name
   * @param service
   *          The service this message relates to.
   * @param status
   *          The status of the message.
   */
  private IndexRecreateObject(String indexName, Service service, Status status) {
    this.indexName = indexName;
    this.service = service;
    this.status = status;
  }

  /**
   * Constructor for an update message.
   *
   * @param indexName
   *          The index name
   * @param service
   *          The service that has been updated.
   * @param total
   *          The total number of objects that will be re-added.
   * @param current
   *          The current number that have been re-added.
   */
  private IndexRecreateObject(String indexName, Service service, int total, int current) {
    this.indexName = indexName;
    this.service = service;
    this.status = Status.Update;
    this.total = total;
    this.current = current;
  }

  public static IndexRecreateObject start(String indexName, Service service) {
    return new IndexRecreateObject(indexName, service, Status.Start);
  }

  public static IndexRecreateObject update(String indexName, Service service, int total, int current) {
    return new IndexRecreateObject(indexName, service, total, current);
  }

  public static IndexRecreateObject end(String indexName, Service service) {
    return new IndexRecreateObject(indexName, service, Status.End);
  }

  public static IndexRecreateObject error(String indexName, Service service, int total, int current, String message) {
    return new IndexRecreateObject(indexName, service, total, current);
  }

  public String getMessage() {
    return message;
  }

  public String getIndexName() {
    return indexName;
  }

  public int getTotal() {
    return total;
  }

  public int getCurrent() {
    return current;
  }

  public Status getStatus() {
    return status;
  }

  public Service getService() {
    return service;
  }
}
