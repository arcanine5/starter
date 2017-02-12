/**
 * Copyright 2014-2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//[START all]
package com.example.starter;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Index;

import com.google.appengine.api.users.User;

import java.util.Date;
import java.util.HashSet;


/**
 * The @Entity tells Objectify about our entity.  We also register it in
 * OfyHelper.java -- very important.
 *
 * Represents a photo album
 */
@Entity
public class Album {
  @Id private String name;
  @Index private Date date;
  private MyUser owner;
  private HashSet<MyUser> collaborators;
  private boolean restricted; // Whether this album should limit access to the collaborators
  
  
  private Album() {
    this.date = new Date();
    System.out.println("Album created at time: " + this.date);
    this.collaborators = new HashSet<MyUser>();
  }
  
  public Album(String name, MyUser owner, boolean privacy) {
    this();
    this.name = name;
    this.owner = owner;
    this.collaborators.add(owner);
    this.restricted = privacy;
  }
  
  public String getName() {
    return this.name;
  }
  
  @Override
  public String toString() {
    StringBuilder buffer = new  StringBuilder();
    buffer.append("Album: " + this.name + "\n");
    buffer.append("\t Created: " + this.date + "\n");
    buffer.append("\t Owner: " + this.owner + "\n");
    buffer.append("\t Shared with: " + this.collaborators + "Restricted "+ restricted + "\n");
    
    return buffer.toString();
  }
  
  public MyUser getOwner() {
    return this.owner;
  }
  
  public boolean addCollaborator(MyUser user) {
    System.out.println("Collab count: " + this.collaborators.size());
    return this.collaborators.add(user);
  }
  
  public HashSet<MyUser> getCollaborators() {
    HashSet<MyUser> copy = new HashSet<>(this.collaborators);
    return copy;
  }
  
  public boolean isRestricted() {
    return this.restricted;
  }
  
  public boolean isViewer(MyUser user) {
    return this.collaborators.contains(user);
  }
}
//[END all]
