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
import com.google.appengine.api.datastore.Key;
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



/**
 * Form Handling Servlet
 * Most of the action for this sample is in webapp/album.jsp, which displays the
 * {@link Post}'s. This servlet has one method
 * {@link #doPost(<#HttpServletRequest req#>, <#HttpServletResponse resp#>)} which takes the form
 * data and saves it.
 */
public class ImagesServlet extends HttpServlet {
  
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

  // Process the http POST of the form
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Post newImagePost;

    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();  // Find out who the user is.

    // Fetch HTTP POST query string pairs for debug
    String albumName = req.getParameter("albumName");
    String content = req.getParameter("content");
    String bucketName = req.getParameter("bucket");
    String filename = req.getParameter("fileName");
    String MIMEtype = req.getHeader("Content-Type");
    
    String reqURL = req.getRequestURL().toString();
    String reqURI = req.getRequestURI();
    String reqQstring = req.getQueryString();
    
    System.out.println("\n\nRequest URL: " + reqURL + "\n Request URI:" + 
        reqURI + "\n Query String:" + reqQstring);
    
     
    System.out.println("Recieved POST with bucket: " + bucketName + 
        " filename: " + filename + " album: " + albumName + " content: "
        + content + " filetype: " + MIMEtype);
    
    // Enumerate the parameters of this request
    Enumeration<String> paramNames = req.getParameterNames();
    System.out.print("Parameter names in this req:  ");
    while (paramNames.hasMoreElements()) {
      String paramKey = paramNames.nextElement();
      System.out.println("\t " + paramKey);
      for (String value : req.getParameterValues(paramKey))
        System.out.println("\t \t value: " + value);
    }
    
    // Ignore POSTS from HTML forms TODO: fix this
    if (reqURI.equals("/sign")) {
      System.out.println("Skipping POST FROM FORM");
      System.out.println("About to redirect...");
      resp.sendRedirect("/images.jsp?albumName=" + albumName);
      return;
    }
        
    // Construct Post for Datastore
    if (user != null) {
      String newPostUuidString = UUID.randomUUID().toString();
      newImagePost = new Post(albumName, content, user.getUserId(), user.getEmail(),
          reqURI, newPostUuidString);
    } else {
      // TODO: Remove this. Do not allow post from users not logged in
      newImagePost = new Post(albumName, content);
    }

    // Use Objectify to save the newImagePost and now() is used to make the call synchronously as we
    // will immediately get a new page using redirect and we want the data to be present.
    ObjectifyService.ofy().save().entity(newImagePost).now();
    
    // Upload to Cloud Storage
    GcsFileOptions instance = (new GcsFileOptions.Builder()).mimeType(MIMEtype)
        .build();

    GcsFilename fileName = getFileName(req);
    if (APPEND_UUID_TO_FILENAME) {
      fileName = appendToObjectName(fileName, newImagePost.getUuidString());
    }
    GcsOutputChannel outputChannel;
    outputChannel = gcsService.createOrReplace(fileName, instance);
    copy(req.getInputStream(), Channels.newOutputStream(outputChannel));

    System.out.println("About to redirect...");
    resp.sendRedirect("/images.jsp?albumName=" + albumName);
  }
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    System.out.println("\n\n do get!!!!!");
    
    String reqURL = req.getRequestURL().toString();
    String reqURI = req.getRequestURI();
    String reqQstring = req.getQueryString();
    
    System.out.println("\n\nRequest URL: " + reqURL + "\n Request URI:" + 
        reqURI + "\n Query String:" + reqQstring);
    
    // Serve image files from GCS
    GcsFilename fileName = getFileName(req);
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    BlobKey blobKey = blobstoreService.createGsBlobKey(
        "/gs/" + fileName.getBucketName() + "/" + fileName.getObjectName());
    blobstoreService.serve(blobKey, resp);
     
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
  
  private void parseRequestAsMultiPart(HttpServletRequest req) 
  {
    // Try parsing as multipart
    /*
    Collection<javax.servlet.http.Part> formData = req.getParts();
    
    if (formData.isEmpty()) {
      System.out.println("No form parts.");
    } else {
      Iterator<Part> iter = formData.iterator();
      while (iter.hasNext()) {
        Part next = iter.next();
        System.out.println("Content Part named: " + next.getName() + " of type");
      }
    }
    */
  }
  
  public static GcsFilename appendToObjectName(GcsFilename oldFilename, String toAppend) {
    return new GcsFilename(oldFilename.getBucketName(), oldFilename.getObjectName() + toAppend);
  }
  
  
}
//[END all]
