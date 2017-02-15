<%-- //[START all]--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>

<%-- //[START imports]--%>
<%@ page import="com.example.starter.Post" %>
<%@ page import="com.example.starter.Album" %>
<%@ page import="com.googlecode.objectify.Key" %>
<%@ page import="com.googlecode.objectify.ObjectifyService" %>
<%@ page import="com.example.starter.ImagesServlet" %>
<%@ page import="com.example.starter.MyUser" %>


<%-- //[END imports]--%>

<%@ page import="java.util.List" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
</head>

<body>

<%
    String albumName = request.getParameter("albumName");
    if (albumName == null) {
        albumName = "default";
    }
    pageContext.setAttribute("albumName", albumName);
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    if (user != null) {
        pageContext.setAttribute("user", user);
%>

<p>Hello, ${fn:escapeXml(user.nickname)}! (You can
    <a href="<%= userService.createLogoutURL("/albums.jsp") %>">sign out</a>.)</p>
<%
    } else {
%>
<p>Hello!
    <a href="<%= userService.createLoginURL(request.getRequestURI() + "?albumName=" + albumName ) %>">Sign in</a>
    to include your name with posts you post.</p>
<%
    }
     // Create the correct Ancestor key and fetch album metadata
      Key<Album> albumKey = Key.create(Album.class, albumName);
      Album album =  ObjectifyService.ofy()
          .load()
          .key(albumKey)
          .now();
     
      // Check album existensce and permission to view
      if (album == null) {
        throw new IllegalArgumentException("Album " + albumName + " does not exist.");
      } else if ((album.isRestricted() && (user == null || (!album.isViewer(new MyUser(user))) ))) {
         throw new IllegalArgumentException("You do not have permission to view this album. Restricted: " 
         + album.isRestricted() + " you: " + user);
      }

      final boolean userCanPost = (user != null) && (album.isRestricted() ? 
          (album.isEditor(new MyUser(user))) : true);
      final boolean userCanShare = (user != null) && (album.isEditor(new MyUser(user)));

    // Run an ancestor query to ensure we see the most up-to-date
    // view of the Greetings belonging to the selected Guestbook.
      List<Post> posts = ObjectifyService.ofy()
          .load()
          .type(Post.class) // We want only Greetings
          .ancestor(albumKey)    // Anyone in this book
          .order("date")       // Most recent first - date is indexed.
          .list();

%>

<p><h1> Album '${fn:escapeXml(albumName)}' </h1></p>

<table style="width:100%" > <tr>
  <td>
    <input type="button" onclick="location.href='/albums.jsp';" value="Back">
  </td>
  <td>
    <!-- Album Sharing form -->
    <p> <h3> Album Sharing   </h3>  </p>
    <ul>
      <li> <b> Owner: </b> <%= album.getOwner().getNickname() %> </li>
      <li> <b> Shared with: </b>   </li>
      <form   action="/share" method="post" name="removeCollab" id="removeCollab"> 
        <fieldset <% if(!userCanShare) {%> disabled <%} %> >
        <select name="collabName" size="3" form="removeCollab" required>
          <% for (MyUser currUser : album.getViewers()) { 
            final boolean currCanEdit = (album.isEditor(currUser)) ;

            final String permissionDescript = (currCanEdit ? "(can edit)" : "(can view)");
          %>
          <option value="<%= currUser.getEmail()  %>" name="collabName" id="collabName" 
              <% if(album.isOwner((currUser))) {%> disabled <%} %>    > 
                <%= currUser.getNickname() +" "+ permissionDescript %> </option>
          <%}%>
        </select>
        <input type="hidden" name="operation" id="operation" value="remove" >
        <input type="hidden" name="collabDomain" id="collabDomain" value="gmail.com" >
        <input type="hidden" name="albumName" id="albumName" value="<%= albumName %>" >

        <input type="submit" value="Revoke Access" >
        </fieldset>
      </form>
    </ul>
    <form  action="/share" method="post" name="addCollab" id="addCollab">
      <fieldset <% if(!userCanShare) {%> disabled <%} %> >
        <legend> Add Collaborator: </legend>
        Email Address: <input type="email" name="collabName" id="collabName"
                  placeholder="person1@gmail.com" required /> <br>
                 <input type="hidden" name="collabDomain" id="collabDomain"
                  placeholder="gmail.com" value="gmail.com" required /> <br>
                  <input type="hidden" name="albumName" id="albumName" value="<%= albumName %>" >
                  <input type="checkbox" name="grantEditAccess" id="grantEditAccess"> Can Edit <br>
                  <input type="hidden" name="operation" id="operation" value="add" >


        <input type="submit"  value="Share">
      </fieldset>
    </form>
  </td>
