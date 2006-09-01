/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.bpel.o;

import org.apache.ode.utils.ObjectPrinter;

import java.util.HashSet;
import java.util.Set;


/**
 * Compiled represnetation of a BPEL activity.
 */
public abstract class OActivity extends OAgent {
  static final long serialVersionUID = -1L  ;
  
  public OExpression joinCondition;
  public final Set<OLink>sourceLinks = new HashSet<OLink>();
  public final Set<OLink>targetLinks = new HashSet<OLink>();
  public String name;
  public FailureHandling failureHandling;

  public String getType() {
    return ObjectPrinter.getShortClassName(getClass());
  }

  public OActivity(OProcess owner) {
    super(owner);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer(super.toString());
    if (name != null) {
      buf.append('-');
      buf.append(name);
    }

    return buf.toString();
  }

}
