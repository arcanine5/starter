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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.*;

import java.util.Collection;
import java.util.Enumeration;
import java.util.UUID;

// Borrowed Imports
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;


// End borrowed GCS imports

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;


/**
 * Form Handling Servlet
 * Most of the action for this sample is in webapp/album.jsp, which displays the
 * {@link Post}'s. This servlet has one method
 * {@link #doPost(<#HttpServletRequest req#>, <#HttpServletResponse resp#>)} which takes the form
 * data and saves it.
 */
public class SharingServlet extends HttpServlet {
  
  /**Used below to determine the size of chucks to read in. Should be > 1kb and < 10MB */
  private static final int BUFFER_SIZE = 2 * 1024 * 1024;

  // Process the http POST of the form
  @Override
  public void doPost(HttpServletRequest req, final HttpServletResponse resp) throws IOException {
    System.out.println("\n\n Sharing Servlet POST");
    
    // Enumerate the parameters of this request
    Enumeration<String> paramNames = req.getParameterNames();
    System.out.print("Parameter names in this req:  ");
    while (paramNames.hasMoreElements()) {
      String paramKey = paramNames.nextElement();
      System.out.println("\t " + paramKey);
      for (String value : req.getParameterValues(paramKey))
        System.out.println("\t \t value: " + value);
    }
    
    UserService userService = UserServiceFactory.getUserService();
    final User requestingUser = userService.getCurrentUser();  // Find out who the user is.

    final String collabEmailAddr = req.getParameter("collabName");
    final String authDomain = req.getParameter("collabDomain");
    final String requestedAlbumName = req.getParameter("albumName");
    final boolean grantEditAccess = req.getParameter("grantEditAccess") != null;
    final String operation = req.getParameter("operation");
    
    
    final User newCollab = new User(collabEmailAddr, authDomain);
    assert (newCollab != null);
    assert(false);
    
    // Check format of new Collaborator
    System.out.println("User creating album is: " + SharingServlet.userToString(newCollab));
    
    Object trash = ObjectifyService.ofy().transact( new Work() {
      public Object run() {
    
        // Create if name not taken by existing Album
        Album albumToShare = ObjectifyService.ofy()
            .load()
            .key(Key.create(Album.class, requestedAlbumName))
            .now();
        
        // Handle req format and login errors
        try {
          if ((requestedAlbumName == null) || (requestedAlbumName.equals(""))) {
            resp.sendError(resp.SC_BAD_REQUEST, "Specify the new album name.");
            return "";
          } else if (requestingUser == null) {
            resp.sendError(resp.SC_UNAUTHORIZED, "log in to share an album.");
            return "";
          } else if (!( albumToShare.isEditor(new MyUser(requestingUser))  )) {
            resp.sendError(resp.SC_UNAUTHORIZED, "You are not an editor. Ask: " + 
                albumToShare.getOwner().getEmail() + " for assistance");
            return "";
          }
        } catch (IOException e) {
          System.out.println("Io exception in Sharing servlet " + e);
          return "";
        }
        
        // wipe out existing permissions for this user
        albumToShare.removeEditor(new MyUser(newCollab));
        albumToShare.removeViewer(new MyUser(newCollab));
        
        if (operation.equals("add")) {
          System.out.println("Adding collaborator");
          // Add collaborator TODO: Avoid race conditions. maybe with transactions
          if (grantEditAccess) {
            albumToShare.addEditor(new MyUser(newCollab));
          } else {
            albumToShare.addViewer(new MyUser(newCollab));
          }
        } else {
          System.out.println("Removing collaborator");
        }
        
        
        ObjectifyService.ofy().save().entity(albumToShare).now();
        try {
          System.out.println("About to redirect...");
          resp.sendRedirect("/images.jsp?albumName=" + requestedAlbumName);
        } catch (IOException e) {
          System.out.println("Redirect failed.");
        }
        
        return "";
      }
    });
  }
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    System.out.println("\n\n Albums Servlet GET");
    if (true)
      throw new UnsupportedOperationException("Write sharing get handler.");
    
    String reqURL = req.getRequestURL().toString();
    String reqURI = req.getRequestURI();
    String reqQstring = req.getQueryString();
    
    System.out.println("\n\nRequest URL: " + reqURL + "\n Request URI:" + 
        reqURI + "\n Query String:" + reqQstring);
    
     
  }
  
  
  /**
   * Transfer the data from the inputStream to the outputStream. Then close both streams.
   */
  private void copy(InputStream input, OutputStream output) throws IOException {
    try {
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead = input.read(buffer);
      while (bytesRead != -1) {
        output.write(buffer, 0, bytesRead);
        bytesRead = input.read(buffer);
      }
    } finally {
      input.close();
      output.close();
    }
  }
  
  public static String userToString(User us) {
    StringBuilder buffer = new  StringBuilder();
    buffer.append("\t Email: " + us.getEmail() + "\n");
    buffer.append("\t ID: " + us.getUserId() + "\n");
    buffer.append("\t AuthDomain: " + us.getAuthDomain() + "\n");
    buffer.append("\t Fed ID: " + us.getFederatedIdentity() + "\n");
    
    return buffer.toString();
  }
  
}
//[END all]
