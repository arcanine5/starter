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
  private HashSet<MyUser> editors;
  private HashSet<MyUser> viewers;
  private boolean restricted; // Whether this album should limit access to the editors
  
  
  private Album() {
    this.date = new Date();
    System.out.println("Album created at time: " + this.date);
    this.editors = new HashSet<MyUser>();
    this.viewers = new HashSet<MyUser>();
  }
  
  public Album(String name, MyUser owner, boolean privacy) {
    this();
    this.name = name;
    this.owner = owner;
    this.editors.add(owner);
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
    buffer.append("\t Shared with: " + this.editors + "Restricted "+ restricted + "\n");
    
    return buffer.toString();
  }
  
  public MyUser getOwner() {
    return this.owner;
  }
  
  public boolean addEditor(MyUser user) {
    System.out.println("Collab count: " + this.editors.size());
    return this.editors.add(user);
  }
  
  public HashSet<MyUser> getEditors() {
    HashSet<MyUser> copy = new HashSet<>(this.editors);
    return copy;
  }
  
  public boolean isRestricted() {
    return this.restricted;
  }
  
  public boolean isEditor(MyUser user) {
    return this.editors.contains(user);
  }
  
  public boolean removeEditor(MyUser user) {
    System.out.println("Collab count: " + this.editors.size());
    return this.editors.remove(user);
  }
  
  // ------------------------------------------------------------------------------
  
  public boolean addViewer(MyUser user) {
    System.out.println("Collab count: " + this.viewers.size());
    return this.viewers.add(user);
  }
  
  public HashSet<MyUser> getViewers() {
    HashSet<MyUser> copy = new HashSet<>(this.viewers);
    copy.addAll(this.editors);
    return copy;
  }
  
  
  public boolean isViewer(MyUser user) {
    return this.editors.contains(user) || this.viewers.contains(user);
  }
  
  public boolean removeViewer(MyUser user) {
    System.out.println("Collab count: " + this.viewers.size());
    return this.viewers.remove(user);
  }
  
  
}
//[END all]
