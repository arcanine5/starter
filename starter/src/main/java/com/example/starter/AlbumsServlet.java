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
//[START gcs_imports]
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;
//[END gcs_imports]
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
public class AlbumsServlet extends HttpServlet {
  
  public static final boolean APPEND_UUID_TO_FILENAME = true;

  /**
   * This is where backoff parameters are configured. Here it is aggressively retrying with
   * backoff, up to 10 times but taking no more that 15 seconds total to do so.
   */
  private final GcsService gcsService = GcsServiceFactory.createGcsService(new RetryParams.Builder()
      .initialRetryDelayMillis(10)
      .retryMaxAttempts(10)
      .totalRetryPeriodMillis(15000)
      .build());

  /**Used below to determine the size of chucks to read in. Should be > 1kb and < 10MB */
  private static final int BUFFER_SIZE = 2 * 1024 * 1024;

  // Handle Album Creation and deletion
  @Override
  public void doPost(HttpServletRequest req, final HttpServletResponse resp) throws IOException {
    System.out.println("\n\n Albums Servlet POST");
    
    UserService userService = UserServiceFactory.getUserService();
    final User user = userService.getCurrentUser();  // Find out who the user is.

    final String requestedAlbumName = req.getParameter("newAlbumName");
    final boolean isPrivate = req.getParameter("privacy") != null;
    System.out.println("Privacy setting " + isPrivate);
    
    
    // Handle req format errors
    if ((requestedAlbumName == null) || (requestedAlbumName.equals(""))) {
      resp.sendError(resp.SC_BAD_REQUEST, "Specify the new album name.");
      return;
    } else if (user == null) {
      resp.sendError(resp.SC_UNAUTHORIZED, "log in to create an album.");
      return;
    }
    
    // Check format of requesting user
    System.out.println("User creating album is: " + SharingServlet.userToString(user));
    
    // Create if name not taken by existing Album
    Object trash = ObjectifyService.ofy().transact( new Work() {
      public Object run() {
        Album existing = ObjectifyService.ofy()
            .load()
            .key(Key.create(Album.class, requestedAlbumName))
            .now();
        
        if (existing == null) {
          System.out.println("Album with name " + requestedAlbumName + " is NEW. Creating...");
          Album newAlbum = new Album(requestedAlbumName, new MyUser(user), isPrivate);
          ObjectifyService.ofy().save().entity(newAlbum).now();
          
          resp.setStatus(resp.SC_CREATED);
          System.out.println("About to redirect...");
          try {
            resp.sendRedirect("/albums.jsp");
          } catch (IOException e) {
            System.out.println("Redirect failed.");
          }
        } else {
          System.out.println("Album with name " + requestedAlbumName + " already exists");
          try {
            resp.sendError(resp.SC_CONFLICT, "Album with name: " + requestedAlbumName + " already exists.");
          } catch (IOException e) {
            System.out.println("Redirect failed.");
          }
        }
        
        return "";
      }
    });
    
  }
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    System.out.println("\n\n Albums Servlet GET");
    
    String reqURL = req.getRequestURL().toString();
    String reqURI = req.getRequestURI();
    String reqQstring = req.getQueryString();
    
    System.out.println("\n\nRequest URL: " + reqURL + "\n Request URI:" + 
        reqURI + "\n Query String:" + reqQstring);
    
     
  }
  
  
  private GcsFilename getFileName(HttpServletRequest req) {
    String[] splits = req.getRequestURI().split("/", 4);
    if (!splits[0].equals("") || !splits[1].equals("gcs")) {
      throw new IllegalArgumentException("The URL is not formed as expected. " +
          "Expecting /gcs/<bucket>/<object>");
    }
    return new GcsFilename(splits[2], splits[3]);
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
  

  
  public static GcsFilename appendToObjectName(GcsFilename oldFilename, String toAppend) {
    return new GcsFilename(oldFilename.getBucketName(), oldFilename.getObjectName() + toAppend);
  }
  
  
}
//[END all]
