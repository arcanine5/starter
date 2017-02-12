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

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import java.lang.String;


/**

 **/

public class MyUser implements java.lang.Comparable<MyUser>  {
  
  private User user;
  
  public MyUser() {
    
  }
  
  public MyUser(User us) {
    if (us == null) {
      throw new IllegalArgumentException("User instance may not be null");
    }
    this.user = us;
  }
  
  @Override
  public int compareTo(MyUser o) {
    return this.user.getEmail().compareTo(o.getEmail());
  }
  
  @Override
  public int hashCode() {
    return this.user.getEmail().hashCode();
  }
  
  @Override
  public boolean equals(java.lang.Object object) {
    MyUser other = (MyUser) object;
    return this.getEmail().equals(other.getEmail());
  }
  public java.lang.String getNickname() {
    return this.user.getNickname();
  }
  public java.lang.String getAuthDomain() {
    return this.user.getAuthDomain();
  }
  public java.lang.String getEmail() {
    return this.user.getEmail();
  }
  public java.lang.String getUserId() {
    return this.user.getUserId();
  }
  public java.lang.String getFederatedIdentity() {
    return this.user.getFederatedIdentity();
  }
  public java.lang.String toString() {
    return this.user.toString();
  }
  
  

}
//[END all]
