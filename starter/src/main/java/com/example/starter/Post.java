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

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

import java.lang.String;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.example.starter.Album;
/**
 * An image posting.
 * 
 * The @Entity tells Objectify about our entity.  We also register it in {@link OfyHelper}
 * Our primary key @Id is set automatically by the Google Datastore for us.
 *
 * We add a @Parent to tell the object about its ancestor. We are doing this to support many
 * guestbooks.  Objectify, unlike the AppEngine library requires that you specify the fields you
 * want to index using @Index.  Only indexing the fields you need can lead to substantial gains in
 * performance -- though if not indexing your data from the start will require indexing it later.
 *
 * NOTE - all the properties are PUBLIC so that we can keep the code simple.
 **/
@Entity
public class Post {
  @Parent Key<Album> theBook;
  @Id public Long id;

  public String author_email;
  public String author_id;
  public String content;
  @Index public Date date;
  public String imageFilename;
  private String uuidString; // Disambiguates posts with same image filenames and content

  /**
   * Simple constructor just sets the date
   **/
  public Post() {
    date = new Date();
  }

  /**
   * A convenience constructor
   **/
  public Post(String book, String content) {
    this();
    if( book != null ) {
      theBook = Key.create(Album.class, book);  // Creating the Ancestor key
    } else {
      theBook = Key.create(Album.class, "default");
    }
    this.content = content;
  }

  /**
   * Takes all important fields
   **/
  public Post(String book, String content, String id, String email) {
    this(book, content);
    author_email = email;
    author_id = id;
  }
  
  /**
   * Constructor for image posts
   */
  public Post(String book, String content, String id, String email, String imageFilename, 
      String uuid) {
    this(book, content, id, email);
    this.imageFilename = imageFilename;
    this.uuidString =  uuid;
  }
  
  public String  getUuidString() {
    return this.uuidString;
  }

}
//[END all]