</tr> </table>




<%
       if (posts.isEmpty()) {
%>
<%
    } else {
%>

<%
      // Display all Image posts
        for (Post greeting : posts) {
            pageContext.setAttribute("greeting_content", greeting.content);
            String author;
            if (greeting.author_email == null) {
                author = "An anonymous person";
            } else {
                author = greeting.author_email;
                String author_id = greeting.author_id;
                if (user != null && user.getUserId().equals(author_id)) {
                    author += " (You)";
                }
            }
            pageContext.setAttribute("greeting_user", author);
            // Construct image URL
%>
<p><b>${fn:escapeXml(greeting_user)}</b> posted (on:<%= greeting.date.toString() %> ):</p>
<blockquote>${fn:escapeXml(greeting_content)}</blockquote>
<% final String imageSrc = greeting.imageFilename + (ImagesServlet.APPEND_UUID_TO_FILENAME ? greeting.getUuidString() : "");  %>
<a href="<%= imageSrc %>">
  
    <img src="<%= imageSrc %>">
  
</a>
<%
        }
    }
%>
<hr>
<br>

<%-- // Post Greeting / Upload file form --%>
<%  
    if (!userCanPost) {
%>
<p> <i> Sign in (and have edit rights) to make a post </i> </p>
<%
    } else {
%>

<h2> Upload an image </h2>
<form action="/sign" method="post" name="putFile" id="putFile">
    <div><textarea style="display:none" name="content" rows="3" cols="60"></textarea></div>
    <div><input type="hidden" value="Post Greeting"/></div>
    <input type="hidden" name="albumName" value="${fn:escapeXml(albumName)}" required/>
    <div>
            Bucket: <input type="text" name="bucket" value="runexamples.appspot.com" required />
            <input type="hidden" name="fileName" value="cat.gif" required />
            <br /> File Contents:
            
            <input type="file" name="pic" id="pic" accept="image/*" required>
            <br />
            <input type="submit" onclick='uploadFile(this)' value="Upload Content" />
    </div>

</form>
<%
    }
%>

<%-- //[END datastore]--%>
<hr>

<script>
  function myFunc() {
  
    alert("in myFunc()");
  
  }
  
  function uploadFile() {
        var bucket = document.forms["putFile"]["bucket"].value;
        var filename = document.forms["putFile"]["fileName"].value;
        var gBookName = document.forms["putFile"]["albumName"].value;
        var uploadButton = document.getElementById("pic");

        if (bucket == null || bucket == "" || filename == null || filename == "") {
          alert("Both Bucket and FileName are required griowjhgwg!!!!");
          return false;
        } else if (uploadButton.files.length != 1) {
          alert("Select exactly one file");
        } else {
          var theFile = uploadButton.files[0];
          var postData = theFile;

          var request = new XMLHttpRequest();
          request.open("POST", "/gcs/" + bucket + "/" + filename + "?albumName=" + gBookName, false);
          request.setRequestHeader("Content-Type", theFile.type);
          request.send(postData);

        }
      }
  

</script>


</body>
</html>
<%-- //[END all]--%>
